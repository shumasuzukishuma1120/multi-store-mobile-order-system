package com.portfolio.mobileorder.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerCategoryResponse {
    List<Category> categories;

    /**
     * カテゴリ情報。
     */
    @Getter
    @AllArgsConstructor
    public static class Category {

        private Long categoryId;

        private String categoryName;

        private String imageUrl;
    }
}

