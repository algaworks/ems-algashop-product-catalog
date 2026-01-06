package com.algaworks.algashop.product.catalog.application.category.query;

import com.algaworks.algashop.product.catalog.application.PageModel;
import org.springframework.cache.annotation.Cacheable;

import java.util.UUID;

public interface CategoryQueryService {
    @Cacheable(cacheNames = "algashop:categories-filter:v1",
            key = "#filter.hashCode()", 
            condition = "#filter.isDefault()")
    PageModel<CategoryDetailOutput> filter(CategoryFilter filter);
    CategoryDetailOutput findById(UUID categoryId);
}
