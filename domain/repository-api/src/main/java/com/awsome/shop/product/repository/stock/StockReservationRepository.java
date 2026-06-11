package com.awsome.shop.product.repository.stock;

import com.awsome.shop.product.domain.model.stock.ReservationStatus;
import com.awsome.shop.product.domain.model.stock.StockReservationEntity;

/**
 * 库存预占仓储接口
 */
public interface StockReservationRepository {

    /**
     * 按预占 ID 查询
     *
     * @param reservationId 预占 ID
     * @return 预占实体；不存在返回 null
     */
    StockReservationEntity findById(String reservationId);

    /**
     * 按 (orderRef, productId) 查询，用于预占幂等
     *
     * @param orderRef  业务幂等键
     * @param productId 商品 ID
     * @return 预占实体；不存在返回 null
     */
    StockReservationEntity findByOrderRefAndProductId(String orderRef, Long productId);

    /**
     * 统计某商品处于 RESERVED 状态的预占总量
     *
     * @param productId 商品 ID
     * @return 已预占数量
     */
    int sumReservedQuantity(Long productId);

    /**
     * 批量统计多个商品处于 RESERVED 状态的预占总量
     *
     * @param productIds 商品 ID 列表
     * @return Map，key 为 productId，value 为已预占数量（无预占的商品不出现在 Map 中）
     */
    java.util.Map<Long, Integer> sumReservedQuantities(java.util.List<Long> productIds);

    /**
     * 保存新的预占记录（id 由调用方预先生成）
     *
     * @param entity 预占实体
     */
    void save(StockReservationEntity entity);

    /**
     * 更新预占状态
     *
     * @param reservationId 预占 ID
     * @param newStatus     新状态
     */
    void updateStatus(String reservationId, ReservationStatus newStatus);
}
