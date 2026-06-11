# Implementation Plan

> Feature: product-stock-reservation-api（已扩展为 Product Service 模块级）
> 关联文档：requirements.md（Requirement 1–16）、design.md

---

## 实施状态（Implementation Status）

> 最近更新：阶段 A（P0）+ 阶段 B/C（模块补全）已落地并通过 `mvn clean install -DskipTests` 验证。

- ✅ **阶段 A（P0）已完成**：任务 1.1、2.1、2.2、3.1–3.4、4.1–4.2、5.1–5.6、
  6.1–6.2、7.1–7.2、8.1–8.6、9.1–9.2、10.x、11.1–11.2 的**生产代码**均已实现。
- ✅ **阶段 B/C（模块补全）已完成**：任务 17.x（二级分类 CRUD）、18.x（商品
  getById/update/changeStatus/delete + 搜索 + 售罄判定）、19.x（图片上传）均已实现。
- ⚠️ **迁移版本号修正**：磁盘上 `V3` 已被既有 `V3__create_category_table.sql` 占用，
  因此本特性的迁移实际落地为 **`V4__add_product_type_to_product.sql`** 与
  **`V5__create_stock_reservation_table.sql`**（原计划写作 V3/V4）。
- ⚠️ **模块补全落地决策（与 design 模块补全章节的差异）**：
  - 二级分类**复用既有 `category` 表（V3）** 与既有 CategoryEntity（含 icon/sortOrder/
    status/description），未新建 `V5__create_category_and_link_product.sql`，也未为
    `product` 增加 `category_id` 列。
  - 商品按既有 `category`（名称字符串）模型组织；`categoryId` 过滤通过“按 ID 解析
    分类名称后按名称过滤”实现；分类删除占用校验按**分类名称**统计关联商品。
  - 公开列表 `/list` 仅返回 `status=1`（已上架）商品（Req 13.5）。
  - 图片存储端口 `ImageStoragePort` 置于 `repository-api`，本地卷适配器
    `LocalImageStoragePort` 与静态资源映射 `WebConfig` 置于 `bootstrap`（无独立 infra
    存储模块）。
- ⚠️ **过滤器注册方式调整**：`InternalAuthFilter` 通过 `@Component` + `@Order`
  由 Spring Boot 自动注册（`shouldNotFilter` 限定 `/api/v1/private/**`），未使用
  `FilterRegistrationBean`（其位于 `spring-boot`，非 `interface-http` 依赖）。任务
  10.2 因此不再需要。
- ⚠️ **构建环境**：本地仅安装 JDK 25/26，已将 Lombok 升级至 `1.18.42`（仍兼容
  Java 21）以支持在 JDK 25 上构建。
- 🧹 **脚手架清理**：已删除全部 `Test*` 示例代码与 `V1__create_test_table.sql`。
- ✅ **构建与测试（部分）**：新增并通过 **28 个领域层单元测试**（Mockito）：
  `StockDomainServiceImplTest`(15)、`CategoryDomainServiceImplTest`(7)、
  `ProductDomainServiceImplTest`(6)，覆盖预占状态机/幂等/库存不足、分类层级与占用
  校验、商品 CRUD 不存在分支与图片回写。
- ✅ **集成/并发/E2E 测试已编写并通过**（bootstrap 模块，Testcontainers + 真实 MySQL 8.4）：
  `StockReservationIntegrationTest`(4)、`ConcurrentReserveIntegrationTest`(1，悲观锁防超兑)、
  `InternalAuthFilterE2ETest`(4)，共 9 个用例本地全部通过。标注
  `@Testcontainers(disabledWithoutDocker = true)` + 单例容器模式，**无 Docker 时自动跳过**，
  整体 `mvn clean install` 仍为 BUILD SUCCESS。
  > 本机为 colima 且 docker-java 默认 API 版本过低，验证通过的运行命令为：
  > `export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock`
  > `export TESTCONTAINERS_RYUK_DISABLED=true`
  > `mvn test -pl bootstrap -DforkCount=0 -Dapi.version=1.43`
- ⚠️ **构建环境**：本地仅安装 JDK 25/26，已将 Lombok 升级至 `1.18.42`、JaCoCo 升级至
  `0.8.13`（均兼容 Java 21）以支持在 JDK 25 上构建与测试。
- ⏳ **仍未覆盖**：Bean Validation 单测（12.2）、Repository 层 IT（13.1–13.3）、
  库存/E2E 其余用例（15.2–15.3）、模块补全测试（20.x）。

---

## Overview

本实施计划基于 design.md 落地全部 10 项需求。任务按依赖自底向上组织：
common → bootstrap migration → domain-model → repository-api → mysql-impl → domain-api → domain-impl → application-api → application-impl → interface-http → bootstrap config → tests → finalization。

每个父任务完成后，整个仓库 `mvn -DskipTests install` 应仍可成功构建；每个子任务对应明确的代码产物与需求条目，便于增量提交与回归。

---

## Tasks

### 1. Common 层：错误码与基础类型

- [x] 1.1 新增 `StockErrorCode` 枚举
  - 路径：`common/src/main/java/com/awsome/shop/product/common/enums/StockErrorCode.java`
  - 包含 8 个错误码：`PRODUCT_NOT_FOUND`、`RESERVATION_NOT_FOUND`、`STOCK_INSUFFICIENT`、`RESERVATION_CONFLICT`、`RESERVATION_ALREADY_CONFIRMED`、`RESERVATION_ALREADY_RELEASED`、`INVALID_PRODUCT_TYPE`、`INTERNAL_UNAUTHORIZED`
  - 实现 `ErrorCode` 接口，code 前缀严格遵循 `NOT_FOUND_` / `BIZ_` / `PARAM_` / `AUTH_` 体系
  - _Requirements: 1.2, 1.3, 2.3, 3.3, 3.4, 4.3, 5.4, 5.5, 6.4, 6.5, 9.3, 9.4, 10.3_

### 2. Bootstrap 层：Flyway 数据库迁移

- [x] 2.1 新增 Flyway 脚本 `V4__add_product_type_to_product.sql`
  - 路径：`bootstrap/src/main/resources/db/migration/V4__add_product_type_to_product.sql`
  - 在 `product` 表 `category` 列之后新增 `product_type VARCHAR(16) NOT NULL DEFAULT 'PHYSICAL'`
  - 添加 `CHECK (product_type IN ('PHYSICAL','VIRTUAL'))` 约束
  - 显式回填存量数据（容错语句）
  - _Requirements: 1.1, 1.6, 1.7_

- [x] 2.2 新增 Flyway 脚本 `V5__create_stock_reservation_table.sql`
  - 路径：`bootstrap/src/main/resources/db/migration/V5__create_stock_reservation_table.sql`
  - 表字段：`id VARCHAR(36) PK`、`order_ref VARCHAR(64) NOT NULL`、`product_id BIGINT NOT NULL`、`quantity INT NOT NULL`、`status VARCHAR(16) NOT NULL`，加上标准审计字段（`created_at`/`updated_at`/`created_by`/`updated_by`/`deleted`/`version`）
  - 唯一索引 `uk_order_ref_product_id (order_ref, product_id)`
  - 普通索引 `idx_product_status (product_id, status)`
  - CHECK 约束：`status ∈ {RESERVED, RELEASED, CONFIRMED}`、`quantity > 0`
  - InnoDB / utf8mb4 / utf8mb4_unicode_ci
  - _Requirements: 4.4, 8.5_

- [x] 2.3 启动验证迁移
  - 在 `bootstrap` 模块运行 `mvn flyway:migrate -pl bootstrap`（或启动 `Application` 触发自动迁移）
  - 通过 MySQL 客户端确认 `product.product_type` 列存在且取值合法、`stock_reservation` 表创建成功且唯一索引生效
  - _Requirements: 1.6, 4.4_

---

### 3. Domain-model：枚举与实体

- [x] 3.1 新增 `ProductType` 枚举
  - 路径：`domain/domain-model/src/main/java/com/awsome/shop/product/domain/model/product/ProductType.java`
  - 枚举值：`PHYSICAL`、`VIRTUAL`
  - 纯 POJO，不依赖 Spring 注解
  - _Requirements: 1.1_

- [x] 3.2 新增 `ReservationStatus` 枚举
  - 路径：`domain/domain-model/src/main/java/com/awsome/shop/product/domain/model/stock/ReservationStatus.java`
  - 枚举值：`RESERVED`、`RELEASED`、`CONFIRMED`
  - 提供 `isTerminal()` 方法（RELEASED / CONFIRMED 返回 true）
  - _Requirements: 8.1, 8.2, 8.4_

- [x] 3.3 修改 `ProductEntity` 增加 `productType` 字段
  - 路径：`domain/domain-model/src/main/java/com/awsome/shop/product/domain/model/product/ProductEntity.java`
  - 新增字段：`private ProductType productType;`
  - 保留既有 `@Data` 与其他字段
  - _Requirements: 1.1, 1.4, 1.5_

- [x] 3.4 新增 `StockReservationEntity` 领域实体
  - 路径：`domain/domain-model/src/main/java/com/awsome/shop/product/domain/model/stock/StockReservationEntity.java`
  - 字段：`String id`、`String orderRef`、`Long productId`、`Integer quantity`、`ReservationStatus status`、审计字段
  - 使用 `@Data`，纯 POJO
  - _Requirements: 3.2, 4.1, 5.2, 6.2, 8.1_

### 4. Repository-api：仓储端口

- [x] 4.1 扩展 `ProductRepository` 接口
  - 路径：`domain/repository-api/src/main/java/com/awsome/shop/product/repository/product/ProductRepository.java`
  - 新增方法：
    - `ProductEntity lockById(Long id)` — FOR UPDATE 行级锁
    - `void deductStockAndIncrSold(Long productId, int quantity)` — confirm 扣减 + 售出累计
    - `void adjustStock(Long productId, int newQty)` — 管理员绝对值替换
  - _Requirements: 6.2, 7.1, 9.2_

- [x] 4.2 新增 `StockReservationRepository` 接口
  - 路径：`domain/repository-api/src/main/java/com/awsome/shop/product/repository/stock/StockReservationRepository.java`
  - 方法：
    - `StockReservationEntity findById(String reservationId)`
    - `StockReservationEntity findByOrderRefAndProductId(String orderRef, Long productId)`
    - `int sumReservedQuantity(Long productId)`
    - `void save(StockReservationEntity entity)`
    - `void updateStatus(String reservationId, ReservationStatus newStatus)`
  - _Requirements: 2.4, 3.2, 4.1, 5.2, 6.2_

---

### 5. Infrastructure mysql-impl：持久化适配器

- [x] 5.1 新增 `StockReservationPO` 持久化对象
  - 路径：`infrastructure/repository/mysql-impl/src/main/java/com/awsome/shop/product/repository/mysql/po/stock/StockReservationPO.java`
  - `@TableName("stock_reservation")`、`@TableId(type = IdType.INPUT)`（手动赋值 UUID）
  - 标准审计字段使用 `@TableField(fill = ...)`，`@TableLogic` + `@Version`
  - _Requirements: 4.4, 8.5_

- [x] 5.2 修改 `ProductPO` 增加 `productType` 字段
  - 路径：`infrastructure/repository/mysql-impl/src/main/java/com/awsome/shop/product/repository/mysql/po/product/ProductPO.java`
  - 新增字段：`private String productType;`
  - 保留 `autoResultMap = true`
  - _Requirements: 1.1, 1.4, 1.5_

- [x] 5.3 新增 `StockReservationMapper` 与 XML
  - 接口路径：`infrastructure/repository/mysql-impl/src/main/java/com/awsome/shop/product/repository/mysql/mapper/stock/StockReservationMapper.java`
  - XML 路径：`infrastructure/repository/mysql-impl/src/main/resources/mapper/stock/StockReservationMapper.xml`
  - 接口继承 `BaseMapper<StockReservationPO>`
  - XML 提供：`selectByOrderRefAndProductId`、`sumReservedQuantity`、`updateStatusById`
  - 禁止 Wrapper / 注解 SQL
  - _Requirements: 2.4, 4.1, 4.4, 5.2, 6.2_

- [x] 5.4 修改 `ProductMapper` 与 XML
  - 接口：`infrastructure/repository/mysql-impl/src/main/java/com/awsome/shop/product/repository/mysql/mapper/product/ProductMapper.java`
  - XML：`infrastructure/repository/mysql-impl/src/main/resources/mapper/product/ProductMapper.xml`
  - 新增 XML 方法：`selectByIdForUpdate`（含 `FOR UPDATE`）、`deductStockAndIncrSold`、`adjustStock`
  - 校验现有 `BaseResultMap` 已包含 `productType` 列映射
  - _Requirements: 6.2, 7.1, 9.2_

- [x] 5.5 新增 `StockReservationRepositoryImpl`
  - 路径：`infrastructure/repository/mysql-impl/src/main/java/com/awsome/shop/product/repository/mysql/impl/stock/StockReservationRepositoryImpl.java`
  - `@Repository` + `@RequiredArgsConstructor`，依赖 `StockReservationMapper`
  - 实现全部 5 个端口方法；`save` 在 try-catch 中捕获 `DuplicateKeyException`，回查已有记录或转换为 `BusinessException(RESERVATION_CONFLICT)`
  - 私有 `toEntity(po)` / `toPO(entity)`，不使用 MapStruct
  - _Requirements: 2.4, 3.2, 4.1, 4.4, 5.2, 6.2_

- [x] 5.6 修改 `ProductRepositoryImpl` 实现新增方法
  - 路径：`infrastructure/repository/mysql-impl/src/main/java/com/awsome/shop/product/repository/mysql/impl/product/ProductRepositoryImpl.java`
  - 实现 `lockById`、`deductStockAndIncrSold`、`adjustStock`
  - 修改 `toEntity` / `toPO`：双向映射 `productType`（String ↔ ProductType 枚举）
  - _Requirements: 1.4, 1.5, 6.2, 7.1, 9.2_

### 6. Domain-api：领域服务接口

- [x] 6.1 新增 `StockDomainService` 接口
  - 路径：`domain/domain-api/src/main/java/com/awsome/shop/product/domain/service/stock/StockDomainService.java`
  - 4 个方法：`getAvailableStock`、`reserveStock`、`releaseStock`、`confirmDeduct`
  - 使用基本类型与领域实体，**禁止**引用 DTO / Request 对象
  - 完整 Javadoc 描述各方法状态机分支与异常
  - _Requirements: 2.1, 3.1, 5.1, 6.1_

- [x] 6.2 扩展 `ProductDomainService` 接口
  - 在既有 `ProductDomainService` 增加 `void adjustStock(Long productId, int newQty)`
  - _Requirements: 9.2_

---

### 7. Domain-impl：领域服务实现

- [x] 7.1 新增 `StockDomainServiceImpl`
  - 路径：`domain/domain-impl/src/main/java/com/awsome/shop/product/domain/impl/service/stock/StockDomainServiceImpl.java`
  - `@Service` + `@RequiredArgsConstructor`，依赖 `ProductRepository` 与 `StockReservationRepository`（仅端口接口）
  - **`getAvailableStock`**：商品不存在抛 `BusinessException(PRODUCT_NOT_FOUND)`；返回 `product.stock - sumReservedQuantity`
  - **`reserveStock`**：`@Transactional`；幂等查询 → FOR UPDATE → 计算可用 → 写入；UUID 通过 `UUID.randomUUID().toString()` 生成
    - 幂等命中且 quantity 一致 → 返回已有 reservationId
    - 幂等命中但 quantity 冲突 → `BusinessException(RESERVATION_CONFLICT)`
    - 库存不足 → `BusinessException(STOCK_INSUFFICIENT, available, requested)`
  - **`releaseStock`**：`@Transactional`；不存在 → NOT_FOUND；CONFIRMED → `RESERVATION_ALREADY_CONFIRMED`；RELEASED → no-op；RESERVED → updateStatus(RELEASED)
  - **`confirmDeduct`**：`@Transactional`；不存在 → NOT_FOUND；RELEASED → `RESERVATION_ALREADY_RELEASED`；CONFIRMED → no-op；RESERVED → lockById + deductStockAndIncrSold + updateStatus(CONFIRMED)
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 3.6, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 8.1, 8.2, 8.3, 8.4_

- [x] 7.2 扩展 `ProductDomainServiceImpl` 增加 `adjustStock`
  - 不存在商品 → `BusinessException(PRODUCT_NOT_FOUND)`；newQty < 0 由 Application 层校验，但 Domain 仍兜底防御
  - 仅更新 `product.stock`，不接触 `stock_reservation` 表
  - _Requirements: 9.2, 9.4, 9.5_

### 8. Application-api：DTO 与服务接口

- [x] 8.1 新增 Stock 模块 Application DTO
  - 路径：`application/application-api/src/main/java/com/awsome/shop/product/application/api/dto/stock/`
  - `StockQueryResponse`（productId, availableStock, productType）
  - `ReserveStockResponse`（reservationId）
  - 使用 `@Data`
  - _Requirements: 2.2, 3.2_

- [x] 8.2 新增 Stock 模块 Request 对象
  - 路径：`application/application-api/src/main/java/com/awsome/shop/product/application/api/dto/stock/request/`
  - `StockQueryRequest`（productId `@NotNull`）
  - `ReserveStockRequest`（productId `@NotNull`、quantity `@NotNull` `@Min(1)`、orderRef `@NotBlank` `@Size(max=64)`）
  - `ReleaseStockRequest`（reservationId `@NotBlank`）
  - `ConfirmStockRequest`（reservationId `@NotBlank`）
  - 校验消息使用中文
  - _Requirements: 2.1, 3.1, 3.5, 5.1, 6.1_

- [x] 8.3 修改 `ProductDTO` 增加 `productType`
  - 路径：`application/application-api/src/main/java/com/awsome/shop/product/application/api/dto/product/ProductDTO.java`
  - 新增字段：`private String productType;`
  - _Requirements: 1.4, 1.5_

- [x] 8.4 修改 `CreateProductRequest` 增加 `productType` 校验
  - 新增字段：`private String productType;`，注解 `@NotBlank(message="商品类型不能为空")` + `@Pattern(regexp="PHYSICAL|VIRTUAL", message="商品类型必须为 PHYSICAL 或 VIRTUAL")`
  - _Requirements: 1.2, 1.3_

- [x] 8.5 新增 `AdjustStockRequest`
  - 路径：`application/application-api/src/main/java/com/awsome/shop/product/application/api/dto/product/request/AdjustStockRequest.java`
  - 字段：productId `@NotNull`、newQty `@NotNull` `@Min(0, message="库存数量不能为负")`
  - _Requirements: 9.1, 9.3_

- [x] 8.6 新增 `StockApplicationService` 接口
  - 路径：`application/application-api/src/main/java/com/awsome/shop/product/application/api/service/stock/StockApplicationService.java`
  - 4 个方法对应 Domain Service，返回 DTO；参数使用 Request 对象或基本类型
  - _Requirements: 2.1, 3.1, 5.1, 6.1_

---

### 9. Application-impl：服务实现

- [x] 9.1 新增 `StockApplicationServiceImpl`
  - 路径：`application/application-impl/src/main/java/com/awsome/shop/product/application/impl/service/stock/StockApplicationServiceImpl.java`
  - `@Service` + `@RequiredArgsConstructor`，仅依赖 `StockDomainService`（不直接依赖 Repository）
  - 不加 `@Transactional`（事务由 Domain 层控制）
  - 私有方法 `toQueryResponse(ProductEntity, int available)` 完成 Entity → DTO
  - _Requirements: 2.1, 2.2, 3.1, 3.2, 5.1, 6.1_

- [x] 9.2 修改 `ProductApplicationServiceImpl`
  - 路径：`application/application-impl/src/main/java/com/awsome/shop/product/application/impl/service/product/ProductApplicationServiceImpl.java`
  - 修改 `toDTO`：映射 `productType`（ProductType 枚举 → String）
  - 修改 `create`：从 Request 提取 `productType` String → ProductType 枚举，写入 ProductEntity
  - 新增 `adjustStock(AdjustStockRequest req)`：委托 `productDomainService.adjustStock`
  - _Requirements: 1.2, 1.3, 1.4, 9.1, 9.2, 9.3, 9.4_

### 10. Interface-http：HTTP 入口

- [x] 10.1 新增 `InternalAuthFilter`
  - 路径：`interface/interface-http/src/main/java/com/awsome/shop/product/facade/http/filter/InternalAuthFilter.java`
  - 继承 `OncePerRequestFilter`，`@Component`
  - 配置项 `@Value("${awsomeshop.internal-auth.token}")`
  - `shouldNotFilter`：路径不以 `/api/v1/private/` 开头时返回 true
  - `doFilterInternal`：缺失或不匹配 token → 401 + `Result.error(StockErrorCode.INTERNAL_UNAUTHORIZED)`；通过则 `chain.doFilter`
  - 使用 Slf4j 记录拒绝日志：`{path, reason, remoteAddr}`
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

- [x] 10.2 ~~新增 `InternalAuthFilterConfig`~~（改用 `@Component` + `@Order` 自动注册，已由 10.1 覆盖，无需单独的 FilterRegistrationBean）
  - 路径：`interface/interface-http/src/main/java/com/awsome/shop/product/facade/http/config/InternalAuthFilterConfig.java`
  - `@Configuration`，注册 `FilterRegistrationBean<InternalAuthFilter>`，URL pattern `/api/v1/private/*`
  - `setOrder(Ordered.HIGHEST_PRECEDENCE + 10)` 确保在其他过滤器之前
  - _Requirements: 10.1_

- [x] 10.3 新增 `InternalStockController`
  - 路径：`interface/interface-http/src/main/java/com/awsome/shop/product/facade/http/controller/InternalStockController.java`
  - `@RestController` + `@RequestMapping("/api/v1")` + `@RequiredArgsConstructor`，依赖 `StockApplicationService`
  - 4 个 `@PostMapping` 端点：`/private/stock/get`、`/private/stock/reserve`、`/private/stock/release`、`/private/stock/confirm`
  - 全部使用 `@RequestBody @Valid` 接收 Request 对象，返回 `Result<T>`
  - 类与方法添加 Swagger `@Tag` / `@Operation`
  - _Requirements: 2.1, 3.1, 5.1, 6.1_

- [x] 10.4 新增 `AdminProductController`（仅 adjustStock）
  - 路径：`interface/interface-http/src/main/java/com/awsome/shop/product/facade/http/controller/AdminProductController.java`
  - `@PostMapping("/public/product/adjust-stock")`
  - 使用 `@RequestBody @Valid AdjustStockRequest`
  - _Requirements: 9.1_

---

### 11. Bootstrap：配置项

- [x] 11.1 增加内网鉴权 token 配置
  - 文件：`bootstrap/src/main/resources/application.yml`（基础）+ `application-{profile}.yml`（覆盖）
  - 新增配置块：
    ```yaml
    awsomeshop:
      internal-auth:
        token: ${INTERNAL_AUTH_TOKEN:dev-internal-token}
    ```
  - 文档化：在配置文件顶部添加注释说明该 token 必须由部署平台注入，开发环境用默认值
  - _Requirements: 10.2, 10.3_

- [x] 11.2 验证 ComponentScan 覆盖新增包
  - 检查 `Application.java` 上的 `@ComponentScan(basePackages = "com.awsome.shop.product")` 已覆盖所有新增类（默认覆盖，无需修改）
  - 启动 `mvn spring-boot:run -pl bootstrap`，确认无 `NoSuchBeanDefinitionException`
  - _Requirements: 全部_

### 12. 单元测试：Domain Service 与 Application Service

- [x] 12.1 `StockDomainServiceImplTest`（Mockito 单测）
  - 路径：`domain/domain-impl/src/test/java/com/awsome/shop/product/domain/impl/service/stock/StockDomainServiceImplTest.java`
  - 用例（Mock Repository）：
    - `reserveStock_quantityExceedsAvailable_throwsStockInsufficient` _Requirements: 3.3_
    - `reserveStock_sameOrderRefSameQuantity_returnsExisting` _Requirements: 4.1, 4.2_
    - `reserveStock_sameOrderRefDifferentQuantity_throwsConflict` _Requirements: 4.3_
    - `releaseStock_alreadyReleased_isNoop` _Requirements: 5.3_
    - `releaseStock_alreadyConfirmed_throwsBizError` _Requirements: 5.4_
    - `releaseStock_notFound_throwsNotFound` _Requirements: 5.5_
    - `releaseStock_reserved_setsReleased` _Requirements: 5.2_
    - `confirmDeduct_alreadyConfirmed_isNoop` _Requirements: 6.3_
    - `confirmDeduct_alreadyReleased_throwsBizError` _Requirements: 6.4_
    - `confirmDeduct_notFound_throwsNotFound` _Requirements: 6.5_
    - `confirmDeduct_reserved_deductsAndUpdates` _Requirements: 6.2_
    - `getAvailableStock_productNotFound_throwsNotFound` _Requirements: 2.3_

- [ ] 12.2 `ProductApplicationServiceImplTest` 增量
  - 在既有测试类追加：
    - `create_withInvalidProductType_validationFails`（仅断言 Pattern 校验消息） _Requirements: 1.3_
    - `create_withoutProductType_validationFails` _Requirements: 1.2_
    - `adjustStock_negativeQty_validationFails` _Requirements: 9.3_

---

### 13. 集成测试：Repository + 真实 MySQL

- [ ] 13.1 引入 Testcontainers 依赖（如未引入）
  - 在根 `pom.xml` 或 `infrastructure/repository/mysql-impl/pom.xml` 增加 `testcontainers-mysql`、`junit-jupiter`、`spring-boot-testcontainers`
  - 提供 `@TestConfiguration` 启动 MySQL 8.4 容器
  - _Requirements: 全部数据访问类_

- [ ] 13.2 `StockReservationRepositoryImplIT`
  - 路径：`infrastructure/repository/mysql-impl/src/test/java/com/awsome/shop/product/repository/mysql/impl/stock/StockReservationRepositoryImplIT.java`
  - 用例：
    - `save_thenFindById_roundTrip`
    - `findByOrderRefAndProductId_uniqueIndexHit` _Requirements: 4.4_
    - `sumReservedQuantity_excludesReleasedAndConfirmed` _Requirements: 2.4_
    - `save_duplicateOrderRefProductId_throwsDuplicateKey` _Requirements: 4.4_

- [ ] 13.3 `ProductRepositoryImplIT`
  - 用例：
    - `lockById_returnsEntity_andHoldsLock` _Requirements: 7.1_
    - `deductStockAndIncrSold_atomicUpdate` _Requirements: 6.2_
    - `adjustStock_setsAbsoluteValue` _Requirements: 9.2_
    - `flywayV3_addProductTypeColumn_backfillToPhysical` _Requirements: 1.6, 1.7_

- [ ] 13.4 `StockDomainServiceImplIT`（端到端在 Domain 层）
  - 关键用例：
    - `reserveStock_thenConfirmDeduct_stockAndSoldCountUpdated` _Requirements: 6.2_
    - `reserveStock_thenRelease_availableStockRestored` _Requirements: 5.6_

### 14. 并发测试：悲观锁防超兑

- [x] 14.1 `ConcurrentReserveIT`（已实现为 `ConcurrentReserveIntegrationTest`，无 Docker 时自动跳过）
  - 路径：`domain/domain-impl/src/test/java/com/awsome/shop/product/domain/impl/service/stock/ConcurrentReserveIT.java`
  - 复用 13.1 的 Testcontainers 配置（必须真实 MySQL）
  - 用例：`concurrent_NReserve_M_stock_only_M_succeed`
    - 准备：插入 productId 库存=5
    - 执行：20 个线程并发 reserve 1 单位（orderRef 互不相同），使用 `CountDownLatch` 同步起跑
    - 断言：成功数=5、`STOCK_INSUFFICIENT`=15、`product.stock`=5（未扣减）、`sumReservedQuantity`=5
  - _Requirements: 7.3, 7.4_

- [ ] 14.2 `ConcurrentReserveSameOrderRefIT`
  - 用例：`concurrent_sameOrderRef_DuplicateKey_resolvedToSameReservationId`
    - 10 个线程使用相同 orderRef + productId + quantity 并发 reserve
    - 断言：所有线程返回**相同**的 reservationId、库存仅扣 1 次的 RESERVED 数量
  - _Requirements: 4.4_

---

### 15. E2E 测试：HTTP 入口 + 鉴权过滤器

- [x] 15.1 `InternalAuthFilterE2ETest`
  - 路径：`interface/interface-http/src/test/java/com/awsome/shop/product/facade/http/filter/InternalAuthFilterE2ETest.java`
  - 使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
  - 用例：
    - `privateEndpoint_withoutToken_returns401` _Requirements: 10.3_
    - `privateEndpoint_withInvalidToken_returns401` _Requirements: 10.3_
    - `privateEndpoint_withValidToken_returns200` _Requirements: 10.2_
    - `publicEndpoint_unaffectedByFilter`（调用 `/api/v1/public/product/list`，无 token 应正常通过） _Requirements: 10.5_
    - `rejectedRequest_logsStructuredEntry`（使用 `@CaptureLogger` 或 LogbackTestAppender） _Requirements: 10.6_

- [ ] 15.2 `InternalStockControllerE2ETest`
  - 用例（携带正确 token）：
    - `getStock_existing_returnsAvailableAndProductType` _Requirements: 2.2_
    - `reserve_thenConfirm_endToEnd` _Requirements: 3.2, 6.2_
    - `reserve_thenRelease_endToEnd` _Requirements: 3.2, 5.2_
    - `reserve_quantityExceedsAvailable_returnsBizError` _Requirements: 3.3_
    - `release_nonExistent_returnsNotFound` _Requirements: 5.5_

- [ ] 15.3 `AdminProductControllerE2ETest`
  - 用例：
    - `adjustStock_validRequest_updatesStock` _Requirements: 9.2_
    - `adjustStock_negative_returns400` _Requirements: 9.3_
    - `adjustStock_nonExistentProduct_returnsNotFound` _Requirements: 9.4_
    - `adjustStock_doesNotModifyReservations` _Requirements: 9.5_

### 17. 模块补全：二级分类（FR-PR3 / Req 11）

- [x] 17.1 Domain-model + 端口：`CategoryEntity`、`CategoryRepository`（findTree/findById/save/update/deleteById/countChildren/countProducts）
  - _Requirements: 11.1, 11.4_
- [x] 17.2 Flyway `V5__create_category_and_link_product.sql`（category 表 + product.category_id）
  - _Requirements: 11.1_
- [x] 17.3 mysql-impl：`CategoryPO` / `CategoryMapper`(+XML countChildren/countProducts/selectTree) / `CategoryRepositoryImpl`
  - _Requirements: 11.4, 11.6_
- [x] 17.4 Domain：`CategoryDomainService` + Impl（创建二级校验、删除占用校验）
  - _Requirements: 11.2, 11.3, 11.6, 11.7_
- [x] 17.5 Application：`CategoryApplicationService` + Impl + `CategoryDTO`/Request 对象
  - _Requirements: 11.4, 11.5_
- [x] 17.6 Interface：`CategoryController`（tree/create/update/delete）
  - _Requirements: 11.1, 11.5, 11.7_
- [x] 17.7 common：`CategoryErrorCode`（CATEGORY_NOT_FOUND/LEVEL_EXCEEDED/IN_USE）
  - _Requirements: 11.3, 11.6_

### 18. 模块补全：商品 CRUD + 搜索 + 售罄（Req 12–14, 16）

- [x] 18.1 Domain/Repository 扩展：`ProductDomainService.{update, changeStatus, delete, page(keyword,categoryId)}`；`ProductEntity/ProductPO` 增加 `categoryId`
  - _Requirements: 12.2, 13.1, 13.4, 13.6, 14.1_
- [x] 18.2 mysql-impl：`ProductMapper` 增强 selectPage（keyword + categoryId）；update/changeStatus/软删
  - _Requirements: 13.5, 14.2, 14.3, 14.4_
- [x] 18.3 Application：扩展 `ProductApplicationService.{getById, update, changeStatus, delete}`；toDTO 增加 `categoryId`、派生 `soldOut`
  - _Requirements: 12.2, 13.1, 16.1, 16.2_
- [x] 18.4 Application Request：`GetProductRequest`/`UpdateProductRequest`(productType @Pattern)/`ChangeStatusRequest`/`DeleteProductRequest`；`ListProductRequest` 增加 keyword/categoryId
  - _Requirements: 12.1, 13.3, 14.1_
- [x] 18.5 Interface：`ProductController` 增加 get/update/change-status/delete 端点
  - _Requirements: 12.1, 13.1, 13.4, 13.6_

### 19. 模块补全：图片上传（FR-PR6 / Req 15）

- [x] 19.1 端口 + 适配器：`ImageStorageComponent`（domain 端口 + infra 本地卷实现，校验 content-type，生成 URL）
  - _Requirements: 15.1, 15.3_
- [x] 19.2 Application：`ProductApplicationService.uploadImage`（回写 imageUrl）
  - _Requirements: 15.2, 15.4_
- [x] 19.3 Interface：`ProductImageController` multipart 端点 `/api/v1/public/product/upload-image`
  - _Requirements: 15.1, 15.2_
- [x] 19.4 common：`ProductErrorCode.INVALID_IMAGE_TYPE`
  - _Requirements: 15.3_
- [x] 19.5 配置：本地卷路径 `awsomeshop.image.storage-path` + 静态资源映射
  - _Requirements: 15.1_

### 20. 模块补全测试（Req 11–16）

- [ ] 20.1 Category 单测 + 集成测（层级校验/占用校验/树）
  - _Requirements: 11.2, 11.3, 11.4, 11.6, 11.7_
- [ ] 20.2 Product CRUD + 搜索 + 售罄 测试
  - _Requirements: 12.2, 12.3, 13.3, 13.5, 13.6, 14.4, 14.5, 16.1_
- [ ] 20.3 图片上传 E2E（valid / 非图片 400）
  - _Requirements: 15.2, 15.3_

### 21. 收尾：构建与回归

- [ ] 21.1 全量编译
  - 执行 `mvn clean compile`
  - 确认 0 编译错误
  - _Requirements: 全部_

- [ ] 21.2 全量单元测试
  - 执行 `mvn test`
  - 全部 12.x、13.x、14.x、15.x、20.x 用例通过
  - _Requirements: 全部_

- [ ] 21.3 启动验证
  - 启动 MySQL Docker 容器（参见 `tech.md`）
  - 执行 `mvn spring-boot:run -pl bootstrap`
  - 访问 Swagger UI `http://localhost:8081/swagger-ui.html`，确认 4 个 `InternalStockController` 端点 + `AdminProductController.adjust-stock` 显示
  - 使用 curl 携带 `X-Internal-Token` 调用 `/api/v1/private/stock/get`，确认返回 `Result.success`
  - 使用 curl 不携带 token 调用相同端点，确认返回 401
  - _Requirements: 10.2, 10.3, 全部端点_

- [ ] 21.4 回归既有功能
  - 调用既有 `/api/v1/public/product/list`、`/api/v1/public/product/create`，确认：
    - list 响应包含 `productType` 字段（Req 1.4）
    - create 不传 `productType` 时返回 400（Req 1.2）
    - create 传 `productType=INVALID` 返回 400（Req 1.3）
  - _Requirements: 1.2, 1.3, 1.4, 10.5_

---

## Task Dependency Graph

```json
{
  "waves": [
    {
      "id": "wave-1",
      "name": "基础类型与数据库迁移（无依赖，可并行）",
      "tasks": ["1.1", "2.1", "2.2", "3.1", "3.2", "3.3", "3.4"],
      "dependsOn": []
    },
    {
      "id": "wave-2",
      "name": "迁移落库验证 + 仓储端口",
      "tasks": ["2.3", "4.1", "4.2"],
      "dependsOn": ["wave-1"]
    },
    {
      "id": "wave-3",
      "name": "持久化适配器（PO / Mapper / Impl）",
      "tasks": ["5.1", "5.2", "5.3", "5.4", "5.5", "5.6"],
      "dependsOn": ["wave-2"]
    },
    {
      "id": "wave-4",
      "name": "领域服务接口",
      "tasks": ["6.1", "6.2"],
      "dependsOn": ["wave-3"]
    },
    {
      "id": "wave-5",
      "name": "领域服务实现",
      "tasks": ["7.1", "7.2"],
      "dependsOn": ["wave-4"]
    },
    {
      "id": "wave-6",
      "name": "Application DTO / Request / 服务接口",
      "tasks": ["8.1", "8.2", "8.3", "8.4", "8.5", "8.6"],
      "dependsOn": ["wave-5"]
    },
    {
      "id": "wave-7",
      "name": "Application 服务实现",
      "tasks": ["9.1", "9.2"],
      "dependsOn": ["wave-6"]
    },
    {
      "id": "wave-8",
      "name": "Interface HTTP（过滤器、配置、控制器）",
      "tasks": ["10.1", "10.2", "10.3", "10.4"],
      "dependsOn": ["wave-7"]
    },
    {
      "id": "wave-9",
      "name": "Bootstrap 配置",
      "tasks": ["11.1", "11.2"],
      "dependsOn": ["wave-8"]
    },
    {
      "id": "wave-10",
      "name": "单元测试",
      "tasks": ["12.1", "12.2"],
      "dependsOn": ["wave-7"]
    },
    {
      "id": "wave-11",
      "name": "集成测试与并发测试（依赖 Testcontainers）",
      "tasks": ["13.1", "13.2", "13.3", "13.4", "14.1", "14.2"],
      "dependsOn": ["wave-7"]
    },
    {
      "id": "wave-12",
      "name": "E2E 测试",
      "tasks": ["15.1", "15.2", "15.3"],
      "dependsOn": ["wave-9"]
    },
    {
      "id": "wave-13",
      "name": "模块补全：分类 + 商品CRUD/搜索/售罄 + 图片上传",
      "tasks": ["17.1","17.2","17.3","17.4","17.5","17.6","17.7","18.1","18.2","18.3","18.4","18.5","19.1","19.2","19.3","19.4","19.5"],
      "dependsOn": ["wave-7"]
    },
    {
      "id": "wave-14",
      "name": "模块补全测试",
      "tasks": ["20.1","20.2","20.3"],
      "dependsOn": ["wave-13"]
    },
    {
      "id": "wave-15",
      "name": "构建与回归收尾",
      "tasks": ["21.1", "21.2", "21.3", "21.4"],
      "dependsOn": ["wave-10", "wave-11", "wave-12", "wave-14"]
    }
  ]
}
```

### 文本可视化

```
wave-1  [1.1, 2.1, 2.2, 3.1-3.4]      基础类型 + DDL + 枚举/实体（可并行）
   │
   ▼
wave-2  [2.3, 4.1, 4.2]                迁移验证 + 仓储端口
   │
   ▼
wave-3  [5.1-5.6]                      持久化适配器
   │
   ▼
wave-4  [6.1, 6.2]                     领域服务接口
   │
   ▼
wave-5  [7.1, 7.2]                     领域服务实现
   │
   ▼
wave-6  [8.1-8.6]                      Application DTO / 接口
   │
   ▼
wave-7  [9.1, 9.2]                     Application 实现
   │            │                              │
   ▼            ▼                              ▼
wave-8       wave-10                       wave-11
[10.1-10.4]  [12.1, 12.2]                  [13.1-13.4, 14.1, 14.2]
Interface    单元测试                       集成 + 并发测试
   │
   ▼
wave-9  [11.1, 11.2]                   Bootstrap 配置
   │
   ▼
wave-12 [15.1-15.3]                    E2E 测试

wave-13 [17.x,18.x,19.x]               模块补全（分类/CRUD/搜索/售罄/图片）← wave-7
   │
   ▼
wave-14 [20.1-20.3]                    模块补全测试
   │
   └──────┬───────────┬───────────┬──────────┐
          ▼           ▼           ▼          ▼
                  wave-15  [21.1-21.4]   构建与回归收尾
              （依赖 wave-10/11/12/14）
```


---

## Notes

### 编码约束

- **不新增 Maven 模块**：所有新增类按 `<layer>.stock.<class>` 包路径放入既有模块，遵循 `structure.md` 与 `layer-*.md` 约定。
- **DDD 方向严格**：Domain 不依赖 Application/Interface；Application 不直接依赖 Repository；Interface 仅依赖 Application。
- **SQL 规范**（`layer-infrastructure.md`）：除 `selectById` / `insert` / `updateById` / `deleteById` 外，全部 SQL 写在 Mapper XML；禁止 `@Select` 注解 / Wrapper / LambdaQuery。
- **URL 命名**（`layer-interface.md`）：内部接口路径为 `/api/v1/private/stock/<action>`，与 `requirements.md` 中表述的"`/internal/stock/**`"语义等价（已在 design.md Overview 标注）。
- **悲观锁与乐观锁隔离**：`SELECT ... FOR UPDATE` 走 XML，不经 Wrapper，不会触发 MyBatis-Plus 的 `OptimisticLockerInnerInterceptor`。
- **审计字段**：所有新表/PO 必须包含 `created_at` / `updated_at` / `created_by` / `updated_by` / `deleted` / `version`，由 MyBatis-Plus 自动填充。
- **测试基础设施**：集成测试与并发测试必须使用 Testcontainers + 真实 MySQL 8.4，不可用 H2（`SELECT FOR UPDATE` 与 `DuplicateKeyException` 行为差异）。

### 提交策略

- 每个父任务（1–16）形成一次原子提交，commit message 形如 `feat(product): [task-N] <描述>`。
- 每次提交后运行 `mvn -DskipTests install`，仓库必须保持可构建。
- 任务 13、14、15 (集成测试) 完成后再统一提交，避免中间状态测试失败。

### 风险点

- **任务 5.5（StockReservationRepositoryImpl 的 DuplicateKey 处理）**：是 reserve 幂等性正确性的关键，必须由 14.2 并发测试覆盖。
- **任务 7.1（StockDomainServiceImpl）**：含状态机全部分支与悲观锁，单测（12.1）必须覆盖每个分支。
- **任务 10.1（InternalAuthFilter）**：必须确保不影响既有 `/api/v1/public/**` 路径，由 15.1 `publicEndpoint_unaffectedByFilter` 用例兜底。
- **任务 11.1（token 配置）**：生产环境必须由部署平台注入，**不得**将 `dev-internal-token` 默认值带入生产 profile；建议在 PR 模板提醒中加以确认。

### 后续 P1/P2 范围（不在本 spec 内）

requirements.md 已明确以下项目留待后续 spec：Category 二级分类完整模块、Product 完整公开 CRUD（getById/update/changeStatus/delete）、图片上传组件、`/admin/**` / `/products/**` 路径分离、Test* 脚手架清理、ProductEntity 中可疑字段的清理。本计划完成后将形成新的 baseline 供这些 P1/P2 工作迭代。
