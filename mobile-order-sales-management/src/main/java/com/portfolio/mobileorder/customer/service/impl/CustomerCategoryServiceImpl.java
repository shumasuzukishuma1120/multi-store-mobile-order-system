package com.portfolio.mobileorder.customer.service.impl;

import com.portfolio.mobileorder.customer.dto.CustomerCategoryResponse;
import com.portfolio.mobileorder.customer.model.CustomerTableAccess;
import com.portfolio.mobileorder.customer.service.CustomerCategoryService;
import com.portfolio.mobileorder.customer.validation.CustomerTableAccessValidator;
import com.portfolio.mobileorder.menu.dto.Category;
import com.portfolio.mobileorder.menu.mapper.MenuCategoryMapper;
import com.portfolio.mobileorder.menu.model.MenuCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerCategoryServiceImpl implements CustomerCategoryService {

    private final MenuCategoryMapper menuCategoryMapper;

    private final CustomerTableAccessValidator customerTableAccessValidator;

    @Override
    public CustomerCategoryResponse getCategoriesForCustomer(String qrToken, String visitToken) {

        CustomerTableAccess customerTableAccess = customerTableAccessValidator.validate(qrToken, visitToken);

        List<MenuCategory> categories = new ArrayList<>();

        categories = menuCategoryMapper.findAvailableCategoriesByStoreId(customerTableAccess.getStoreId());

        CustomerCategoryResponse response = new CustomerCategoryResponse(new ArrayList<Category>());

        for (MenuCategory category : categories){
            Category tmp = new Category(
                    category.getId(),
                    category.getName(),
                    category.getImageUrl()
            );
            response.getCategories().add(tmp);
        }

        return response;
    }
}
