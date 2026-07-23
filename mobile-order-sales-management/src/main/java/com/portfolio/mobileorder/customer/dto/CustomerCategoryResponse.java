package com.portfolio.mobileorder.customer.dto;

import com.portfolio.mobileorder.menu.dto.Category;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CustomerCategoryResponse {
    List<Category> categories;

    public CustomerCategoryResponse(ArrayList<Category> categories) {
        this.categories = categories;
    }
}
