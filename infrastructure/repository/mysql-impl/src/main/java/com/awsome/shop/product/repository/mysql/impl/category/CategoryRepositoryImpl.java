package com.awsome.shop.product.repository.mysql.impl.category;

import com.awsome.shop.product.domain.model.category.CategoryEntity;
import com.awsome.shop.product.repository.category.CategoryRepository;
import com.awsome.shop.product.repository.mysql.mapper.category.CategoryMapper;
import com.awsome.shop.product.repository.mysql.po.category.CategoryPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Category 仓储实现
 */
@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryMapper categoryMapper;

    @Override
    public CategoryEntity getById(Long id) {
        CategoryPO po = categoryMapper.selectById(id);
        return po == null ? null : toEntity(po);
    }

    @Override
    public List<CategoryEntity> listAll(String name, Integer status) {
        List<CategoryPO> poList = categoryMapper.selectList(name, status);
        return poList.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public void save(CategoryEntity entity) {
        CategoryPO po = toPO(entity);
        categoryMapper.insert(po);
        entity.setId(po.getId());
    }

    @Override
    public void update(CategoryEntity entity) {
        CategoryPO po = toPO(entity);
        categoryMapper.updateById(po);
    }

    @Override
    public void deleteById(Long id) {
        categoryMapper.deleteById(id);
    }

    @Override
    public long countChildren(Long parentId) {
        return categoryMapper.countChildren(parentId);
    }

    private CategoryEntity toEntity(CategoryPO po) {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(po.getId());
        entity.setName(po.getName());
        entity.setParentId(po.getParentId());
        entity.setIcon(po.getIcon());
        entity.setSortOrder(po.getSortOrder());
        entity.setStatus(po.getStatus());
        entity.setDescription(po.getDescription());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        return entity;
    }

    private CategoryPO toPO(CategoryEntity entity) {
        CategoryPO po = new CategoryPO();
        po.setId(entity.getId());
        po.setName(entity.getName());
        po.setParentId(entity.getParentId());
        po.setIcon(entity.getIcon());
        po.setSortOrder(entity.getSortOrder());
        po.setStatus(entity.getStatus());
        po.setDescription(entity.getDescription());
        return po;
    }
}
