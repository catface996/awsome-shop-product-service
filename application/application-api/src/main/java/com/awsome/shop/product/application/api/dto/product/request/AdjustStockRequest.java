package com.awsome.shop.product.application.api.dto.product.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员调整库存请求
 */
@Data
public class AdjustStockRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能为负")
    private Integer newQty;
}
