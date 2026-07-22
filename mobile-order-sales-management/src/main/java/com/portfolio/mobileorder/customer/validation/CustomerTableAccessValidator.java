package com.portfolio.mobileorder.customer.validation;

import com.portfolio.mobileorder.customer.model.CustomerTableAccess;

/**
 * 顧客がテーブルにアクセスするための情報を検証するためのインターフェース。
 */
public interface CustomerTableAccessValidator {

    /**
     * 顧客がテーブルにアクセスするための情報を検証するメソッド。
     * @param qrToken QRコードトークン
     * @param visitToken 訪問トークン
     * @return 顧客のテーブルアクセス情報
     */
    CustomerTableAccess validate(String qrToken, String visitToken);
}
