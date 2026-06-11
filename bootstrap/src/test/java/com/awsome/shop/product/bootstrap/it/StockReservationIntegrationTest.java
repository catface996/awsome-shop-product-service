package com.awsome.shop.product.bootstrap.it;

import com.awsome.shop.product.bootstrap.Application;
import com.awsome.shop.product.common.exception.BusinessException;
import com.awsome.shop.product.domain.model.product.ProductEntity;
import com.awsome.shop.product.domain.model.product.ProductType;
import com.awsome.shop.product.domain.service.stock.StockDomainService;
import com.awsome.shop.product.repository.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 库存预占领域服务集成测试（真实 MySQL + Flyway + 悲观锁路径）。
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class StockReservationIntegrationTest extends AbstractMysqlIntegrationTest {

    @Autowired
    private StockDomainService stockDomainService;

    @Autowired
    private ProductRepository productRepository;

    private static final AtomicLong SKU_SEQ = new AtomicLong();

    private Long seedProduct(int stock) {
        ProductEntity p = new ProductEntity();
        p.setName("测试商品");
        p.setSku("IT-SKU-" + SKU_SEQ.incrementAndGet() + "-" + System.nanoTime());
        p.setCategory("测试分类");
        p.setProductType(ProductType.PHYSICAL);
        p.setPointsPrice(100);
        p.setStock(stock);
        p.setSoldCount(0);
        p.setStatus(1);
        productRepository.save(p);
        return p.getId();
    }

    @Test
    void reserve_thenConfirm_decrementsStockAndIncrementsSold() {
        Long productId = seedProduct(10);

        String reservationId = stockDomainService.reserveStock(productId, 3, "order-confirm-1");
        assertThat(reservationId).isNotBlank();
        // 预占后可用库存下降，但 stock 列未变
        assertThat(stockDomainService.getAvailableStock(productId)).isEqualTo(7);
        assertThat(productRepository.getById(productId).getStock()).isEqualTo(10);

        stockDomainService.confirmDeduct(reservationId);

        ProductEntity after = productRepository.getById(productId);
        assertThat(after.getStock()).isEqualTo(7);
        assertThat(after.getSoldCount()).isEqualTo(3);
        // 已确认的预占不再计入 RESERVED，可用库存等于 stock
        assertThat(stockDomainService.getAvailableStock(productId)).isEqualTo(7);
    }

    @Test
    void reserve_thenRelease_restoresAvailableStock() {
        Long productId = seedProduct(5);

        String reservationId = stockDomainService.reserveStock(productId, 2, "order-release-1");
        assertThat(stockDomainService.getAvailableStock(productId)).isEqualTo(3);

        stockDomainService.releaseStock(reservationId);

        assertThat(stockDomainService.getAvailableStock(productId)).isEqualTo(5);
        assertThat(productRepository.getById(productId).getStock()).isEqualTo(5);
    }

    @Test
    void reserve_sameOrderRef_isIdempotent() {
        Long productId = seedProduct(10);
        String first = stockDomainService.reserveStock(productId, 2, "order-idem-1");
        String second = stockDomainService.reserveStock(productId, 2, "order-idem-1");
        assertThat(second).isEqualTo(first);
        // 仅占用一次
        assertThat(stockDomainService.getAvailableStock(productId)).isEqualTo(8);
    }

    @Test
    void reserve_insufficientStock_throws() {
        Long productId = seedProduct(1);
        assertThatThrownBy(() -> stockDomainService.reserveStock(productId, 2, "order-insufficient-1"))
                .isInstanceOf(BusinessException.class);
        // 失败不改变库存
        assertThat(stockDomainService.getAvailableStock(productId)).isEqualTo(1);
    }
}
