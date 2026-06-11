package com.awsome.shop.product.application.api.dto.stock;

import lombok.Data;

/**
 * 库存预占响应
 */
@Data
public class ReserveStockResponse {

    /**
     * 预占 ID（reservationId）
     */
    private String reservationId;
}
