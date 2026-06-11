package com.awsome.shop.product.repository.mysql.impl.stock;

import com.awsome.shop.product.domain.model.stock.ReservationStatus;
import com.awsome.shop.product.domain.model.stock.StockReservationEntity;
import com.awsome.shop.product.repository.mysql.mapper.stock.StockReservationMapper;
import com.awsome.shop.product.repository.mysql.po.stock.StockReservationPO;
import com.awsome.shop.product.repository.stock.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * StockReservation 仓储实现
 */
@Repository
@RequiredArgsConstructor
public class StockReservationRepositoryImpl implements StockReservationRepository {

    private final StockReservationMapper stockReservationMapper;

    @Override
    public StockReservationEntity findById(String reservationId) {
        StockReservationPO po = stockReservationMapper.selectById(reservationId);
        return po == null ? null : toEntity(po);
    }

    @Override
    public StockReservationEntity findByOrderRefAndProductId(String orderRef, Long productId) {
        StockReservationPO po = stockReservationMapper.selectByOrderRefAndProductId(orderRef, productId);
        return po == null ? null : toEntity(po);
    }

    @Override
    public int sumReservedQuantity(Long productId) {
        return stockReservationMapper.sumReservedQuantity(productId);
    }

    @Override
    public java.util.Map<Long, Integer> sumReservedQuantities(java.util.List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        java.util.List<java.util.Map<String, Object>> rows =
                stockReservationMapper.sumReservedByProductIds(productIds);
        java.util.Map<Long, Integer> result = new java.util.HashMap<>();
        for (java.util.Map<String, Object> row : rows) {
            Object pid = row.get("productId");
            Object total = row.get("total");
            if (pid instanceof Number && total instanceof Number) {
                result.put(((Number) pid).longValue(), ((Number) total).intValue());
            }
        }
        return result;
    }

    @Override
    public void save(StockReservationEntity entity) {
        stockReservationMapper.insert(toPO(entity));
    }

    @Override
    public void updateStatus(String reservationId, ReservationStatus newStatus) {
        stockReservationMapper.updateStatusById(reservationId, newStatus.name());
    }

    private StockReservationEntity toEntity(StockReservationPO po) {
        StockReservationEntity entity = new StockReservationEntity();
        entity.setId(po.getId());
        entity.setOrderRef(po.getOrderRef());
        entity.setProductId(po.getProductId());
        entity.setQuantity(po.getQuantity());
        entity.setStatus(po.getStatus() == null ? null : ReservationStatus.valueOf(po.getStatus()));
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        entity.setCreatedBy(po.getCreatedBy());
        entity.setUpdatedBy(po.getUpdatedBy());
        entity.setDeleted(po.getDeleted());
        entity.setVersion(po.getVersion());
        return entity;
    }

    private StockReservationPO toPO(StockReservationEntity entity) {
        StockReservationPO po = new StockReservationPO();
        po.setId(entity.getId());
        po.setOrderRef(entity.getOrderRef());
        po.setProductId(entity.getProductId());
        po.setQuantity(entity.getQuantity());
        po.setStatus(entity.getStatus() == null ? null : entity.getStatus().name());
        return po;
    }
}
