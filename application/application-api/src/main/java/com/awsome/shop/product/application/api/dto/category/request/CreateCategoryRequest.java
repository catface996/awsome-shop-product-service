package com.awsome.shop.product.application.api.dto.category.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建分类请求
 */
@Data
public class CreateCategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称不能超过100个字符")
    private String name;

    /**
     * 父分类 ID；为空表示创建顶级分类，非空表示创建二级分类
     */
    private Long parentId;

    @Size(max = 100, message = "图标名称不能超过100个字符")
    private String icon;

    private Integer sortOrder;

    private Integer status;

    @Size(max = 500, message = "分类描述不能超过500个字符")
    private String description;
}
