package com.awsome.shop.product.repository.product;

import com.awsome.shop.product.common.dto.PageResult;
import com.awsome.shop.product.domain.model.product.ProductEntity;

import java.util.Map;

/**
 * Product 仓储接口
 */
public interface ProductRepository {

    ProductEntity getById(Long id);

    ProductEntity getBySku(String sku);

    PageResult<ProductEntity> page(int page, int size, String name, String category);

    void save(ProductEntity entity);

    void update(ProductEntity entity);

    void deleteById(Long id);

    /**
     * 悲观锁查询：SELECT * FROM product WHERE id=? AND deleted=0 FOR UPDATE
     *
     * @param id 商品 ID
     * @return 商品实体；不存在返回 null
     */
    ProductEntity lockById(Long id);

    /**
     * 正式扣减库存并累加已售数量
     * UPDATE product SET stock = stock - ?, sold_count = sold_count + ? WHERE id=? AND deleted=0
     *
     * @param productId 商品 ID
     * @param quantity  扣减数量
     */
    void deductStockAndIncrSold(Long productId, int quantity);

    /**
     * 绝对值替换库存（管理员调整）
     * UPDATE product SET stock = ? WHERE id=? AND deleted=0
     *
     * @param productId 商品 ID
     * @param newQty    新库存数量
     */
    void adjustStock(Long productId, int newQty);

    /**
     * 按分类名称统计商品数量
     *
     * @return Map，key 为分类名称，value 为商品数量
     */
    Map<String, Long> countGroupByCategory();

    /**
     * 统计某分类名称下未删除的商品数量
     *
     * @param category 分类名称
     * @return 商品数量
     */
    long countByCategory(String category);
}
