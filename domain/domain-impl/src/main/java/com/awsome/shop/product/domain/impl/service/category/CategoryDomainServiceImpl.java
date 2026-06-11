package com.awsome.shop.product.domain.impl.service.category;

import com.awsome.shop.product.common.enums.CategoryErrorCode;
import com.awsome.shop.product.common.exception.BusinessException;
import com.awsome.shop.product.domain.model.category.CategoryEntity;
import com.awsome.shop.product.domain.service.category.CategoryDomainService;
import com.awsome.shop.product.repository.category.CategoryRepository;
import com.awsome.shop.product.repository.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Category 领域服务实现
 */
@Service
@RequiredArgsConstructor
public class CategoryDomainServiceImpl implements CategoryDomainService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    public List<CategoryEntity> list(String name, Integer status) {
        return categoryRepository.listAll(name, status);
    }

    @Override
    public CategoryEntity getById(Long id) {
        CategoryEntity entity = categoryRepository.getById(id);
        if (entity == null) {
            throw new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND);
        }
        return entity;
    }

    @Override
    public CategoryEntity create(CategoryEntity entity) {
        // 二级约束：若指定父分类，父分类必须存在且本身为顶级（parentId 为空）
        if (entity.getParentId() != null) {
            CategoryEntity parent = categoryRepository.getById(entity.getParentId());
            if (parent == null) {
                throw new BusinessException(CategoryErrorCode.CATEGORY_NOT_FOUND);
            }
            if (parent.getParentId() != null) {
                throw new BusinessException(CategoryErrorCode.CATEGORY_LEVEL_EXCEEDED);
            }
        }
        categoryRepository.save(entity);
        return categoryRepository.getById(entity.getId());
    }

    @Override
    public CategoryEntity update(CategoryEntity entity) {
        CategoryEntity existing = getById(entity.getId());
        existing.updateInfo(entity.getName(), existing.getParentId(), entity.getIcon(),
                entity.getSortOrder(), entity.getStatus(), entity.getDescription());
        categoryRepository.update(existing);
        return categoryRepository.getById(existing.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CategoryEntity entity = getById(id);
        // 占用校验：存在子分类或关联商品则不可删除
        if (categoryRepository.countChildren(id) > 0) {
            throw new BusinessException(CategoryErrorCode.CATEGORY_IN_USE);
        }
        if (productRepository.countByCategory(entity.getName()) > 0) {
            throw new BusinessException(CategoryErrorCode.CATEGORY_IN_USE);
        }
        categoryRepository.deleteById(id);
    }
}
