package com.awsome.shop.product.application.api.dto.stock.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 库存释放请求
 */
@Data
public class ReleaseStockRequest {

    @NotBlank(message = "reservationId 不能为空")
    private String reservationId;
}
