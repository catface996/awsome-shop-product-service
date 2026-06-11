package com.awsome.shop.product.application.api.service.product;

import com.awsome.shop.product.application.api.dto.product.ProductDTO;
import com.awsome.shop.product.application.api.dto.product.request.AdjustStockRequest;
import com.awsome.shop.product.application.api.dto.product.request.ChangeStatusRequest;
import com.awsome.shop.product.application.api.dto.product.request.CreateProductRequest;
import com.awsome.shop.product.application.api.dto.product.request.DeleteProductRequest;
import com.awsome.shop.product.application.api.dto.product.request.GetProductRequest;
import com.awsome.shop.product.application.api.dto.product.request.ListProductRequest;
import com.awsome.shop.product.application.api.dto.product.request.UpdateProductRequest;
import com.awsome.shop.product.common.dto.PageResult;

/**
 * Product 应用服务接口
 */
public interface ProductApplicationService {

    PageResult<ProductDTO> list(ListProductRequest request);

    ProductDTO create(CreateProductRequest request);

    /**
     * 商品详情查询
     *
     * @param request 含 productId
     * @return 商品 DTO
     */
    ProductDTO getById(GetProductRequest request);

    /**
     * 编辑商品
     *
     * @param request 更新请求
     * @return 更新后的商品 DTO
     */
    ProductDTO update(UpdateProductRequest request);

    /**
     * 上下架
     *
     * @param request 含 productId 与 status
     */
    void changeStatus(ChangeStatusRequest request);

    /**
     * 删除商品（软删）
     *
     * @param request 含 productId
     */
    void delete(DeleteProductRequest request);

    /**
     * 上传商品图片
     *
     * @param productId        商品 ID
     * @param content          图片二进制内容
     * @param originalFilename 原始文件名
     * @param contentType      文件 MIME 类型（用于校验是否为图片）
     * @return 图片访问 URL
     */
    String uploadImage(Long productId, byte[] content, String originalFilename, String contentType);

    /**
     * 管理员调整库存（绝对值替换）
     *
     * @param request 调整库存请求
     */
    void adjustStock(AdjustStockRequest request);
}
