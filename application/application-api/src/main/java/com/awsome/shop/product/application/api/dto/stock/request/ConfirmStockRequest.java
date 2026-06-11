package com.awsome.shop.product.application.api.dto.stock.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 库存正式扣减（确认）请求
 */
@Data
public class ConfirmStockRequest {

    @NotBlank(message = "reservationId 不能为空")
    private String reservationId;
}
