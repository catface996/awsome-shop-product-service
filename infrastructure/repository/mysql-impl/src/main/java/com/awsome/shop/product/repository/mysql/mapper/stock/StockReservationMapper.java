package com.awsome.shop.product.repository.mysql.mapper.stock;

import com.awsome.shop.product.repository.mysql.po.stock.StockReservationPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * StockReservation Mapper 接口
 */
@Mapper
public interface StockReservationMapper extends BaseMapper<StockReservationPO> {

    /**
     * 按 (orderRef, productId) 查询预占记录（命中唯一索引 uk_order_ref_product_id）
     *
     * @param orderRef  业务幂等键
     * @param productId 商品 ID
     * @return 预占 PO；不存在返回 null
     */
    StockReservationPO selectByOrderRefAndProductId(@Param("orderRef") String orderRef,
                                                    @Param("productId") Long productId);

    /**
     * 统计某商品处于 RESERVED 状态的预占总量
     *
     * @param productId 商品 ID
     * @return 已预占数量
     */
    int sumReservedQuantity(@Param("productId") Long productId);

    /**
     * 批量统计多个商品 RESERVED 状态的预占总量
     *
     * @param productIds 商品 ID 列表（非空）
     * @return 每行包含 productId 与 total
     */
    java.util.List<java.util.Map<String, Object>> sumReservedByProductIds(
            @Param("productIds") java.util.List<Long> productIds);

    /**
     * 更新预占状态
     *
     * @param reservationId 预占 ID
     * @param status        新状态
     * @return 受影响行数
     */
    int updateStatusById(@Param("reservationId") String reservationId, @Param("status") String status);
}
