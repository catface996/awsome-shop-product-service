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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发预占测试：验证悲观锁防超兑（NFR-4 / BR-8 / Req 7）。
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class ConcurrentReserveIntegrationTest extends AbstractMysqlIntegrationTest {

    @Autowired
    private StockDomainService stockDomainService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void concurrentReserve_neverOversells() throws InterruptedException {
        // 准备：库存 5
        ProductEntity p = new ProductEntity();
        p.setName("稀缺商品");
        p.setSku("IT-CONC-" + System.nanoTime());
        p.setCategory("测试分类");
        p.setProductType(ProductType.PHYSICAL);
        p.setPointsPrice(100);
        p.setStock(5);
        p.setSoldCount(0);
        p.setStatus(1);
        productRepository.save(p);
        Long productId = p.getId();

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    stockDomainService.reserveStock(productId, 1, "conc-order-" + idx);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    insufficient.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        done.await(60, TimeUnit.SECONDS);
        pool.shutdownNow();

        // 恰好 5 个成功，其余 15 个库存不足
        assertThat(success.get()).isEqualTo(5);
        assertThat(insufficient.get()).isEqualTo(threads - 5);
        // RESERVED 总量为 5，stock 列未被扣减
        assertThat(stockDomainService.getAvailableStock(productId)).isEqualTo(0);
        assertThat(productRepository.getById(productId).getStock()).isEqualTo(5);
    }
}
