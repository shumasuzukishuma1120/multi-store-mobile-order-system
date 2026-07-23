package com.portfolio.mobileorder.customer.service;

import com.portfolio.mobileorder.customer.dto.CustomerCategoryResponse;

/**
 * 来店客向けカテゴリ取得サービス。
 */
public interface CustomerCategoryService {

    /**
     * 来店客向けカテゴリ一覧を取得する。
     *
     * @param qrToken QRコードトークン
     * @param visitToken 来店トークン
     * @return 来店客向けカテゴリ一覧
     */
    CustomerCategoryResponse getCategoriesForCustomer(String qrToken, String visitToken);
}
