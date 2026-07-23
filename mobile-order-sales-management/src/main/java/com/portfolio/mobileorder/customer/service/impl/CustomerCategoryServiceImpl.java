package com.portfolio.mobileorder.customer.service.impl;

import com.portfolio.mobileorder.customer.dto.CustomerCategoryResponse;
import com.portfolio.mobileorder.customer.model.CustomerTableAccess;
import com.portfolio.mobileorder.customer.service.CustomerCategoryService;
import com.portfolio.mobileorder.customer.validation.CustomerTableAccessValidator;
import com.portfolio.mobileorder.menu.mapper.MenuCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 来店客向けカテゴリ取得サービスの実装クラス。
 */
@Service
@RequiredArgsConstructor
public class CustomerCategoryServiceImpl implements CustomerCategoryService {

    private final MenuCategoryMapper menuCategoryMapper;

    private final CustomerTableAccessValidator customerTableAccessValidator;

    /**
     * 来店客向けカテゴリ一覧を取得する。
     *
     * @param qrToken QRコードトークン
     * @param visitToken 来店トークン
     * @return 来店客向けカテゴリ一覧のレスポンス
     */
    @Override
    public CustomerCategoryResponse getCategoriesForCustomer(String qrToken, String visitToken) {

        CustomerTableAccess customerTableAccess = customerTableAccessValidator.validate(qrToken, visitToken);

        List<CustomerCategoryResponse.Category> categories  =
                menuCategoryMapper.findAvailableCategoriesByStoreId(customerTableAccess.getStoreId())
                        .stream()
                        .map(category -> new CustomerCategoryResponse.Category(
                                category.getId(),
                                category.getName(),
                                category.getImageUrl()
                        ))
                        .toList();

        return new CustomerCategoryResponse(categories);
    }
}
