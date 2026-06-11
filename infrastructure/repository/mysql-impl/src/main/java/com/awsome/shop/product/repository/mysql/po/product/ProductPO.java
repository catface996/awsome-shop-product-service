package com.awsome.shop.product.repository.mysql.po.product;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Product 持久化对象
 */
@Data
@TableName(value = "product", autoResultMap = true)
public class ProductPO {

    @TableId(type = IdType.AUTO)
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

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, String>> specs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;
}
