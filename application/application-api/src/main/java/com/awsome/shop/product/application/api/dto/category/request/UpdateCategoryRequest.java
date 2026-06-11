package com.awsome.shop.product.application.api.dto.category.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新分类请求
 */
@Data
public class UpdateCategoryRequest {

    @NotNull(message = "分类ID不能为空")
    private Long id;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称不能超过100个字符")
    private String name;

    @Size(max = 100, message = "图标名称不能超过100个字符")
    private String icon;

    private Integer sortOrder;

    private Integer status;

    @Size(max = 500, message = "分类描述不能超过500个字符")
    private String description;
}
