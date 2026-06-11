package com.awsome.shop.product.domain.impl.service.product;

import com.awsome.shop.product.common.enums.SampleErrorCode;
import com.awsome.shop.product.common.enums.StockErrorCode;
import com.awsome.shop.product.common.exception.BusinessException;
import com.awsome.shop.product.domain.model.product.ProductEntity;
import com.awsome.shop.product.repository.product.ProductRepository;
import com.awsome.shop.product.storage.ImageStoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ProductDomainServiceImpl 单元测试（Mockito）
 *
 * <p>覆盖：SKU 唯一性、库存调整/上下架/删除/编辑的不存在分支、图片上传回写。</p>
 */
@ExtendWith(MockitoExtension.class)
class ProductDomainServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ImageStoragePort imageStoragePort;

    @InjectMocks
    private ProductDomainServiceImpl productDomainService;

    private ProductEntity product(long id) {
        ProductEntity p = new ProductEntity();
        p.setId(id);
        p.setStock(10);
        return p;
    }

    @Test
    void create_skuAlreadyExists_throwsConflict() {
        when(productRepository.getBySku("SKU1")).thenReturn(product(1L));
        assertThatThrownBy(() -> productDomainService.create(
                "n", "SKU1", "cat", null, null, null, null, null, null, null, null, null, null, null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SampleErrorCode.RESOURCE_ALREADY_EXISTS.getCode());
    }

    @Test
    void adjustStock_productNotFound_throws() {
        when(productRepository.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> productDomainService.adjustStock(1L, 5))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(StockErrorCode.PRODUCT_NOT_FOUND.getCode());
    }

    @Test
    void adjustStock_success_updatesStock() {
        when(productRepository.getById(1L)).thenReturn(product(1L));
        productDomainService.adjustStock(1L, 42);
        verify(productRepository).adjustStock(1L, 42);
    }

    @Test
    void changeStatus_productNotFound_throws() {
        when(productRepository.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> productDomainService.changeStatus(1L, 1))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SampleErrorCode.RESOURCE_NOT_FOUND.getCode());
        verify(productRepository, never()).update(any());
    }

    @Test
    void delete_productNotFound_throws() {
        when(productRepository.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> productDomainService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SampleErrorCode.RESOURCE_NOT_FOUND.getCode());
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void uploadImage_storesAndWritesBackImageUrl() {
        when(productRepository.getById(1L)).thenReturn(product(1L));
        byte[] content = {1, 2, 3};
        when(imageStoragePort.store(content, "a.png")).thenReturn("/images/abc.png");

        String url = productDomainService.uploadImage(1L, content, "a.png");

        assertThat(url).isEqualTo("/images/abc.png");
        ArgumentCaptor<ProductEntity> captor = ArgumentCaptor.forClass(ProductEntity.class);
        verify(productRepository).update(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getImageUrl()).isEqualTo("/images/abc.png");
    }
}
