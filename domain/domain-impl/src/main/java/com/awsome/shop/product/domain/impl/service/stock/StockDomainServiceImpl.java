package com.awsome.shop.product.domain.impl.service.stock;

import com.awsome.shop.product.common.enums.StockErrorCode;
import com.awsome.shop.product.common.exception.BusinessException;
import com.awsome.shop.product.domain.model.product.ProductEntity;
import com.awsome.shop.product.domain.model.stock.ReservationStatus;
import com.awsome.shop.product.domain.model.stock.StockReservationEntity;
import com.awsome.shop.product.domain.service.stock.StockDomainService;
import com.awsome.shop.product.repository.product.ProductRepository;
import com.awsome.shop.product.repository.stock.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 库存领域服务实现
 *
 * <p>悲观锁路径走 ProductRepository.lockById（Mapper XML 中的 SELECT ... FOR UPDATE），
 * 串行化同一商品的并发预占，防止超兑（NFR-4 / BR-8）。</p>
 */
@Service
@RequiredArgsConstructor
public class StockDomainServiceImpl implements StockDomainService {

    private final ProductRepository productRepository;
    private final StockReservationRepository stockReservationRepository;

    @Override
    public int getAvailableStock(Long productId) {
        ProductEntity product = productRepository.getById(productId);
        if (product == null) {
            throw new BusinessException(StockErrorCode.PRODUCT_NOT_FOUND);
        }
        int reserved = stockReservationRepository.sumReservedQuantity(productId);
        return safeStock(product) - reserved;
    }

    @Override
    @Transactional
    public String reserveStock(Long productId, int quantity, String orderRef) {
        // 1. 悲观锁锁定商品行，串行化同一商品的并发预占（同时校验商品存在）
        ProductEntity product = productRepository.lockById(productId);
        if (product == null) {
            throw new BusinessException(StockErrorCode.PRODUCT_NOT_FOUND);
        }

        // 2. 幂等：锁内检查是否已存在相同 (orderRef, productId) 的预占
        StockReservationEntity existing =
                stockReservationRepository.findByOrderRefAndProductId(orderRef, productId);
        if (existing != null) {
            if (!existing.getQuantity().equals(quantity)) {
                throw new BusinessException(StockErrorCode.RESERVATION_CONFLICT);
            }
            return existing.getId();
        }

        // 3. 计算可用库存并校验
        int reserved = stockReservationRepository.sumReservedQuantity(productId);
        int available = safeStock(product) - reserved;
        if (quantity > available) {
            throw new BusinessException(StockErrorCode.STOCK_INSUFFICIENT, available, quantity);
        }

        // 4. 写入 RESERVED 预占记录
        String reservationId = UUID.randomUUID().toString();
        StockReservationEntity entity = new StockReservationEntity();
        entity.setId(reservationId);
        entity.setOrderRef(orderRef);
        entity.setProductId(productId);
        entity.setQuantity(quantity);
        entity.setStatus(ReservationStatus.RESERVED);
        stockReservationRepository.save(entity);
        return reservationId;
    }

    @Override
    @Transactional
    public void releaseStock(String reservationId) {
        StockReservationEntity reservation = stockReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new BusinessException(StockErrorCode.RESERVATION_NOT_FOUND);
        }
        switch (reservation.getStatus()) {
            case RELEASED -> {
                // 幂等 no-op
            }
            case CONFIRMED -> throw new BusinessException(StockErrorCode.RESERVATION_ALREADY_CONFIRMED);
            case RESERVED -> stockReservationRepository.updateStatus(reservationId, ReservationStatus.RELEASED);
            default -> throw new BusinessException(StockErrorCode.RESERVATION_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public void confirmDeduct(String reservationId) {
        StockReservationEntity reservation = stockReservationRepository.findById(reservationId);
        if (reservation == null) {
            throw new BusinessException(StockErrorCode.RESERVATION_NOT_FOUND);
        }
        switch (reservation.getStatus()) {
            case CONFIRMED -> {
                // 幂等 no-op
            }
            case RELEASED -> throw new BusinessException(StockErrorCode.RESERVATION_ALREADY_RELEASED);
            case RESERVED -> {
                // 锁定商品行后正式扣减并累加已售
                productRepository.lockById(reservation.getProductId());
                productRepository.deductStockAndIncrSold(reservation.getProductId(), reservation.getQuantity());
                stockReservationRepository.updateStatus(reservationId, ReservationStatus.CONFIRMED);
            }
            default -> throw new BusinessException(StockErrorCode.RESERVATION_NOT_FOUND);
        }
    }

    private int safeStock(ProductEntity product) {
        return product.getStock() == null ? 0 : product.getStock();
    }

    @Override
    public java.util.Map<Long, Integer> getReservedQuantities(java.util.List<Long> productIds) {
        return stockReservationRepository.sumReservedQuantities(productIds);
    }
}
