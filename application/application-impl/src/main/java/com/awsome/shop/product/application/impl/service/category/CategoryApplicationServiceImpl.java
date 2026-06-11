package com.awsome.shop.product.application.impl.service.category;

import com.awsome.shop.product.application.api.dto.category.CategoryDTO;
import com.awsome.shop.product.application.api.dto.category.request.CreateCategoryRequest;
import com.awsome.shop.product.application.api.dto.category.request.DeleteCategoryRequest;
import com.awsome.shop.product.application.api.dto.category.request.ListCategoryRequest;
import com.awsome.shop.product.application.api.dto.category.request.UpdateCategoryRequest;
import com.awsome.shop.product.application.api.service.category.CategoryApplicationService;
import com.awsome.shop.product.domain.model.category.CategoryEntity;
import com.awsome.shop.product.domain.service.category.CategoryDomainService;
import com.awsome.shop.product.domain.service.product.ProductDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Category 应用服务实现
 *
 * <p>依赖 CategoryDomainService 查询类目，依赖 ProductDomainService 统计商品数量</p>
 */
@Service
@RequiredArgsConstructor
public class CategoryApplicationServiceImpl implements CategoryApplicationService {

    private final CategoryDomainService categoryDomainService;
    private final ProductDomainService productDomainService;

    @Override
    public List<CategoryDTO> list(ListCategoryRequest request) {
        // 1. 查询全量类目
        List<CategoryEntity> allCategories = categoryDomainService.list(
                request.getName(), request.getStatus());

        // 2. 批量查询各分类名称对应的商品数量
        Map<String, Long> productCountMap = productDomainService.countGroupByCategory();

        // 3. 转换为 DTO
        List<CategoryDTO> allDTOs = allCategories.stream()
                .map(entity -> toDTO(entity, productCountMap))
                .collect(Collectors.toList());

        // 4. 组装树形结构
        return buildTree(allDTOs);
    }

    @Override
    public CategoryDTO create(CreateCategoryRequest request) {
        CategoryEntity entity = new CategoryEntity();
        entity.setName(request.getName());
        entity.setParentId(request.getParentId());
        entity.setIcon(request.getIcon());
        entity.setSortOrder(request.getSortOrder());
        entity.setStatus(request.getStatus());
        entity.setDescription(request.getDescription());
        return toSingleDTO(categoryDomainService.create(entity));
    }

    @Override
    public CategoryDTO update(UpdateCategoryRequest request) {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(request.getId());
        entity.setName(request.getName());
        entity.setIcon(request.getIcon());
        entity.setSortOrder(request.getSortOrder());
        entity.setStatus(request.getStatus());
        entity.setDescription(request.getDescription());
        return toSingleDTO(categoryDomainService.update(entity));
    }

    @Override
    public void delete(DeleteCategoryRequest request) {
        categoryDomainService.delete(request.getId());
    }

    private CategoryDTO toSingleDTO(CategoryEntity entity) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setParentId(entity.getParentId());
        dto.setIcon(entity.getIcon());
        dto.setSortOrder(entity.getSortOrder());
        dto.setStatus(entity.getStatus());
        dto.setDescription(entity.getDescription());
        dto.setChildren(new ArrayList<>());
        return dto;
    }

    private List<CategoryDTO> buildTree(List<CategoryDTO> allDTOs) {
        // 按 parentId 分组
        Map<Long, List<CategoryDTO>> childrenMap = allDTOs.stream()
                .filter(dto -> dto.getParentId() != null)
                .collect(Collectors.groupingBy(CategoryDTO::getParentId));

        // 找出一级类目（parentId 为 null）并挂载子类目
        List<CategoryDTO> tree = allDTOs.stream()
                .filter(dto -> dto.getParentId() == null)
                .peek(parent -> {
                    List<CategoryDTO> children = childrenMap.getOrDefault(parent.getId(), new ArrayList<>());
                    children.sort(Comparator.comparing(CategoryDTO::getSortOrder, Comparator.reverseOrder())
                            .thenComparing(CategoryDTO::getId));
                    parent.setChildren(children);
                })
                .sorted(Comparator.comparing(CategoryDTO::getSortOrder, Comparator.reverseOrder())
                        .thenComparing(CategoryDTO::getId))
                .collect(Collectors.toList());

        return tree;
    }

    private CategoryDTO toDTO(CategoryEntity entity, Map<String, Long> productCountMap) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setParentId(entity.getParentId());
        dto.setIcon(entity.getIcon());
        dto.setSortOrder(entity.getSortOrder());
        dto.setStatus(entity.getStatus());
        dto.setDescription(entity.getDescription());
        dto.setProductCount(productCountMap.getOrDefault(entity.getName(), 0L));
        dto.setChildren(new ArrayList<>());
        return dto;
    }
}
