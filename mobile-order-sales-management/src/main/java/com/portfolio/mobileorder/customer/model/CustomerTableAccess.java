package com.portfolio.mobileorder.customer.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 顧客がテーブルにアクセスするための情報を保持するModel。
 */
@Getter
@AllArgsConstructor
public class CustomerTableAccess {
    private final Long storeId;
    private final Long tableId;
    private final Long visitSessionId;
}
