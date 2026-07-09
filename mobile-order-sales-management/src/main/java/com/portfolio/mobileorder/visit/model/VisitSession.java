package com.portfolio.mobileorder.visit.model;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 来店セッションを表すモデル
 * visit_sessions に対応する。
 */
@Getter
@Setter
public class VisitSession {

    private Long id;
    private Long storeId;
    private Long tableId;
    private String visitToken;
    private VisitSessionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
