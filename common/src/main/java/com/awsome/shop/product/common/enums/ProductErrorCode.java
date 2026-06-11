package com.awsome.shop.product.common.enums;

/**
 * 商品相关错误码
 *
 * <p>错误码前缀决定 HTTP 状态码映射，参见 {@link ErrorCode} 接口说明。</p>
 */
public enum ProductErrorCode implements ErrorCode {

    /**
     * 上传文件不是图片类型
     */
    INVALID_IMAGE_TYPE("PARAM_301", "上传文件必须为图片类型"),

    /**
     * 图片存储失败
     */
    IMAGE_UPLOAD_FAILED("SYS_301", "图片上传失败，请稍后重试");

    private final String code;
    private final String message;

    ProductErrorCode(String code, String message) {
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
