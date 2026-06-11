package com.awsome.shop.product.domain.impl.service.stock;

import com.awsome.shop.product.common.enums.StockErrorCode;
import com.awsome.shop.product.common.exception.BusinessException;
import com.awsome.shop.product.domain.model.product.ProductEntity;
import com.awsome.shop.product.domain.model.stock.ReservationStatus;
import com.awsome.shop.product.domain.model.stock.StockReservationEntity;
import com.awsome.shop.product.repository.product.ProductRepository;
import com.awsome.shop.product.repository.stock.StockReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * StockDomainServiceImpl 单元测试（Mockito）
 *
 * <p>覆盖：可用库存计算、预占幂等/冲突/不足、释放与确认的状态机分支。</p>
 */
@ExtendWith(MockitoExtension.class)
class StockDomainServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @InjectMocks
    private StockDomainServiceImpl stockDomainService;

    private ProductEntity product(long id, Integer stock) {
        ProductEntity p = new ProductEntity();
        p.setId(id);
        p.setStock(stock);
        return p;
    }

    private StockReservationEntity reservation(String id, Long productId, int qty, ReservationStatus status) {
        StockReservationEntity r = new StockReservationEntity();
        r.setId(id);
        r.setProductId(productId);
        r.setOrderRef("ord-" + id);
        r.setQuantity(qty);
        r.setStatus(status);
        return r;
    }

    // ===== getAvailableStock =====

    @Test
    void getAvailableStock_productNotFound_throwsNotFound() {
        when(productRepository.getById(1L)).thenReturn(null);
        assertThatThrownBy(() -> stockDomainService.getAvailableStock(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(StockErrorCode.PRODUCT_NOT_FOUND.getCode());
    }

    @Test
    void getAvailableStock_returnsStockMinusReserved() {
        when(productRepository.getById(1L)).thenReturn(product(1L, 10));
        when(stockReservationRepository.sumReservedQuantity(1L)).thenReturn(3);
        assertThat(stockDomainService.getAvailableStock(1L)).isEqualTo(7);
    }

    // ===== reserveStock =====

    @Test
    void reserveStock_productNotFound_throwsNotFound() {
        when(productRepository.lockById(1L)).thenReturn(null);
        assertThatThrownBy(() -> stockDomainService.reserveStock(1L, 1, "ord1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(StockErrorCode.PRODUCT_NOT_FOUND.getCode());
    }

    @Test
    void reserveStock_idempotentSameQuantity_returnsExistingIdWithoutSaving() {
        when(productRepository.lockById(1L)).thenReturn(product(1L, 10));
        when(stockReservationRepository.findByOrderRefAndProductId("ord1", 1L))
                .thenReturn(reservation("r1", 1L, 2, ReservationStatus.RESERVED));

        String id = stockDomainService.reserveStock(1L, 2, "ord1");

        assertThat(id).isEqualTo("r1");
        verify(stockReservationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reserveStock_idempotentDifferentQuantity_throwsConflict() {
        when(productRepository.lockById(1L)).thenReturn(product(1L, 10));
        when(stockReservationRepository.findByOrderRefAndProductId("ord1", 1L))
                .thenReturn(reservation("r1", 1L, 2, ReservationStatus.RESERVED));

        assertThatThrownBy(() -> stockDomainService.reserveStock(1L, 3, "ord1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(StockErrorCode.RESERVATION_CONFLICT.getCode());
    }

    @Test
    void reserveStock_quantityExceedsAvailable_throwsInsufficient() {
        when(productRepository.lockById(1L)).thenReturn(product(1L, 5));
        when(stockReservationRepository.findByOrderRefAndProductId("ord1", 1L)).thenReturn(null);
        when(stockReservationRepository.sumReservedQuantity(1L)).thenReturn(4);

        assertThatThrownBy(() -> stockDomainService.reserveStock(1L, 2, "ord1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(StockErrorCode.STOCK_INSUFFICIENT.getCode());
    }

    @Test
    void reserveStock_success_savesReservedAndReturnsGeneratedId() {
        when(productRepository.lockById(1L)).thenReturn(product(1L, 10));
        when(stockReservationRepository.findByOrderRefAndProductId("ord2", 1L)).thenReturn(null);
        when(stockReservationRepository.sumReservedQuantity(1L)).thenReturn(0);

        String id = stockDomainService.reserveStock(1L, 3, "ord2");

        ArgumentCaptor<StockReservationEntity> captor = ArgumentCaptor.forClass(StockReservationEntity.class);
        verify(stockReservationRepository).save(captor.capture());
        StockReservationEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(saved.getQuantity()).isEqualTo(3);
        assertThat(saved.getOrderRef()).isEqualTo("ord2");
        assertThat(saved.getProductId()).isEqualTo(1L);
        assertThat(saved.getId()).isNotBlank();
        assertThat(id).isEqualTo(saved.getId());
    }

    // ===== releaseStock =====

    @Test
    void releaseStock_notFound_throwsNotFound() {
        when(stockReservationRepository.findById("x")).thenReturn(null);
        assertThatThrownBy(() -> stockDomainService.releaseStock("x"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(StockErrorCode.RESERVATION_NOT_FOUND.getCode());
    }

    @Test
    void releaseStock_confirmed_throwsAlreadyConfirmed() {
        when(stockReservationRepository.findById("r1"))
                .thenReturn(reservation("r1", 1L, 2, ReservationStatus.CONFIRMED));
        assertThatThrownBy(() -> stockDomainService.releaseStock("r1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(StockErrorCode.RESERVATION_ALREADY_CONFIRMED.getCode());
    }

    @Test
    void releaseStock_alreadyReleased_isNoop() {
        when(stockReservationRepository.findById("r1"))
                .thenReturn(reservation("r1", 1L, 2, ReservationStatus.RELEASED));
        stockDomainService.releaseStock("r1");
        verify(stockReservationRepository, never()).updateStatus(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void releaseStock_reserved_transitionsToReleased() {
        when(stockReservationRepository.findById("r1"))
                .thenReturn(reservation("r1", 1L, 2, ReservationStatus.RESERVED));
        stockDomainService.releaseStock("r1");
        verify(stockReservationRepository).updateStatus("r1", ReservationStatus.RELEASED);
    }

    // ===== confirmDeduct =====

    @Test
    void confirmDeduct_notFound_throwsNotFound() {
        when(stockReservationRepository.findById("x")).thenReturn(null);
        assertThatThrownBy(() -> stockDomainService.confirmDeduct("x"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(StockErrorCode.RESERVATION_NOT_FOUND.getCode());
    }

    @Test
    void confirmDeduct_released_throwsAlreadyReleased() {
        when(stockReservationRepository.findById("r1"))
                .thenReturn(reservation("r1", 1L, 2, ReservationStatus.RELEASED));
        assertThatThrownBy(() -> stockDomainService.confirmDeduct("r1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(StockErrorCode.RESERVATION_ALREADY_RELEASED.getCode());
    }

    @Test
    void confirmDeduct_alreadyConfirmed_isNoop() {
        when(stockReservationRepository.findById("r1"))
                .thenReturn(reservation("r1", 1L, 2, ReservationStatus.CONFIRMED));
        stockDomainService.confirmDeduct("r1");
        verify(productRepository, never()).deductStockAndIncrSold(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt());
        verify(stockReservationRepository, never()).updateStatus(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void confirmDeduct_reserved_deductsAndConfirms() {
        when(stockReservationRepository.findById("r1"))
                .thenReturn(reservation("r1", 5L, 2, ReservationStatus.RESERVED));
        stockDomainService.confirmDeduct("r1");
        verify(productRepository).lockById(5L);
        verify(productRepository).deductStockAndIncrSold(5L, 2);
        verify(stockReservationRepository).updateStatus("r1", ReservationStatus.CONFIRMED);
    }
}
