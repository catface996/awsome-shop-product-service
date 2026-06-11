package com.awsome.shop.product.repository.mysql.po.stock;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存预占持久化对象
 *
 * <p>主键即 reservationId（UUID 字符串），使用 {@link IdType#INPUT} 由领域服务显式赋值，
 * 不使用自增——因为该 ID 作为对外契约返回给 Order Service。</p>
 */
@Data
@TableName(value = "stock_reservation")
public class StockReservationPO {

    @TableId(type = IdType.INPUT)
    private String id;

    private String orderRef;

    private Long productId;

    private Integer quantity;

    /**
     * 持久化为字符串：RESERVED / RELEASED / CONFIRMED
     */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;
}
