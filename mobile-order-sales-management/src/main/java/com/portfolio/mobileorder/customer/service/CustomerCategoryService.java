package com.portfolio.mobileorder.customer.service;

import com.portfolio.mobileorder.customer.dto.CustomerCategoryResponse;

public interface CustomerCategoryService {
    CustomerCategoryResponse getCategoriesForCustomer(String qrToken, String visitToken);
}
