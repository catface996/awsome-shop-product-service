package com.awsome.shop.product.domain.model.stock;

/**
 * 库存预占状态
 *
 * <p>状态机：RESERVED 为初始非终态；RELEASED / CONFIRMED 为终态，不可再迁移。</p>
 * <p>合法迁移：RESERVED → RELEASED、RESERVED → CONFIRMED。</p>
 */
public enum ReservationStatus {

    /**
     * 预占成功（初始状态，可流转）
     */
    RESERVED,

    /**
     * 已释放（终态）
     */
    RELEASED,

    /**
     * 已正式扣减（终态）
     */
    CONFIRMED;

    /**
     * 是否为终态（不可再迁移）
     *
     * @return RELEASED / CONFIRMED 返回 true
     */
    public boolean isTerminal() {
        return this == RELEASED || this == CONFIRMED;
    }
}
