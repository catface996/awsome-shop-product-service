package com.awsome.shop.product.application.api.dto.stock.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 库存查询请求
 */
@Data
public class StockQueryRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;
}
