package com.awsome.shop.product.domain.impl.service.category;

import com.awsome.shop.product.common.enums.CategoryErrorCode;
import com.awsome.shop.product.common.exception.BusinessException;
import com.awsome.shop.product.domain.model.category.CategoryEntity;
import com.awsome.shop.product.repository.category.CategoryRepository;
import com.awsome.shop.product.repository.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CategoryDomainServiceImpl 单元测试（Mockito）
 *
 * <p>覆盖：二级层级校验、删除占用校验。</p>
 */
@ExtendWith(MockitoExtension.class)
class CategoryDomainServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryDomainServiceImpl categoryDomainService;

    private CategoryEntity category(Long id, String name, Long parentId) {
        CategoryEntity c = new CategoryEntity();
        c.setId(id);
        c.setName(name);
        c.setParentId(parentId);
        return c;
    }

    @Test
    void getById_notFound_throws() {
        when(categoryRepository.getById(9L)).thenReturn(null);
        assertThatThrownBy(() -> categoryDomainService.getById(9L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND.getCode());
    }

    @Test
    void create_parentNotFound_throws() {
        CategoryEntity child = category(null, "子", 99L);
        when(categoryRepository.getById(99L)).thenReturn(null);
        assertThatThrownBy(() -> categoryDomainService.create(child))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND.getCode());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_parentIsAlreadyChild_throwsLevelExceeded() {
        CategoryEntity child = category(null, "三级", 2L);
        // parent(2) itself has a parent(1) -> would be a 3rd level
        when(categoryRepository.getById(2L)).thenReturn(category(2L, "二级", 1L));
        assertThatThrownBy(() -> categoryDomainService.create(child))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CategoryErrorCode.CATEGORY_LEVEL_EXCEEDED.getCode());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_topLevel_saves() {
        CategoryEntity top = category(null, "顶级", null);
        when(categoryRepository.getById(any())).thenReturn(category(10L, "顶级", null));
        categoryDomainService.create(top);
        verify(categoryRepository).save(top);
    }

    @Test
    void delete_hasChildren_throwsInUse() {
        when(categoryRepository.getById(1L)).thenReturn(category(1L, "顶级", null));
        when(categoryRepository.countChildren(1L)).thenReturn(2L);
        assertThatThrownBy(() -> categoryDomainService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CategoryErrorCode.CATEGORY_IN_USE.getCode());
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void delete_hasProducts_throwsInUse() {
        when(categoryRepository.getById(1L)).thenReturn(category(1L, "数码", null));
        when(categoryRepository.countChildren(1L)).thenReturn(0L);
        when(productRepository.countByCategory("数码")).thenReturn(3L);
        assertThatThrownBy(() -> categoryDomainService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(CategoryErrorCode.CATEGORY_IN_USE.getCode());
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void delete_noChildrenNoProducts_softDeletes() {
        when(categoryRepository.getById(1L)).thenReturn(category(1L, "空分类", null));
        when(categoryRepository.countChildren(1L)).thenReturn(0L);
        when(productRepository.countByCategory("空分类")).thenReturn(0L);
        categoryDomainService.delete(1L);
        verify(categoryRepository).deleteById(1L);
    }
}
