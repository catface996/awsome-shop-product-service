package com.awsome.shop.product.domain.model.product;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Product 领域实体
 */
@Data
public class ProductEntity {

    private Long id;

    private String name;

    private String sku;

    private String category;

    private ProductType productType;

    private String brand;

    private Integer pointsPrice;

    private BigDecimal marketPrice;

    private Integer stock;

    private Integer soldCount;

    private Integer status;

    private String description;

    private String imageUrl;

    private String subtitle;

    private String deliveryMethod;

    private String serviceGuarantee;

    private String promotion;

    private String colors;

    private List<Map<String, String>> specs;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void updateInfo(String name, String sku, String category, String brand,
                           Integer pointsPrice, BigDecimal marketPrice, Integer stock,
                           Integer status, String description, String imageUrl,
                           String subtitle, String deliveryMethod, String serviceGuarantee,
                           String promotion, String colors, List<Map<String, String>> specs) {
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.brand = brand;
        this.pointsPrice = pointsPrice;
        this.marketPrice = marketPrice;
        this.stock = stock;
        this.status = status;
        this.description = description;
        this.imageUrl = imageUrl;
        this.subtitle = subtitle;
        this.deliveryMethod = deliveryMethod;
        this.serviceGuarantee = serviceGuarantee;
        this.promotion = promotion;
        this.colors = colors;
        this.specs = specs;
    }
}
