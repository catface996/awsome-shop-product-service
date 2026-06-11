package com.awsome.shop.product.application.api.dto.product.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页查询 Product 请求
 */
@Data
public class ListProductRequest {

    @Min(value = 1, message = "页码最小为 1")
    private Integer page = 1;

    @Min(value = 1, message = "每页大小最小为 1")
    @Max(value = 100, message = "每页大小最大为 100")
    private Integer size = 20;

    private String name;

    private String category;

    /**
     * 关键字搜索（按商品名称模糊匹配，FR-PR4）
     */
    private String keyword;

    /**
     * 按分类 ID 过滤（FR-PR4）
     */
    private Long categoryId;
}
