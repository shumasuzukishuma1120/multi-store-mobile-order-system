package com.portfolio.mobileorder.customer.controller;

import com.portfolio.mobileorder.customer.dto.CustomerCategoryResponse;
import com.portfolio.mobileorder.customer.service.CustomerCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerCategoryController {

    private final CustomerCategoryService customerCategoryService;

    @GetMapping("/tables/{qrToken}/categories")
    public ResponseEntity<CustomerCategoryResponse> getCategories(
            @PathVariable String qrToken,
            @RequestHeader("X-Visit-Token") String visitToken
    ) {
        CustomerCategoryResponse response = customerCategoryService.getCategoriesForCustomer(qrToken, visitToken);

        return ResponseEntity.ok(response);
    }
 }
