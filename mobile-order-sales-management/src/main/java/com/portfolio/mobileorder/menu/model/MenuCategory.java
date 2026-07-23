package com.portfolio.mobileorder.menu.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class MenuCategory {
    Long id;
    Long storeId;
    String name;
    String imageUrl;
    LocalDateTime createdAt;
    Long createdBy;
    LocalDateTime updatedAt;
    Long updatedBy;
    LocalDateTime deletedAt;
    Integer version;
}
