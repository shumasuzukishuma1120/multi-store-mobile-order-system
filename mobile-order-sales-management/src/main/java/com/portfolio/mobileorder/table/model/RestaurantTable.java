package com.portfolio.mobileorder.table.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 飲食店のテーブルを表すモデル
 * restaurant_tables に対応する
 */
@Getter
@Setter
@AllArgsConstructor
public class RestaurantTable {

    private Long id;
    private Long storeId;
    private String tableNumber;
    private String qrToken;
    private TableStatus status;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private LocalDateTime deletedAt;
    private Integer version;
}
