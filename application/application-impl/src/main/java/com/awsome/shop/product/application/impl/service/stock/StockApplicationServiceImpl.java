package com.awsome.shop.product.application.impl.service.stock;

import com.awsome.shop.product.application.api.dto.stock.ReserveStockResponse;
import com.awsome.shop.product.application.api.dto.stock.StockQueryResponse;
import com.awsome.shop.product.application.api.dto.stock.request.ReserveStockRequest;
import com.awsome.shop.product.application.api.service.stock.StockApplicationService;
import com.awsome.shop.product.domain.model.product.ProductEntity;
import com.awsome.shop.product.domain.service.product.ProductDomainService;
import com.awsome.shop.product.domain.service.stock.StockDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Stock 应用服务实现
 *
 * <p>仅依赖 Domain Service，不直接依赖 Repository；事务边界由 Domain 层控制。</p>
 */
@Service
@RequiredArgsConstructor
public class StockApplicationServiceImpl implements StockApplicationService {

    private final StockDomainService stockDomainService;
    private final ProductDomainService productDomainService;

    @Override
    public StockQueryResponse getAvailableStock(Long productId) {
        // 校验商品存在并取得商品类型
        ProductEntity product = productDomainService.getById(productId);
        int available = stockDomainService.getAvailableStock(productId);

        StockQueryResponse response = new StockQueryResponse();
        response.setProductId(productId);
        response.setAvailableStock(available);
        response.setProductType(product.getProductType() == null ? null : product.getProductType().name());
        return response;
    }

    @Override
    public ReserveStockResponse reserve(ReserveStockRequest request) {
        String reservationId = stockDomainService.reserveStock(
                request.getProductId(), request.getQuantity(), request.getOrderRef());

        ReserveStockResponse response = new ReserveStockResponse();
        response.setReservationId(reservationId);
        return response;
    }

    @Override
    public void release(String reservationId) {
        stockDomainService.releaseStock(reservationId);
    }

    @Override
    public void confirm(String reservationId) {
        stockDomainService.confirmDeduct(reservationId);
    }
}
