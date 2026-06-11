-- 为 product 表新增商品类型字段（PHYSICAL-实物 / VIRTUAL-虚拟）
-- 关联需求：Req 1（FR-PR2 / AS-1~3）

ALTER TABLE `product`
    ADD COLUMN `product_type` VARCHAR(16) NOT NULL DEFAULT 'PHYSICAL'
        COMMENT '商品类型 PHYSICAL-实物 VIRTUAL-虚拟' AFTER `category`;

-- 显式回填存量数据（DEFAULT 'PHYSICAL' 已保证非空，此条用于清晰审计）
UPDATE `product` SET `product_type` = 'PHYSICAL'
    WHERE `product_type` IS NULL OR `product_type` = '';

-- 取值约束（MySQL 8.0+ 强制执行）
ALTER TABLE `product`
    ADD CONSTRAINT `chk_product_type`
        CHECK (`product_type` IN ('PHYSICAL', 'VIRTUAL'));
