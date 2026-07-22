package com.portfolio.mobileorder.table.service.impl;

import com.portfolio.mobileorder.common.exception.ConflictException;
import com.portfolio.mobileorder.common.exception.ForbiddenException;
import com.portfolio.mobileorder.common.exception.NotFoundException;
import com.portfolio.mobileorder.table.dto.StartTableUseRequest;
import com.portfolio.mobileorder.table.dto.StartTableUseResponse;
import com.portfolio.mobileorder.table.mapper.RestaurantTableMapper;
import com.portfolio.mobileorder.table.model.RestaurantTable;
import com.portfolio.mobileorder.table.model.TableStatus;
import com.portfolio.mobileorder.visit.mapper.VisitSessionMapper;
import com.portfolio.mobileorder.visit.model.VisitSession;
import com.portfolio.mobileorder.visit.model.VisitSessionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TableUseServiceImpl の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class TableUseServiceImplTest {

    @Mock
    private RestaurantTableMapper restaurantTableMapper;

    @Mock
    private VisitSessionMapper visitSessionMapper;

    @InjectMocks
    private TableUseServiceImpl tableUseService;

    @Nested
    @DisplayName("テーブル利用開始")
    class StartTableUse {
        @Test
        @DisplayName("startTableUse_正常系_AVAILABLEテーブルなら来店セッションを作成してレスポンスを返す")
        void startTableUse_availableTable_createsVisitSessionAndReturnsResponse() {
            //arrange
            when(restaurantTableMapper.findById(1L)).thenReturn(
                    Optional.of(restaurantTable(1L, 1L, TableStatus.AVAILABLE, 0)
                    ), Optional.of(restaurantTable(1L, 1L, TableStatus.OCCUPIED, 1)));
            when(visitSessionMapper.existsActiveByTableId(1L)).thenReturn(false);
            when(restaurantTableMapper.updateStatusByIdAndVersion(1L, TableStatus.OCCUPIED, 0)).thenReturn(1);

            //ServiceのvisitSessionMapper.insertの挙動と自動採番のID設定
            doAnswer(invocation -> {
                VisitSession visitSession = invocation.getArgument(0);
                visitSession.setId(100L);
                return 1;
            }).when(visitSessionMapper).insert(any(VisitSession.class));

            //act
            StartTableUseResponse response = tableUseService.startTableUse(1L, 1L, new StartTableUseRequest(0));

            //then
            StartTableUseResponse.CurrentVisitSession currentVisitSession =
                    response.getCurrentVisitSession();

            assertEquals(1L, response.getTableId());
            assertEquals(1L, response.getStoreId());
            assertEquals("A1", response.getTableNumber());
            assertEquals(TableStatus.OCCUPIED, response.getTableStatus());
            assertEquals(1, response.getTableVersion());
            assertEquals(100L, currentVisitSession.getVisitSessionId());
            assertEquals(VisitSessionStatus.ACTIVE, currentVisitSession.getStatus());
            assertNotNull(currentVisitSession.getVisitToken());


            //insertの中身確認
            ArgumentCaptor<VisitSession> visitSessionCaptor =
                    ArgumentCaptor.forClass(VisitSession.class);

            verify(visitSessionMapper).insert(visitSessionCaptor.capture());

            VisitSession capturedVisitSession = visitSessionCaptor.getValue();

            assertEquals(1L, capturedVisitSession.getStoreId());
            assertEquals(1L, capturedVisitSession.getTableId());
            assertEquals(VisitSessionStatus.ACTIVE, capturedVisitSession.getStatus());
            assertNotNull(capturedVisitSession.getVisitToken());
            assertNotNull(capturedVisitSession.getStartedAt());
            assertNotNull(capturedVisitSession.getExpiresAt());
            assertEquals(
                    capturedVisitSession.getStartedAt().plusHours(6),
                    capturedVisitSession.getExpiresAt()
            );

            assertEquals(
                    capturedVisitSession.getVisitToken(),
                    currentVisitSession.getVisitToken()
            );

            verify(restaurantTableMapper, times(2)).findById(1L);
            verify(visitSessionMapper).existsActiveByTableId(1L);
            verify(restaurantTableMapper).updateStatusByIdAndVersion(1L, TableStatus.OCCUPIED, 0);
        }

        @Test
        @DisplayName("startTableUse_異常系01_存在しないtableIdを指定した場合、NotFoundExceptionが発生することを確認する")
        void startTableUse_nonExistentTableId_throwsNotFoundException() {
            //arrange
            when(restaurantTableMapper.findById(1L)).thenReturn(Optional.empty());

            //act&then
            NotFoundException exception = assertThrows(
                    NotFoundException.class,
                    () -> tableUseService.startTableUse(1L, 1L, new StartTableUseRequest(0))
            );

            assertEquals("TABLE_NOT_FOUND", exception.getErrorCode());

            verify(restaurantTableMapper).findById(1L);
            verify(visitSessionMapper, never()).existsActiveByTableId(1L);
            verify(restaurantTableMapper, never()).updateStatusByIdAndVersion(anyLong(), any(), anyInt());
            verify(visitSessionMapper, never()).insert(any(VisitSession.class));
        }

        @Test
        @DisplayName("startTableUse_異常系02_storeIdが一致しないテーブルを指定した場合、ForbiddenExceptionが発生することを確認する")
        void startTableUse_forbiddenException_throwsForbiddenException() {
            //arrange
            when(restaurantTableMapper.findById(1L)).thenReturn(
                    Optional.of(restaurantTable(1L, 1L, TableStatus.OCCUPIED, 0)));

            //act & assert
            ForbiddenException exception = assertThrows(
                    ForbiddenException.class,
                    () -> tableUseService.startTableUse(999L, 1L, new StartTableUseRequest(0))
            );

            assertEquals("STORE_ACCESS_DENIED", exception.getErrorCode());

            verify(restaurantTableMapper).findById(1L);
            verify(visitSessionMapper, never()).existsActiveByTableId(1L);
            verify(restaurantTableMapper, never()).updateStatusByIdAndVersion(anyLong(), any(), anyInt());
            verify(visitSessionMapper, never()).insert(any(VisitSession.class));
        }

        @Test
        @DisplayName("startTableUse_異常系03_すでにOCCUPIEDのテーブルを指定した場合、ConflictExceptionが発生することを確認する")
        void startTableUse_occupiedTable_throwsConflictException() {
            //arrange
            when(restaurantTableMapper.findById(1L)).thenReturn(
                    Optional.of(restaurantTable(1L, 1L, TableStatus.OCCUPIED, 0)));

            //act & assert
            ConflictException exception = assertThrows(
                    ConflictException.class,
                    () -> tableUseService.startTableUse(1L, 1L, new StartTableUseRequest(0)));
            assertEquals("TABLE_NOT_AVAILABLE", exception.getErrorCode());

            verify(restaurantTableMapper).findById(1L);
            verify(visitSessionMapper, never()).existsActiveByTableId(1L);
            verify(restaurantTableMapper, never()).updateStatusByIdAndVersion(anyLong(), any(), anyInt());
            verify(visitSessionMapper, never()).insert(any(VisitSession.class));
        }

        @Test
        @DisplayName("startTableUse_異常系04_ACTIVEな来店セッションが存在するテーブルを指定した場合、ConflictExceptionが発生することを確認する")
        void startTableUse_activeVisitSessionExists_throwsConflictException() {
            //arrange
            when(restaurantTableMapper.findById(1L)).thenReturn(
                    Optional.of(restaurantTable(1L, 1L, TableStatus.AVAILABLE, 0)));
            when(visitSessionMapper.existsActiveByTableId(1L)).thenReturn(true);

            //act & assert
            ConflictException exception = assertThrows(
                    ConflictException.class,
                    () -> tableUseService.startTableUse(1L, 1L, new StartTableUseRequest(0)));
            assertEquals("ACTIVE_VISIT_SESSION_EXISTS", exception.getErrorCode());

            verify(restaurantTableMapper).findById(1L);
            verify(visitSessionMapper).existsActiveByTableId(1L);
            verify(restaurantTableMapper, never()).updateStatusByIdAndVersion(anyLong(), any(), anyInt());
            verify(visitSessionMapper, never()).insert(any(VisitSession.class));
        }

        @Test
        @DisplayName("startTableUse_異常系05_versionが一致しないテーブルを指定した場合、ConflictExceptionが発生することを確認する")
        void startTableUse_versionConflict_throwsConflictException() {
            //arrange
            when(restaurantTableMapper.findById(1L)).thenReturn(
                    Optional.of(restaurantTable(1L, 1L, TableStatus.AVAILABLE, 0)));
            when(visitSessionMapper.existsActiveByTableId(1L)).thenReturn(false);
            when(restaurantTableMapper.updateStatusByIdAndVersion(1L, TableStatus.OCCUPIED, 0)).thenReturn(0);

            //act & assert
            ConflictException exception = assertThrows(
                    ConflictException.class,
                    () -> tableUseService.startTableUse(1L, 1L, new StartTableUseRequest(0))
            );
            assertEquals("OPTIMISTIC_LOCK_CONFLICT", exception.getErrorCode());

            verify(restaurantTableMapper).findById(1L);
            verify(visitSessionMapper).existsActiveByTableId(1L);
            verify(restaurantTableMapper).updateStatusByIdAndVersion(1L, TableStatus.OCCUPIED, 0);
            verify(visitSessionMapper, never()).insert(any(VisitSession.class));
        }

        private RestaurantTable restaurantTable(
                Long id,
                Long storeId,
                TableStatus status,
                Integer version
        ) {
            return new RestaurantTable(
                    id,
                    storeId,
                    "A1",
                    "qrToken",
                    status,
                    null,
                    null,
                    null,
                    null,
                    null,
                    version
            );
        }

        @Test
        @DisplayName("startTableUse_異常系06_更新後テーブルを再取得できない場合、IllegalStateExceptionが発生する")
        void startTableUse_afterUpdateTableNotFound_throwsIllegalStateException() {
            //arrange
            when(restaurantTableMapper.findById(1L)).thenReturn(
                    Optional.of(restaurantTable(1L, 1L, TableStatus.AVAILABLE, 0)),
                    Optional.empty());
            when(visitSessionMapper.existsActiveByTableId(1L)).thenReturn(false);
            when(restaurantTableMapper.updateStatusByIdAndVersion(1L, TableStatus.OCCUPIED, 0)).thenReturn(1);

            //act & assert
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> tableUseService.startTableUse(1L, 1L, new StartTableUseRequest(0)));

            assertThat(exception.getMessage()).contains("tableId=1");
            verify(restaurantTableMapper, times(2)).findById(1L);
            verify(visitSessionMapper).existsActiveByTableId(1L);
            verify(restaurantTableMapper).updateStatusByIdAndVersion(1L, TableStatus.OCCUPIED, 0);
            verify(visitSessionMapper, never()).insert(any(VisitSession.class));
        }
    }
}
