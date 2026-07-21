package com.portfolio.mobileorder.table.controller;

import com.portfolio.mobileorder.common.exception.GlobalExceptionHandler;
import com.portfolio.mobileorder.table.dto.StartTableUseRequest;
import com.portfolio.mobileorder.table.dto.StartTableUseResponse;
import com.portfolio.mobileorder.table.dto.StartTableUseResponse.CurrentVisitSession;
import com.portfolio.mobileorder.table.model.TableStatus;
import com.portfolio.mobileorder.table.service.TableUseService;
import com.portfolio.mobileorder.visit.model.VisitSessionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TableUseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TableUseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TableUseService tableUseService;


    @Nested
    @DisplayName("テーブル利用開始API")
    class StartTableUse {

        @Test
        @DisplayName("startTableUse_正常系_@PostMappingでテーブル利用開始を実行した場合、201とレスポンスJSONを返す")
        void startTableUse_returnsCreatedAndResponseJson() throws Exception {
            // given
            LocalDateTime startedAt = LocalDateTime.of(2026, 1, 1, 10, 0);
            LocalDateTime expiresAt = LocalDateTime.of(2026, 1, 1, 16, 0);

            CurrentVisitSession currentVisitSession =
                    new CurrentVisitSession(
                            100L,
                            "visit-token",
                            VisitSessionStatus.ACTIVE,
                            startedAt,
                            expiresAt
                    );

            StartTableUseResponse response = new StartTableUseResponse(
                    1L,
                    1L,
                    "A1",
                    TableStatus.OCCUPIED,
                    1,
                    currentVisitSession
            );

            when(tableUseService.startTableUse(
                    eq(1L),
                    eq(1L),
                    any(StartTableUseRequest.class)
            )).thenReturn(response);

            // when & then
            mockMvc.perform(post("/stores/{storeId}/tables/{tableId}/visit-sessions", 1L, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "version": 0
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tableId").value(1))
                    .andExpect(jsonPath("$.storeId").value(1))
                    .andExpect(jsonPath("$.tableNumber").value("A1"))
                    .andExpect(jsonPath("$.tableStatus").value("OCCUPIED"))
                    .andExpect(jsonPath("$.tableVersion").value(1))
                    .andExpect(jsonPath("$.currentVisitSession.visitSessionId").value(100))
                    .andExpect(jsonPath("$.currentVisitSession.visitToken").value("visit-token"))
                    .andExpect(jsonPath("$.currentVisitSession.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.currentVisitSession.startedAt").value("2026-01-01T10:00:00"))
                    .andExpect(jsonPath("$.currentVisitSession.expiresAt").value("2026-01-01T16:00:00"));

            ArgumentCaptor<StartTableUseRequest> requestCaptor =
                    ArgumentCaptor.forClass(StartTableUseRequest.class);

            verify(tableUseService).startTableUse(
                    eq(1L),
                    eq(1L),
                    requestCaptor.capture()
            );

            assertEquals(0, requestCaptor.getValue().getVersion());
        }

        @Test
        @DisplayName("startTableUse_異常系01_version未指定の場合、400とVALIDATION_ERRORを返す")
        void startTableUse_returnsBadRequest() throws Exception {
            mockMvc.perform(post("/stores/{storeId}/tables/{tableId}/visit-sessions", 1L, 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                    {
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").value("入力値が不正です。"));
        }

        @Test
        @DisplayName("startTableUse_異常系02_versionが負数の場合、400とVALIDATION_ERRORを返す")
        void startTableUse_returnsBadRequestForNegativeVersion() throws Exception {
            mockMvc.perform(post("/stores/{storeId}/tables/{tableId}/visit-sessions", 1L, 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                    {
                                      "version": -1
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").value("入力値が不正です。"));
        }
    }
}
