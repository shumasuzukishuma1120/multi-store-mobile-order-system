package com.portfolio.mobileorder.menu.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Category {
    Long categoryId;
    String categoryName;
    String imageUrl;
}
