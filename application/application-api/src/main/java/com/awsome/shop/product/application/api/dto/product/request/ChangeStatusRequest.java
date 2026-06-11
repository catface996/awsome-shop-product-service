package com.awsome.shop.product.application.api.dto.product.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品上下架请求（status: 0-下架 / 1-上架）
 */
@Data
public class ChangeStatusRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为 0(下架) 或 1(上架)")
    @Max(value = 1, message = "状态只能为 0(下架) 或 1(上架)")
    private Integer status;
}
