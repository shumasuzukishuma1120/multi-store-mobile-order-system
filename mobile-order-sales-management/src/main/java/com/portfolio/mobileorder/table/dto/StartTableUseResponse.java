package com.portfolio.mobileorder.table.dto;

import com.portfolio.mobileorder.table.model.TableStatus;
import com.portfolio.mobileorder.visit.model.VisitSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * テーブル使用開始レスポンスDTO。
 */
@Getter
@AllArgsConstructor
public class StartTableUseResponse {

    private Long tableId;
    private Long storeId;
    private String tableNumber;
    private TableStatus tableStatus;
    private Integer tableVersion;
    private CurrentVisitSession currentVisitSession;

    @Getter
    @AllArgsConstructor
    public static class CurrentVisitSession {
        private Long visitSessionId;
        private String visitToken;
        private VisitSessionStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime expiresAt;
    }
}
