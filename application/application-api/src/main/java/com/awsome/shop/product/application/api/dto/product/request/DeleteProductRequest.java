package com.awsome.shop.product.application.api.dto.product.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 删除 Product 请求
 */
@Data
public class DeleteProductRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;
}
