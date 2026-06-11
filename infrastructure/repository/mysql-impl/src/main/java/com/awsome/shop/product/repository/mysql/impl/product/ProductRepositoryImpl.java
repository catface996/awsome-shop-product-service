package com.awsome.shop.product.repository.mysql.impl.product;

import com.awsome.shop.product.common.dto.PageResult;
import com.awsome.shop.product.domain.model.product.ProductEntity;
import com.awsome.shop.product.domain.model.product.ProductType;
import com.awsome.shop.product.repository.mysql.mapper.product.ProductMapper;
import com.awsome.shop.product.repository.mysql.po.product.ProductPO;
import com.awsome.shop.product.repository.product.ProductRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Product 仓储实现
 */
@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductMapper productMapper;

    @Override
    public ProductEntity getById(Long id) {
        ProductPO po = productMapper.selectById(id);
        return po == null ? null : toEntity(po);
    }

    @Override
    public ProductEntity getBySku(String sku) {
        LambdaQueryWrapper<ProductPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductPO::getSku, sku);
        ProductPO po = productMapper.selectOne(wrapper);
        return po == null ? null : toEntity(po);
    }

    @Override
    public PageResult<ProductEntity> page(int page, int size, String name, String category) {
        IPage<ProductPO> result = productMapper.selectPage(new Page<>(page, size), name, category);

        PageResult<ProductEntity> pageResult = new PageResult<>();
        pageResult.setCurrent(result.getCurrent());
        pageResult.setSize(result.getSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setPages(result.getPages());
        pageResult.setRecords(result.getRecords().stream().map(this::toEntity).collect(Collectors.toList()));
        return pageResult;
    }

    @Override
    public void save(ProductEntity entity) {
        ProductPO po = toPO(entity);
        productMapper.insert(po);
        entity.setId(po.getId());
    }

    @Override
    public void update(ProductEntity entity) {
        ProductPO po = toPO(entity);
        productMapper.updateById(po);
    }

    @Override
    public void deleteById(Long id) {
        productMapper.deleteById(id);
    }

    @Override
    public ProductEntity lockById(Long id) {
        ProductPO po = productMapper.selectByIdForUpdate(id);
        return po == null ? null : toEntity(po);
    }

    @Override
    public void deductStockAndIncrSold(Long productId, int quantity) {
        productMapper.deductStockAndIncrSold(productId, quantity);
    }

    @Override
    public void adjustStock(Long productId, int newQty) {
        productMapper.adjustStock(productId, newQty);
    }

    @Override
    public Map<String, Long> countGroupByCategory() {
        Map<String, Map<String, Object>> raw = productMapper.countGroupByCategory();
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : raw.entrySet()) {
            Object cnt = entry.getValue().get("cnt");
            result.put(entry.getKey(), cnt instanceof Number ? ((Number) cnt).longValue() : 0L);
        }
        return result;
    }

    @Override
    public long countByCategory(String category) {
        return productMapper.countByCategory(category);
    }

    private ProductEntity toEntity(ProductPO po) {
        ProductEntity entity = new ProductEntity();
        entity.setId(po.getId());
        entity.setName(po.getName());
        entity.setSku(po.getSku());
        entity.setCategory(po.getCategory());
        entity.setProductType(po.getProductType() == null ? null : ProductType.valueOf(po.getProductType()));
        entity.setBrand(po.getBrand());
        entity.setPointsPrice(po.getPointsPrice());
        entity.setMarketPrice(po.getMarketPrice());
        entity.setStock(po.getStock());
        entity.setSoldCount(po.getSoldCount());
        entity.setStatus(po.getStatus());
        entity.setDescription(po.getDescription());
        entity.setImageUrl(po.getImageUrl());
        entity.setSubtitle(po.getSubtitle());
        entity.setDeliveryMethod(po.getDeliveryMethod());
        entity.setServiceGuarantee(po.getServiceGuarantee());
        entity.setPromotion(po.getPromotion());
        entity.setColors(po.getColors());
        entity.setSpecs(po.getSpecs());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        return entity;
    }

    private ProductPO toPO(ProductEntity entity) {
        ProductPO po = new ProductPO();
        po.setId(entity.getId());
        po.setName(entity.getName());
        po.setSku(entity.getSku());
        po.setCategory(entity.getCategory());
        po.setProductType(entity.getProductType() == null ? null : entity.getProductType().name());
        po.setBrand(entity.getBrand());
        po.setPointsPrice(entity.getPointsPrice());
        po.setMarketPrice(entity.getMarketPrice());
        po.setStock(entity.getStock());
        po.setSoldCount(entity.getSoldCount());
        po.setStatus(entity.getStatus());
        po.setDescription(entity.getDescription());
        po.setImageUrl(entity.getImageUrl());
        po.setSubtitle(entity.getSubtitle());
        po.setDeliveryMethod(entity.getDeliveryMethod());
        po.setServiceGuarantee(entity.getServiceGuarantee());
        po.setPromotion(entity.getPromotion());
        po.setColors(entity.getColors());
        po.setSpecs(entity.getSpecs());
        return po;
    }
}
