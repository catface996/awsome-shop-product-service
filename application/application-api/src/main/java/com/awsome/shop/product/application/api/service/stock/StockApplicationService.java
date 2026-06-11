package com.awsome.shop.product.application.api.service.stock;

import com.awsome.shop.product.application.api.dto.stock.ReserveStockResponse;
import com.awsome.shop.product.application.api.dto.stock.StockQueryResponse;
import com.awsome.shop.product.application.api.dto.stock.request.ReserveStockRequest;

/**
 * Stock 应用服务接口
 */
public interface StockApplicationService {

    /**
     * 查询可用库存与商品类型
     *
     * @param productId 商品 ID
     * @return 库存查询响应
     */
    StockQueryResponse getAvailableStock(Long productId);

    /**
     * 预占库存
     *
     * @param request 预占请求
     * @return 预占响应（含 reservationId）
     */
    ReserveStockResponse reserve(ReserveStockRequest request);

    /**
     * 释放预占
     *
     * @param reservationId 预占 ID
     */
    void release(String reservationId);

    /**
     * 正式扣减（确认）预占
     *
     * @param reservationId 预占 ID
     */
    void confirm(String reservationId);
}
