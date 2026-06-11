package com.awsome.shop.product.facade.http.controller;

import com.awsome.shop.product.application.api.service.product.ProductApplicationService;
import com.awsome.shop.product.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 商品图片上传 Controller
 *
 * <p>multipart 上传，存储于本地文件卷并回写商品 imageUrl（FR-PR6）。</p>
 */
@Tag(name = "ProductImage", description = "商品图片上传")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductApplicationService productApplicationService;

    @Operation(summary = "上传商品图片")
    @PostMapping("/public/product/upload-image")
    public Result<String> uploadImage(@RequestParam("productId") Long productId,
                                      @RequestParam("file") MultipartFile file) throws IOException {
        String url = productApplicationService.uploadImage(
                productId, file.getBytes(), file.getOriginalFilename(), file.getContentType());
        return Result.success(url);
    }
}
