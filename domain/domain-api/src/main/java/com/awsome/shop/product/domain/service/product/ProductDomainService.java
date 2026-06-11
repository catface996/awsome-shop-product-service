package com.awsome.shop.product.domain.service.product;

import com.awsome.shop.product.common.dto.PageResult;
import com.awsome.shop.product.domain.model.product.ProductEntity;
import com.awsome.shop.product.domain.model.product.ProductType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Product 领域服务接口
 */
public interface ProductDomainService {

    ProductEntity getById(Long id);

    PageResult<ProductEntity> page(int page, int size, String name, String category);

    ProductEntity create(String name, String sku, String category, ProductType productType, String brand,
                         Integer pointsPrice, BigDecimal marketPrice, Integer stock,
                         Integer status, String description, String imageUrl,
                         String subtitle, String deliveryMethod, String serviceGuarantee,
                         String promotion, String colors, List<Map<String, String>> specs);

    /**
     * 按分类名称统计商品数量
     *
     * @return Map，key 为分类名称，value 为商品数量
     */
    Map<String, Long> countGroupByCategory();

    /**
     * 管理员调整库存（绝对值替换）。
     *
     * @param productId 商品 ID
     * @param newQty    新库存数量（&gt;= 0）
     */
    void adjustStock(Long productId, int newQty);

    /**
     * 更新商品可编辑信息（不含库存正式扣减语义）。
     *
     * @param entity 含 id 的商品实体
     * @return 更新后的商品实体
     */
    ProductEntity update(ProductEntity entity);

    /**
     * 上下架：修改商品 status（0-下架 / 1-上架）。
     *
     * @param productId 商品 ID
     * @param status    新状态
     */
    void changeStatus(Long productId, Integer status);

    /**
     * 软删除商品。
     *
     * @param productId 商品 ID
     */
    void delete(Long productId);

    /**
     * 上传商品图片：存储文件并将访问 URL 回写到商品 imageUrl。
     *
     * @param productId        商品 ID
     * @param content          图片二进制内容
     * @param originalFilename 原始文件名
     * @return 图片访问 URL
     */
    String uploadImage(Long productId, byte[] content, String originalFilename);
}
