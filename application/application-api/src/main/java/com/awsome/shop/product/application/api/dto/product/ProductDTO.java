package com.awsome.shop.product.application.api.dto.product;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Product 数据传输对象
 */
@Data
public class ProductDTO {

    private Long id;

    private String name;

    private String sku;

    private String category;

    private String productType;

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

    /**
     * 派生字段：可用库存为 0 时为 true（FR-PR5 售罄判定）
     */
    private Boolean soldOut;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
