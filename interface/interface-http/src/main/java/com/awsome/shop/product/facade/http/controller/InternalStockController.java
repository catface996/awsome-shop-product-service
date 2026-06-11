package com.awsome.shop.product.facade.http.controller;

import com.awsome.shop.product.application.api.dto.stock.ReserveStockResponse;
import com.awsome.shop.product.application.api.dto.stock.StockQueryResponse;
import com.awsome.shop.product.application.api.dto.stock.request.ConfirmStockRequest;
import com.awsome.shop.product.application.api.dto.stock.request.ReleaseStockRequest;
import com.awsome.shop.product.application.api.dto.stock.request.ReserveStockRequest;
import com.awsome.shop.product.application.api.dto.stock.request.StockQueryRequest;
import com.awsome.shop.product.application.api.service.stock.StockApplicationService;
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
 * 库存预占内部接口 Controller
 *
 * <p>仅供其他微服务（如 Order Service）内部调用，受 InternalAuthFilter 保护。</p>
 */
@Tag(name = "InternalStock", description = "库存预占内部接口")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InternalStockController {

    private final StockApplicationService stockApplicationService;

    @Operation(summary = "查询可用库存与商品类型")
    @PostMapping("/private/stock/get")
    public Result<StockQueryResponse> get(@RequestBody @Valid StockQueryRequest request) {
        return Result.success(stockApplicationService.getAvailableStock(request.getProductId()));
    }

    @Operation(summary = "预占库存")
    @PostMapping("/private/stock/reserve")
    public Result<ReserveStockResponse> reserve(@RequestBody @Valid ReserveStockRequest request) {
        return Result.success(stockApplicationService.reserve(request));
    }

    @Operation(summary = "释放预占")
    @PostMapping("/private/stock/release")
    public Result<Void> release(@RequestBody @Valid ReleaseStockRequest request) {
        stockApplicationService.release(request.getReservationId());
        return Result.success();
    }

    @Operation(summary = "正式扣减（确认）预占")
    @PostMapping("/private/stock/confirm")
    public Result<Void> confirm(@RequestBody @Valid ConfirmStockRequest request) {
        stockApplicationService.confirm(request.getReservationId());
        return Result.success();
    }
}
