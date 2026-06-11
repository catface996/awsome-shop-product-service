package com.awsome.shop.product.common.enums;

/**
 * 分类相关业务错误码
 *
 * <p>错误码前缀决定 HTTP 状态码映射，参见 {@link ErrorCode} 接口说明。</p>
 */
public enum CategoryErrorCode implements ErrorCode {

    /**
     * 分类不存在
     */
    CATEGORY_NOT_FOUND("NOT_FOUND_201", "分类不存在"),

    /**
     * 不支持三级分类（仅允许父/子两级）
     */
    CATEGORY_LEVEL_EXCEEDED("BIZ_201", "不支持三级分类，父分类必须是顶级分类"),

    /**
     * 分类下存在子分类或关联商品，无法删除
     */
    CATEGORY_IN_USE("BIZ_202", "分类下存在子分类或关联商品，无法删除");

    private final String code;
    private final String message;

    CategoryErrorCode(String code, String message) {
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
