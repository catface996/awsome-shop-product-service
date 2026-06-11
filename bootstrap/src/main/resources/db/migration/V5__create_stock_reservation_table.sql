-- 库存预占表：持久化兑换 Saga 的预占凭据
-- 关联需求：Req 3/4/8（FR-PR7 / BR-6 / NFR-4/5）

CREATE TABLE `stock_reservation` (
    `id`         VARCHAR(36)   NOT NULL COMMENT '预占ID(UUID)',
    `order_ref`  VARCHAR(64)   NOT NULL COMMENT '调用方业务幂等键',
    `product_id` BIGINT        NOT NULL COMMENT '商品ID',
    `quantity`   INT           NOT NULL COMMENT '预占数量(>0)',
    `status`     VARCHAR(16)   NOT NULL COMMENT '状态 RESERVED/RELEASED/CONFIRMED',
    `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT                 DEFAULT NULL COMMENT '创建人',
    `updated_by` BIGINT                 DEFAULT NULL COMMENT '更新人',
    `deleted`    TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    `version`    INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_order_ref_product_id` (`order_ref`, `product_id`),
    INDEX `idx_product_status` (`product_id`, `status`),
    CONSTRAINT `chk_reservation_status`
        CHECK (`status` IN ('RESERVED', 'RELEASED', 'CONFIRMED')),
    CONSTRAINT `chk_quantity_positive`
        CHECK (`quantity` > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci
  COMMENT = '库存预占表';
