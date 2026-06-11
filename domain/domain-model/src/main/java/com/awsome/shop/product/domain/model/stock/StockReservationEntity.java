package com.awsome.shop.product.domain.model.stock;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存预占领域实体
 */
@Data
public class StockReservationEntity {

    /**
     * 业务主键，UUID 字符串，由领域服务在预占成功时生成
     */
    private String id;

    /**
     * 调用方业务幂等键
     */
    private String orderRef;

    private Long productId;

    private Integer quantity;

    private ReservationStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;

    private Integer deleted;

    private Integer version;
}
