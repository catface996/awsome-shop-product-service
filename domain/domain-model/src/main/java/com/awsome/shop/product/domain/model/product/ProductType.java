package com.awsome.shop.product.domain.model.product;

/**
 * 商品类型
 */
public enum ProductType {

    /**
     * 实物商品：走"预占 → 发货 → 正式扣减"流程
     */
    PHYSICAL,

    /**
     * 虚拟商品/卡券：下单时立即扣减、即时履约
     */
    VIRTUAL
}
