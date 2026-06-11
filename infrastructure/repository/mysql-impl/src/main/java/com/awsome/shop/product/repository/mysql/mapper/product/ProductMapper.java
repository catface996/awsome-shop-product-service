package com.awsome.shop.product.repository.mysql.mapper.product;

import com.awsome.shop.product.repository.mysql.po.product.ProductPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * Product Mapper 接口
 */
@Mapper
public interface ProductMapper extends BaseMapper<ProductPO> {

    /**
     * 分页查询
     *
     * @param page     MyBatis-Plus 分页对象
     * @param name     名称模糊查询条件（可为 null）
     * @param category 分类精确筛选条件（可为 null）
     * @return 分页结果
     */
    IPage<ProductPO> selectPage(IPage<ProductPO> page, @Param("name") String name, @Param("category") String category);

    /**
     * 按分类名称统计商品数量
     *
     * @return Map，key 为分类名称，value 为商品数量
     */
    @MapKey("category")
    Map<String, Map<String, Object>> countGroupByCategory();

    /**
     * 统计某分类名称下未删除的商品数量
     *
     * @param category 分类名称
     * @return 商品数量
     */
    long countByCategory(@Param("category") String category);

    /**
     * 悲观锁查询：按 ID 加行级排他锁（FOR UPDATE）
     *
     * @param id 商品 ID
     * @return 商品 PO；不存在返回 null
     */
    ProductPO selectByIdForUpdate(@Param("id") Long id);

    /**
     * 正式扣减库存并累加已售数量
     *
     * @param productId 商品 ID
     * @param quantity  扣减数量
     * @return 受影响行数
     */
    int deductStockAndIncrSold(@Param("productId") Long productId, @Param("quantity") int quantity);

    /**
     * 绝对值替换库存
     *
     * @param productId 商品 ID
     * @param newQty    新库存数量
     * @return 受影响行数
     */
    int adjustStock(@Param("productId") Long productId, @Param("newQty") int newQty);
}
