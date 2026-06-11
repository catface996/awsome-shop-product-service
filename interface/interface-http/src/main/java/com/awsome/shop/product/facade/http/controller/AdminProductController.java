package com.awsome.shop.product.facade.http.controller;

import com.awsome.shop.product.application.api.dto.product.request.AdjustStockRequest;
import com.awsome.shop.product.application.api.service.product.ProductApplicationService;
import com.awsome.shop.product.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品管理（管理员）Controller
 *
 * <p>经 API Gateway 调用，角色鉴权由网关的 RoleAuthorizationFilter 校验 ADMIN。</p>
 */
@Tag(name = "AdminProduct", description = "商品管理（管理员）")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductApplicationService productApplicationService;

    @Operation(summary = "调整商品库存")
    @PostMapping("/public/product/adjust-stock")
    public Result<Void> adjustStock(@RequestBody @Valid AdjustStockRequest request) {
        productApplicationService.adjustStock(request);
        return Result.success();
    }
}
