package com.awsome.shop.product.application.api.dto.product.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 更新 Product 请求（仅提供需修改的字段，未提供字段保持不变）
 */
@Data
public class UpdateProductRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Size(max = 200, message = "商品名称不能超过200个字符")
    private String name;

    @Size(max = 100, message = "商品分类不能超过100个字符")
    private String category;

    @Pattern(regexp = "PHYSICAL|VIRTUAL", message = "商品类型必须为 PHYSICAL 或 VIRTUAL")
    private String productType;

    @Size(max = 100, message = "品牌不能超过100个字符")
    private String brand;

    private Integer pointsPrice;

    private BigDecimal marketPrice;

    private Integer status;

    private String description;

    @Size(max = 500, message = "主图URL不能超过500个字符")
    private String imageUrl;

    @Size(max = 500, message = "副标题不能超过500个字符")
    private String subtitle;

    @Size(max = 200, message = "配送方式不能超过200个字符")
    private String deliveryMethod;

    @Size(max = 500, message = "服务保障不能超过500个字符")
    private String serviceGuarantee;

    @Size(max = 200, message = "促销活动不能超过200个字符")
    private String promotion;

    @Size(max = 500, message = "可选颜色不能超过500个字符")
    private String colors;

    private List<Map<String, String>> specs;
}
