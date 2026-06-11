package com.awsome.shop.product.facade.http.controller;

import com.awsome.shop.product.application.api.dto.category.CategoryDTO;
import com.awsome.shop.product.application.api.dto.category.request.CreateCategoryRequest;
import com.awsome.shop.product.application.api.dto.category.request.DeleteCategoryRequest;
import com.awsome.shop.product.application.api.dto.category.request.ListCategoryRequest;
import com.awsome.shop.product.application.api.dto.category.request.UpdateCategoryRequest;
import com.awsome.shop.product.application.api.service.category.CategoryApplicationService;
import com.awsome.shop.product.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 类目管理 Controller
 */
@Tag(name = "Category", description = "类目管理")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryApplicationService categoryApplicationService;

    @Operation(summary = "查询类目列表（树形结构）")
    @PostMapping("/public/category/list")
    public Result<List<CategoryDTO>> list(@RequestBody @Valid ListCategoryRequest request) {
        return Result.success(categoryApplicationService.list(request));
    }

    @Operation(summary = "创建类目")
    @PostMapping("/public/category/create")
    public Result<CategoryDTO> create(@RequestBody @Valid CreateCategoryRequest request) {
        return Result.success(categoryApplicationService.create(request));
    }

    @Operation(summary = "更新类目")
    @PostMapping("/public/category/update")
    public Result<CategoryDTO> update(@RequestBody @Valid UpdateCategoryRequest request) {
        return Result.success(categoryApplicationService.update(request));
    }

    @Operation(summary = "删除类目")
    @PostMapping("/public/category/delete")
    public Result<Void> delete(@RequestBody @Valid DeleteCategoryRequest request) {
        categoryApplicationService.delete(request);
        return Result.success();
    }
}
