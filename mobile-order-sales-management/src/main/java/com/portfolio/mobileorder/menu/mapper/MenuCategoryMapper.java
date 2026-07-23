package com.portfolio.mobileorder.menu.mapper;

import com.portfolio.mobileorder.menu.model.MenuCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MenuCategoryMapper {

    List<MenuCategory> findAvailableCategoriesByStoreId(@Param("storeId") Long storeId);
}
