package com.awsome.shop.product.domain.impl.service.product;

import com.awsome.shop.product.common.dto.PageResult;
import com.awsome.shop.product.common.enums.SampleErrorCode;
import com.awsome.shop.product.common.enums.StockErrorCode;
import com.awsome.shop.product.common.exception.BusinessException;
import com.awsome.shop.product.domain.model.product.ProductEntity;
import com.awsome.shop.product.domain.model.product.ProductType;
import com.awsome.shop.product.domain.service.product.ProductDomainService;
import com.awsome.shop.product.repository.product.ProductRepository;
import com.awsome.shop.product.storage.ImageStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Product 领域服务实现
 */
@Service
@RequiredArgsConstructor
public class ProductDomainServiceImpl implements ProductDomainService {

    private final ProductRepository productRepository;
    private final ImageStoragePort imageStoragePort;

    @Override
    public ProductEntity getById(Long id) {
        ProductEntity entity = productRepository.getById(id);
        if (entity == null) {
            throw new BusinessException(SampleErrorCode.RESOURCE_NOT_FOUND);
        }
        return entity;
    }

    @Override
    public PageResult<ProductEntity> page(int page, int size, String name, String category) {
        return productRepository.page(page, size, name, category);
    }

    @Override
    public ProductEntity create(String name, String sku, String category, ProductType productType, String brand,
                                Integer pointsPrice, BigDecimal marketPrice, Integer stock,
                                Integer status, String description, String imageUrl,
                                String subtitle, String deliveryMethod, String serviceGuarantee,
                                String promotion, String colors, List<Map<String, String>> specs) {
        // SKU 唯一性校验
        ProductEntity existing = productRepository.getBySku(sku);
        if (existing != null) {
            throw new BusinessException(SampleErrorCode.RESOURCE_ALREADY_EXISTS, sku);
        }

        ProductEntity entity = new ProductEntity();
        entity.setName(name);
        entity.setSku(sku);
        entity.setCategory(category);
        entity.setProductType(productType);
        entity.setBrand(brand);
        entity.setPointsPrice(pointsPrice);
        entity.setMarketPrice(marketPrice);
        entity.setStock(stock != null ? stock : 0);
        entity.setStatus(status != null ? status : 0);
        entity.setDescription(description);
        entity.setImageUrl(imageUrl);
        entity.setSubtitle(subtitle);
        entity.setDeliveryMethod(deliveryMethod);
        entity.setServiceGuarantee(serviceGuarantee);
        entity.setPromotion(promotion);
        entity.setColors(colors);
        entity.setSpecs(specs);

        productRepository.save(entity);
        return productRepository.getById(entity.getId());
    }

    @Override
    public Map<String, Long> countGroupByCategory() {
        return productRepository.countGroupByCategory();
    }

    @Override
    @Transactional
    public void adjustStock(Long productId, int newQty) {
        ProductEntity entity = productRepository.getById(productId);
        if (entity == null) {
            throw new BusinessException(StockErrorCode.PRODUCT_NOT_FOUND);
        }
        productRepository.adjustStock(productId, newQty);
    }

    @Override
    public ProductEntity update(ProductEntity entity) {
        // 校验存在性
        getById(entity.getId());
        productRepository.update(entity);
        return productRepository.getById(entity.getId());
    }

    @Override
    public void changeStatus(Long productId, Integer status) {
        getById(productId);
        ProductEntity patch = new ProductEntity();
        patch.setId(productId);
        patch.setStatus(status);
        productRepository.update(patch);
    }

    @Override
    public void delete(Long productId) {
        getById(productId);
        productRepository.deleteById(productId);
    }

    @Override
    public String uploadImage(Long productId, byte[] content, String originalFilename) {
        getById(productId);
        String url = imageStoragePort.store(content, originalFilename);
        ProductEntity patch = new ProductEntity();
        patch.setId(productId);
        patch.setImageUrl(url);
        productRepository.update(patch);
        return url;
    }
}
