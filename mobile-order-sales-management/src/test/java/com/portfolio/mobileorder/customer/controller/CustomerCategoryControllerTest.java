package com.portfolio.mobileorder.customer.controller;

import com.portfolio.mobileorder.common.exception.ErrorCodeConst;
import com.portfolio.mobileorder.common.exception.ForbiddenException;
import com.portfolio.mobileorder.common.exception.GlobalExceptionHandler;
import com.portfolio.mobileorder.customer.dto.CustomerCategoryResponse;
import com.portfolio.mobileorder.customer.service.CustomerCategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 来店客向けカテゴリ取得Controllerの単体テスト。
 */
@WebMvcTest(CustomerCategoryController.class)
@AutoConfigureMockMvc(addFilters = false) //Security追加するまで無効化するアノテーション
@Import(GlobalExceptionHandler.class)
class CustomerCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerCategoryService customerCategoryService;

    @Test
    @DisplayName("getCategoriesForCustomer_正常系01_有効なqrTokenとvisitTokenの場合、カテゴリ一覧を返す")
    void getCategoriesForCustomer_validTokens_returnsCategoryList() throws Exception {
        // arrange
        CustomerCategoryResponse expectedResponse = new CustomerCategoryResponse(
                List.of(new CustomerCategoryResponse.Category(
                        1L,
                        "おすすめ",
                        "https://example.com/categories/recommend.jpg"
                ), new CustomerCategoryResponse.Category(
                        2L,
                        "フード",
                        "https://example.com/categories/food.jpg"
                ))
        );

        when(customerCategoryService.getCategoriesForCustomer("qr-token", "visit-token"))
                .thenReturn(expectedResponse);


        mockMvc.perform(get("/customer/tables/{qrToken}/categories", "qr-token")
                        .header("X-Visit-Token", "visit-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(2))
                .andExpect(jsonPath("$.categories[0].categoryId").value(1))
                .andExpect(jsonPath("$.categories[0].categoryName").value("おすすめ"))
                .andExpect(jsonPath("$.categories[0].imageUrl").value("https://example.com/categories/recommend.jpg"))
                .andExpect(jsonPath("$.categories[1].categoryId").value(2))
                .andExpect(jsonPath("$.categories[1].categoryName").value("フード"))
                .andExpect(jsonPath("$.categories[1].imageUrl").value("https://example.com/categories/food.jpg"));

        verify(customerCategoryService)
                .getCategoriesForCustomer("qr-token", "visit-token");
    }

    @Test
    @DisplayName("getCategoriesForCustomer_正常系02_該当カテゴリがない場合、空配列レスポンスを返す")
    void getCategoriesForCustomer_noCategories_returnsEmptyArray() throws Exception {
        // arrange
        CustomerCategoryResponse expectedResponse = new CustomerCategoryResponse(List.of());

        when(customerCategoryService.getCategoriesForCustomer("qr-token", "visit-token"))
                .thenReturn(expectedResponse);

        // act & assert
        mockMvc.perform(get("/customer/tables/{qrToken}/categories", "qr-token")
                        .header("X-Visit-Token", "visit-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(0));

        verify(customerCategoryService)
                .getCategoriesForCustomer("qr-token", "visit-token");
    }

    @Test
    @DisplayName("getCategoriesForCustomer_異常系01_ヘッダー未指定のとき400を返す")
    void getCategoriesForCustomer_missingVisitTokenHeader_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/customer/tables/{qrToken}/categories", "qr-token")
                        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(ErrorCodeConst.VALIDATION_ERROR))
                .andExpect(jsonPath("$.message").value("必須ヘッダーが指定されていません。"));

        verify(customerCategoryService, never()).getCategoriesForCustomer(anyString(), anyString());
    }

    @Test
    @DisplayName("getCategoriesForCustomer_異常系02_サービスでForbiddenExceptionが発生した場合、403を返す")
    void getCategoriesForCustomer_serviceThrowsForbiddenException_returnsForbidden() throws Exception {
        // arrange
        when(customerCategoryService.getCategoriesForCustomer("qr-token", "visit-token"))
                .thenThrow(new ForbiddenException(ErrorCodeConst.VISIT_SESSION_ACCESS_DENIED, "注文権限がありません。"));

        mockMvc.perform(get("/customer/tables/{qrToken}/categories", "qr-token")
                        .header("X-Visit-Token", "visit-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(ErrorCodeConst.VISIT_SESSION_ACCESS_DENIED))
                .andExpect(jsonPath("$.message").value("注文権限がありません。"));

        verify(customerCategoryService)
                .getCategoriesForCustomer("qr-token", "visit-token");
    }
}
