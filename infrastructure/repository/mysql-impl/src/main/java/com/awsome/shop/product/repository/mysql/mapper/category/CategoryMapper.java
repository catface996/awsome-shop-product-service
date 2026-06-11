package com.awsome.shop.product.repository.mysql.mapper.category;

import com.awsome.shop.product.repository.mysql.po.category.CategoryPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Category Mapper 接口
 */
@Mapper
public interface CategoryMapper extends BaseMapper<CategoryPO> {

    /**
     * 查询类目列表
     *
     * @param name   名称模糊查询条件（可为 null）
     * @param status 状态筛选条件（可为 null）
     * @return 类目列表
     */
    List<CategoryPO> selectList(@Param("name") String name, @Param("status") Integer status);

    /**
     * 统计某分类的直接子分类数量（未删除）
     *
     * @param parentId 父分类 ID
     * @return 子分类数量
     */
    long countChildren(@Param("parentId") Long parentId);
}
