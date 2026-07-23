package com.portfolio.mobileorder.customer.service.impl;

import com.portfolio.mobileorder.common.exception.ConflictException;
import com.portfolio.mobileorder.common.exception.ErrorCodeConst;
import com.portfolio.mobileorder.common.exception.ForbiddenException;
import com.portfolio.mobileorder.customer.dto.CustomerCategoryResponse;
import com.portfolio.mobileorder.customer.model.CustomerTableAccess;
import com.portfolio.mobileorder.customer.validation.CustomerTableAccessValidator;
import com.portfolio.mobileorder.menu.mapper.MenuCategoryMapper;
import com.portfolio.mobileorder.menu.model.MenuCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * 顧客向けカテゴリ取得サービスの単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class CustomerCategoryServiceImplTest {

    @InjectMocks
    private CustomerCategoryServiceImpl customerCategoryService;

    @Mock
    private CustomerTableAccessValidator customerTableAccessValidator;

    @Mock
    private MenuCategoryMapper menuCategoryMapper;

    @Test
    @DisplayName("getCategoriesForCustomer_正常系01_有効なqrTokenとvisitTokenの場合、カテゴリ一覧を返す")
    void getCategoriesForCustomer_validTokens_returnsCategoryList() {
        // arrange
        CustomerTableAccess customerTableAccess = new CustomerTableAccess(
                10L,
                1L,
                100L
        );

        MenuCategory menuCategory1 = menuCategory(
                1L,
                10L,
                "おすすめ",
                "https://example.com/categories/recommend.jpg"
        );

        MenuCategory menuCategory2 = menuCategory(
                2L,
                10L,
                "フード",
                "https://example.com/categories/food.jpg"
        );

        when(customerTableAccessValidator.validate("qr-token", "visit-token")).thenReturn(customerTableAccess);

        when(menuCategoryMapper.findAvailableCategoriesByStoreId(10L)).thenReturn(List.of(menuCategory1, menuCategory2));

        // act
        CustomerCategoryResponse response = customerCategoryService.getCategoriesForCustomer("qr-token", "visit-token");

        // assert
        assertEquals(2, response.getCategories().size());
        assertEquals(1L, response.getCategories().get(0).getCategoryId());
        assertEquals("おすすめ", response.getCategories().get(0).getCategoryName());
        assertEquals("https://example.com/categories/recommend.jpg",
                response.getCategories().get(0).getImageUrl());

        assertEquals(2L, response.getCategories().get(1).getCategoryId());
        assertEquals("フード", response.getCategories().get(1).getCategoryName());
        assertEquals("https://example.com/categories/food.jpg",
                response.getCategories().get(1).getImageUrl());

        verify(customerTableAccessValidator).validate("qr-token", "visit-token");
        verify(menuCategoryMapper).findAvailableCategoriesByStoreId(10L);
    }

    @Test
    @DisplayName("getCategoriesForCustomer_正常系02_有効なqrTokenとvisitTokenでカテゴリが存在しない場合、空のカテゴリ一覧を返す")
    void getCategoriesForCustomer_validTokensNoCategories_returnsEmptyCategoryList() {
        // arrange
        CustomerTableAccess customerTableAccess = new CustomerTableAccess(
                10L,
                1L,
                100L
        );

        when(customerTableAccessValidator.validate("qr-token", "visit-token")).thenReturn(customerTableAccess);

        when(menuCategoryMapper.findAvailableCategoriesByStoreId(10L)).thenReturn(List.of());

        // act
        CustomerCategoryResponse response = customerCategoryService.getCategoriesForCustomer("qr-token", "visit-token");

        // assert
        assertEquals(0, response.getCategories().size());

        verify(customerTableAccessValidator).validate("qr-token", "visit-token");
        verify(menuCategoryMapper).findAvailableCategoriesByStoreId(10L);
    }

    @Test
    @DisplayName("getCategoriesForCustomer_異常系01_visitSessionが取得できない場合、ForbiddenExceptionが発生する")
    void getCategoriesForCustomer_visitSessionNotFound_throwsForbiddenException() {
        // arrange
        when(customerTableAccessValidator.validate("qr-token", "visit-token"))
                .thenThrow(new ForbiddenException(ErrorCodeConst.VISIT_SESSION_ACCESS_DENIED, "注文権限がありません。"));

        // act & assert
        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> customerCategoryService.getCategoriesForCustomer("qr-token", "visit-token")
        );

        assertEquals(ErrorCodeConst.VISIT_SESSION_ACCESS_DENIED, exception.getErrorCode());

        verify(customerTableAccessValidator).validate("qr-token", "visit-token");
        verify(menuCategoryMapper, never()).findAvailableCategoriesByStoreId(anyLong());
    }

    @Test
    @DisplayName("getCategoriesForCustomer_異常系02_visitSessionが期限切れの場合、ConflictExceptionが発生する")
    void getCategoriesForCustomer_visitSessionExpired_throwsConflictException() {
        // arrange
        when(customerTableAccessValidator.validate("qr-token", "visit-token"))
                .thenThrow(new ConflictException(ErrorCodeConst.VISIT_SESSION_EXPIRED, "指定された来店セッションは期限切れです。"));

        // act & assert
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> customerCategoryService.getCategoriesForCustomer("qr-token", "visit-token")
        );

        assertEquals(ErrorCodeConst.VISIT_SESSION_EXPIRED, exception.getErrorCode());

        verify(customerTableAccessValidator).validate("qr-token", "visit-token");
        verify(menuCategoryMapper, never()).findAvailableCategoriesByStoreId(anyLong());
    }

    private MenuCategory menuCategory(
            Long id,
            Long storeId,
            String name,
            String imageUrl
    ) {
        return new MenuCategory(
                id,
                storeId,
                name,
                imageUrl,
                null,
                null,
                null,
                null,
                null,
                0
        );
    }
}
