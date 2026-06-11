package com.awsome.shop.product.application.api.service.category;

import com.awsome.shop.product.application.api.dto.category.CategoryDTO;
import com.awsome.shop.product.application.api.dto.category.request.CreateCategoryRequest;
import com.awsome.shop.product.application.api.dto.category.request.DeleteCategoryRequest;
import com.awsome.shop.product.application.api.dto.category.request.ListCategoryRequest;
import com.awsome.shop.product.application.api.dto.category.request.UpdateCategoryRequest;

import java.util.List;

/**
 * Category 应用服务接口
 */
public interface CategoryApplicationService {

    List<CategoryDTO> list(ListCategoryRequest request);

    CategoryDTO create(CreateCategoryRequest request);

    CategoryDTO update(UpdateCategoryRequest request);

    void delete(DeleteCategoryRequest request);
}
