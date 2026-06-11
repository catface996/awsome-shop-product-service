package com.awsome.shop.product.application.api.dto.stock.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 库存预占请求
 */
@Data
public class ReserveStockRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "预占数量不能为空")
    @Min(value = 1, message = "预占数量最小为 1")
    private Integer quantity;

    @NotBlank(message = "orderRef 不能为空")
    @Size(max = 64, message = "orderRef 不能超过64个字符")
    private String orderRef;
}
