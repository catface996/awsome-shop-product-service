package com.awsome.shop.product.application.api.dto.stock;

import lombok.Data;

/**
 * 库存查询响应
 */
@Data
public class StockQueryResponse {

    private Long productId;

    private Integer availableStock;

    /**
     * 商品类型：PHYSICAL / VIRTUAL
     */
    private String productType;
}
