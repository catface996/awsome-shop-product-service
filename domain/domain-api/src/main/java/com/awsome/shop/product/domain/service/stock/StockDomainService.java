package com.awsome.shop.product.domain.service.stock;

/**
 * 库存领域服务接口
 *
 * <p>对齐 awsome-shop-plan component-methods.md §2 StockService，支撑兑换 Saga 的
 * 库存查询、悲观锁预占、释放、正式扣减。</p>
 */
public interface StockDomainService {

    /**
     * 查询可用库存 = product.stock - SUM(reservations[RESERVED].quantity)。
     *
     * @param productId 商品 ID
     * @return 可用库存
     */
    int getAvailableStock(Long productId);

    /**
     * 悲观锁预占，以 (orderRef, productId) 幂等。
     *
     * <p>流程：在事务内 SELECT product FOR UPDATE，命中已有预占则幂等返回其 ID；
     * 否则校验可用库存并写入 RESERVED 行。</p>
     *
     * @param productId 商品 ID
     * @param quantity  预占数量（&gt; 0）
     * @param orderRef  业务幂等键
     * @return 预占 ID（reservationId）
     */
    String reserveStock(Long productId, int quantity, String orderRef);

    /**
     * 释放预占：RESERVED → RELEASED；RELEASED 幂等 no-op；CONFIRMED 抛业务异常。
     *
     * @param reservationId 预占 ID
     */
    void releaseStock(String reservationId);

    /**
     * 正式扣减：RESERVED → CONFIRMED 并扣减 product.stock、累加 sold_count；
     * CONFIRMED 幂等 no-op；RELEASED 抛业务异常。
     *
     * @param reservationId 预占 ID
     */
    void confirmDeduct(String reservationId);

    /**
     * 批量查询多个商品的已预占数量（用于售罄判定，避免 N+1）。
     *
     * @param productIds 商品 ID 列表
     * @return Map，key 为 productId，value 为 RESERVED 预占数量（无预占的不出现）
     */
    java.util.Map<Long, Integer> getReservedQuantities(java.util.List<Long> productIds);
}
