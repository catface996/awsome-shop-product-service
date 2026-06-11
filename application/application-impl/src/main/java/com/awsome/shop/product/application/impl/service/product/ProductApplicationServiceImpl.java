package com.awsome.shop.product.application.impl.service.product;

import com.awsome.shop.product.application.api.dto.product.ProductDTO;
import com.awsome.shop.product.application.api.dto.product.request.AdjustStockRequest;
import com.awsome.shop.product.application.api.dto.product.request.ChangeStatusRequest;
import com.awsome.shop.product.application.api.dto.product.request.CreateProductRequest;
import com.awsome.shop.product.application.api.dto.product.request.DeleteProductRequest;
import com.awsome.shop.product.application.api.dto.product.request.GetProductRequest;
import com.awsome.shop.product.application.api.dto.product.request.ListProductRequest;
import com.awsome.shop.product.application.api.dto.product.request.UpdateProductRequest;
import com.awsome.shop.product.application.api.service.product.ProductApplicationService;
import com.awsome.shop.product.common.dto.PageResult;
import com.awsome.shop.product.common.enums.ProductErrorCode;
import com.awsome.shop.product.common.exception.ParameterException;
import com.awsome.shop.product.domain.model.category.CategoryEntity;
import com.awsome.shop.product.domain.model.product.ProductEntity;
import com.awsome.shop.product.domain.model.product.ProductType;
import com.awsome.shop.product.domain.service.category.CategoryDomainService;
import com.awsome.shop.product.domain.service.product.ProductDomainService;
import com.awsome.shop.product.domain.service.stock.StockDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Product 应用服务实现
 *
 * <p>只依赖 Domain Service，不直接依赖 Repository</p>
 */
@Service
@RequiredArgsConstructor
public class ProductApplicationServiceImpl implements ProductApplicationService {

    private final ProductDomainService productDomainService;
    private final StockDomainService stockDomainService;
    private final CategoryDomainService categoryDomainService;

    @Override
    public PageResult<ProductDTO> list(ListProductRequest request) {
        // 关键字优先于 name；categoryId 解析为分类名称用于过滤
        String effName = (request.getKeyword() != null && !request.getKeyword().isBlank())
                ? request.getKeyword() : request.getName();
        String effCategory = request.getCategory();
        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryDomainService.getById(request.getCategoryId());
            effCategory = category.getName();
        }

        PageResult<ProductEntity> page = productDomainService.page(
                request.getPage(), request.getSize(), effName, effCategory);

        // 批量查询已预占数量，派生 soldOut（避免 N+1）
        List<Long> ids = page.getRecords().stream().map(ProductEntity::getId).collect(Collectors.toList());
        Map<Long, Integer> reserved = ids.isEmpty()
                ? Collections.emptyMap() : stockDomainService.getReservedQuantities(ids);

        return page.convert(entity -> {
            ProductDTO dto = toDTO(entity);
            int available = safeStock(entity) - reserved.getOrDefault(entity.getId(), 0);
            dto.setSoldOut(available <= 0);
            return dto;
        });
    }

    @Override
    public ProductDTO create(CreateProductRequest request) {
        ProductType productType = ProductType.valueOf(request.getProductType());
        ProductEntity entity = productDomainService.create(
                request.getName(), request.getSku(), request.getCategory(), productType, request.getBrand(),
                request.getPointsPrice(), request.getMarketPrice(), request.getStock(),
                request.getStatus(), request.getDescription(), request.getImageUrl(),
                request.getSubtitle(), request.getDeliveryMethod(), request.getServiceGuarantee(),
                request.getPromotion(), request.getColors(), request.getSpecs());
        return toDTO(entity);
    }

    @Override
    public ProductDTO getById(GetProductRequest request) {
        ProductEntity entity = productDomainService.getById(request.getProductId());
        ProductDTO dto = toDTO(entity);
        int available = stockDomainService.getAvailableStock(entity.getId());
        dto.setSoldOut(available <= 0);
        return dto;
    }

    @Override
    public ProductDTO update(UpdateProductRequest request) {
        ProductEntity entity = new ProductEntity();
        entity.setId(request.getProductId());
        entity.setName(request.getName());
        entity.setCategory(request.getCategory());
        entity.setProductType(request.getProductType() == null ? null : ProductType.valueOf(request.getProductType()));
        entity.setBrand(request.getBrand());
        entity.setPointsPrice(request.getPointsPrice());
        entity.setMarketPrice(request.getMarketPrice());
        entity.setStatus(request.getStatus());
        entity.setDescription(request.getDescription());
        entity.setImageUrl(request.getImageUrl());
        entity.setSubtitle(request.getSubtitle());
        entity.setDeliveryMethod(request.getDeliveryMethod());
        entity.setServiceGuarantee(request.getServiceGuarantee());
        entity.setPromotion(request.getPromotion());
        entity.setColors(request.getColors());
        entity.setSpecs(request.getSpecs());
        return toDTO(productDomainService.update(entity));
    }

    @Override
    public void changeStatus(ChangeStatusRequest request) {
        productDomainService.changeStatus(request.getProductId(), request.getStatus());
    }

    @Override
    public void delete(DeleteProductRequest request) {
        productDomainService.delete(request.getProductId());
    }

    @Override
    public void adjustStock(AdjustStockRequest request) {
        productDomainService.adjustStock(request.getProductId(), request.getNewQty());
    }

    @Override
    public String uploadImage(Long productId, byte[] content, String originalFilename, String contentType) {
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new ParameterException(ProductErrorCode.INVALID_IMAGE_TYPE);
        }
        return productDomainService.uploadImage(productId, content, originalFilename);
    }

    private int safeStock(ProductEntity entity) {
        return entity.getStock() == null ? 0 : entity.getStock();
    }

    private ProductDTO toDTO(ProductEntity entity) {
        ProductDTO dto = new ProductDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSku(entity.getSku());
        dto.setCategory(entity.getCategory());
        dto.setProductType(entity.getProductType() == null ? null : entity.getProductType().name());
        dto.setBrand(entity.getBrand());
        dto.setPointsPrice(entity.getPointsPrice());
        dto.setMarketPrice(entity.getMarketPrice());
        dto.setStock(entity.getStock());
        dto.setSoldCount(entity.getSoldCount());
        dto.setStatus(entity.getStatus());
        dto.setDescription(entity.getDescription());
        dto.setImageUrl(entity.getImageUrl());
        dto.setSubtitle(entity.getSubtitle());
        dto.setDeliveryMethod(entity.getDeliveryMethod());
        dto.setServiceGuarantee(entity.getServiceGuarantee());
        dto.setPromotion(entity.getPromotion());
        dto.setColors(entity.getColors());
        dto.setSpecs(entity.getSpecs());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
