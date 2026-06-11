package com.awsome.shop.product.domain.service.category;

import com.awsome.shop.product.domain.model.category.CategoryEntity;

import java.util.List;

/**
 * Category 领域服务接口
 */
public interface CategoryDomainService {

    List<CategoryEntity> list(String name, Integer status);

    /**
     * 按 ID 查询分类（不存在抛 BusinessException）。
     *
     * @param id 分类 ID
     * @return 分类实体
     */
    CategoryEntity getById(Long id);

    /**
     * 创建分类。若 parentId 非空，校验其指向一个顶级分类（防止三级分类）。
     *
     * @param entity 待创建分类（不含 id）
     * @return 创建后的分类实体
     */
    CategoryEntity create(CategoryEntity entity);

    /**
     * 更新分类基本信息（名称等）。
     *
     * @param entity 含 id 的分类实体
     * @return 更新后的分类实体
     */
    CategoryEntity update(CategoryEntity entity);

    /**
     * 删除分类。存在子分类或关联商品时抛 BusinessException。
     *
     * @param id 分类 ID
     */
    void delete(Long id);
}
