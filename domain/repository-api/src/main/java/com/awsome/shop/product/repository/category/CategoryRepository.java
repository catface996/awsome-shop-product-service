package com.awsome.shop.product.repository.category;

import com.awsome.shop.product.domain.model.category.CategoryEntity;

import java.util.List;

/**
 * Category 仓储接口
 */
public interface CategoryRepository {

    CategoryEntity getById(Long id);

    List<CategoryEntity> listAll(String name, Integer status);

    void save(CategoryEntity entity);

    void update(CategoryEntity entity);

    void deleteById(Long id);

    /**
     * 统计某分类的直接子分类数量（未删除）
     *
     * @param parentId 父分类 ID
     * @return 子分类数量
     */
    long countChildren(Long parentId);
}
