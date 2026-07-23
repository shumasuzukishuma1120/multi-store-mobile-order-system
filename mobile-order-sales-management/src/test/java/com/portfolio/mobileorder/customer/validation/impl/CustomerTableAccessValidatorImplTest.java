package com.portfolio.mobileorder.customer.validation.impl;

import com.portfolio.mobileorder.common.exception.ConflictException;
import com.portfolio.mobileorder.common.exception.ForbiddenException;
import com.portfolio.mobileorder.common.exception.NotFoundException;
import com.portfolio.mobileorder.customer.model.CustomerTableAccess;
import com.portfolio.mobileorder.table.mapper.RestaurantTableMapper;
import com.portfolio.mobileorder.table.model.RestaurantTable;
import com.portfolio.mobileorder.table.model.TableStatus;
import com.portfolio.mobileorder.visit.mapper.VisitSessionMapper;
import com.portfolio.mobileorder.visit.model.VisitSession;
import com.portfolio.mobileorder.visit.model.VisitSessionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * CustomerTableAccessValidatorImplの単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class CustomerTableAccessValidatorImplTest {

    @InjectMocks
    private CustomerTableAccessValidatorImpl customerTableAccessValidator;

    @Mock
    private RestaurantTableMapper restaurantTableMapper;

    @Mock
    private VisitSessionMapper visitSessionMapper;

    @Test
    @DisplayName("validate_正常系_qrTokenとvisitTokenが有効な場合、CustomerTableAccessを返す")
    void validate_validQrTokenAndVisitToken_returnsCustomerTableAccess() {
        // arrange
        RestaurantTable restaurantTable = this.restaurantTable(
                1L,
                10L,
                TableStatus.OCCUPIED
        );

        LocalDateTime now = LocalDateTime.now();

        VisitSession visitSession = this.visitSession(
                100L,
                10L,
                1L,
                "visit-token",
                VisitSessionStatus.ACTIVE,
                now.plusHours(6)
        );

        when(restaurantTableMapper.findByQrToken("qr-token"))
                .thenReturn(Optional.of(restaurantTable));

        when(visitSessionMapper.findByTableIdAndVisitToken(1L, "visit-token"))
                .thenReturn(Optional.of(visitSession));

        // act
        CustomerTableAccess result = customerTableAccessValidator.validate("qr-token", "visit-token");

        // assert
        assertEquals(10L, result.getStoreId());
        assertEquals(1L, result.getTableId());
        assertEquals(100L, result.getVisitSessionId());

        verify(restaurantTableMapper).findByQrToken("qr-token");
        verify(visitSessionMapper).findByTableIdAndVisitToken(1L, "visit-token");
    }

    @Test
    @DisplayName("validate_異常系01_qrTokenに紐づくテーブルが存在しない場合、NotFoundExceptionが発生する")
    void validate_tableNotFound_throwsNotFoundException() {

        // arrange
        when(restaurantTableMapper.findByQrToken("qr-token")).thenReturn(Optional.empty());

        // act & assert
        NotFoundException exception = assertThrows(
                NotFoundException.class, () -> customerTableAccessValidator.validate("qr-token", "visit-token"));

        assertEquals("TABLE_NOT_FOUND", exception.getErrorCode());

        verify(restaurantTableMapper).findByQrToken("qr-token");
        verify(visitSessionMapper, never())
                .findByTableIdAndVisitToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("validate_異常系02_テーブル状態がOCCUPIEDではない場合、409(TABLE_NOT_OCCUPIED)エラーを返す")
    void validate_tableNotOccupied_throwsConflict() {
        // arrange
        RestaurantTable restaurantTable = this.restaurantTable(
                1L,
                10L,
                TableStatus.AVAILABLE
        );

        when(restaurantTableMapper.findByQrToken("qr-token")).thenReturn(Optional.of(restaurantTable));

        // act & assert
        ConflictException exception = assertThrows(
                ConflictException.class, () -> customerTableAccessValidator.validate("qr-token", "visit-token"));

        assertEquals("TABLE_NOT_OCCUPIED", exception.getErrorCode());
        verify(restaurantTableMapper).findByQrToken("qr-token");
        verify(visitSessionMapper, never()).findByTableIdAndVisitToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("validate_異常系03_visitTokenが対象テーブルに紐づかない場合、403(VISIT_SESSION_ACCESS_DENIED)エラーを返す")
    void validate_visitTokenNotLinkedToTable_throwsForbidden() {
        // arrange
        RestaurantTable restaurantTable = this.restaurantTable(
                1L,
                10L,
                TableStatus.OCCUPIED
        );

        when(restaurantTableMapper.findByQrToken("qr-token")).thenReturn(Optional.of(restaurantTable));

        when(visitSessionMapper.findByTableIdAndVisitToken(1L, "visit-token")).thenReturn(Optional.empty());

        // act & assert
        ForbiddenException exception = assertThrows(
                ForbiddenException.class, () -> customerTableAccessValidator.validate("qr-token", "visit-token"));
        assertEquals("VISIT_SESSION_ACCESS_DENIED", exception.getErrorCode());

        verify(restaurantTableMapper).findByQrToken("qr-token");
        verify(visitSessionMapper).findByTableIdAndVisitToken(1L, "visit-token");
    }

    @Test
    @DisplayName("validate_異常系04_来店セッションがACTIVEではない場合、409(VISIT_SESSION_NOT_ACTIVE)エラーを返す")
    void validate_visitSessionNotActive_throwsConflict() {
        // arrange
        RestaurantTable restaurantTable = this.restaurantTable(
                1L,
                10L,
                TableStatus.OCCUPIED
        );

        LocalDateTime now = LocalDateTime.now();

        VisitSession visitSession = this.visitSession(
                100L,
                10L,
                1L,
                "visit-token",
                VisitSessionStatus.CLOSED,
                now.plusHours(6)
        );

        when(restaurantTableMapper.findByQrToken("qr-token")).thenReturn(Optional.of(restaurantTable));

        when(visitSessionMapper.findByTableIdAndVisitToken(1L, "visit-token")).thenReturn(Optional.of(visitSession));

        // act & assert
        ConflictException exception = assertThrows(
                ConflictException.class, () -> customerTableAccessValidator.validate("qr-token", "visit-token"));

        assertEquals("VISIT_SESSION_NOT_ACTIVE", exception.getErrorCode());

        verify(restaurantTableMapper).findByQrToken("qr-token");
        verify(visitSessionMapper).findByTableIdAndVisitToken(1L, "visit-token");
    }

    @Test
    @DisplayName("validate_異常系05_来店セッションが期限切れの場合、409(VISIT_SESSION_EXPIRED)エラーを返す")
    void validate_visitSessionExpired_throwsConflict() {
        // arrange
        RestaurantTable restaurantTable = this.restaurantTable(
                1L,
                10L,
                TableStatus.OCCUPIED
        );

        LocalDateTime now = LocalDateTime.now();

        VisitSession visitSession = this.visitSession(
                100L,
                10L,
                1L,
                "visit-token",
                VisitSessionStatus.ACTIVE,
                now.minusMinutes(1)
        );

        when(restaurantTableMapper.findByQrToken("qr-token")).thenReturn(Optional.of(restaurantTable));

        when(visitSessionMapper.findByTableIdAndVisitToken(1L, "visit-token")).thenReturn(Optional.of(visitSession));

        // act & assert
        ConflictException exception = assertThrows(
                ConflictException.class, () -> customerTableAccessValidator.validate("qr-token", "visit-token"));
        assertEquals("VISIT_SESSION_EXPIRED", exception.getErrorCode());
        verify(restaurantTableMapper).findByQrToken("qr-token");
        verify(visitSessionMapper).findByTableIdAndVisitToken(1L, "visit-token");
    }

    // テストケース作成用
    private RestaurantTable restaurantTable(
            Long id,
            Long storeId,
            TableStatus status
    ) {
        RestaurantTable restaurantTable = new RestaurantTable();
        restaurantTable.setId(id);
        restaurantTable.setStoreId(storeId);
        restaurantTable.setTableNumber("A1");
        restaurantTable.setQrToken("qr-token");
        restaurantTable.setStatus(status);
        restaurantTable.setVersion(0);
        return restaurantTable;
    }

    private VisitSession visitSession(
            Long id,
            Long storeId,
            Long tableId,
            String visitToken,
            VisitSessionStatus status,
            LocalDateTime expiresAt
    ) {
        VisitSession visitSession = new VisitSession();
        visitSession.setId(id);
        visitSession.setStoreId(storeId);
        visitSession.setTableId(tableId);
        visitSession.setVisitToken(visitToken);
        visitSession.setStatus(status);
        visitSession.setStartedAt(LocalDateTime.now().minusHours(1));
        visitSession.setExpiresAt(expiresAt);
        visitSession.setVersion(0);
        return visitSession;
    }
}
