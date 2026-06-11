package com.awsome.shop.product.common.enums;

/**
 * 库存与商品类型相关业务错误码
 *
 * <p>错误码前缀决定 HTTP 状态码映射，参见 {@link ErrorCode} 接口说明。</p>
 *
 * <p>使用示例：</p>
 * <pre>
 * throw new BusinessException(StockErrorCode.PRODUCT_NOT_FOUND);
 * throw new BusinessException(StockErrorCode.STOCK_INSUFFICIENT, available, requested);
 * </pre>
 */
public enum StockErrorCode implements ErrorCode {

    /**
     * 商品不存在
     */
    PRODUCT_NOT_FOUND("NOT_FOUND_101", "商品不存在"),

    /**
     * 库存预占记录不存在
     */
    RESERVATION_NOT_FOUND("NOT_FOUND_102", "库存预占记录不存在"),

    /**
     * 库存不足（可用 {0}，请求 {1}）
     */
    STOCK_INSUFFICIENT("BIZ_101", "库存不足，可用 {0}，请求 {1}"),

    /**
     * orderRef 已被使用且参数冲突
     */
    RESERVATION_CONFLICT("BIZ_102", "orderRef 已被使用且预占参数冲突"),

    /**
     * 无法释放一个已确认的预占
     */
    RESERVATION_ALREADY_CONFIRMED("BIZ_103", "无法释放一个已确认的预占"),

    /**
     * 无法确认一个已释放的预占
     */
    RESERVATION_ALREADY_RELEASED("BIZ_104", "无法确认一个已释放的预占"),

    /**
     * 商品类型非法（必须为 PHYSICAL 或 VIRTUAL）
     */
    INVALID_PRODUCT_TYPE("PARAM_101", "商品类型必须为 PHYSICAL 或 VIRTUAL"),

    /**
     * 内部接口鉴权失败
     */
    INTERNAL_UNAUTHORIZED("AUTH_101", "内部接口鉴权失败");

    private final String code;
    private final String message;

    StockErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
