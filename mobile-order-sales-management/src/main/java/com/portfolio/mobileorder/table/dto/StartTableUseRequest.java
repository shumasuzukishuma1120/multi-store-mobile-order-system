package com.portfolio.mobileorder.table.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * テーブル使用開始リクエストDTO。
 */
@Getter
@Setter
@AllArgsConstructor
public class StartTableUseRequest {

    /**
     * 楽観ロック用バージョン。
     */
    @NotNull
    @PositiveOrZero
    private Integer version;
}
