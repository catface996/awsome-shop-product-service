package com.awsome.shop.product.application.api.dto.category.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 删除分类请求
 */
@Data
public class DeleteCategoryRequest {

    @NotNull(message = "分类ID不能为空")
    private Long id;
}
