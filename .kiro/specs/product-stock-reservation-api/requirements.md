# Requirements Document

> Feature: product-stock-reservation-api

## Introduction

本特性为 `awsome-shop-product-service` 补齐三项 P0 缺失能力，使其能够支撑
`awsome-shop-order-service` 兑换 Saga 编排（参见 awsome-shop-plan
`services.md` 第 2.2/2.3 节、`component-dependency.md` 第 4.1 节）。

补齐范围严格限定为以下三项：

1. **商品类型字段（Product_Type）**：在 Product 实体、PO、数据表中新增
   `product_type` 字段（取值 `PHYSICAL` / `VIRTUAL`），覆盖
   `requirements.md` FR-PR2 / AS-1 / AS-2 / AS-3 与 `stories.md` US-07、
   US-19。下游 Order Service 据此选择"预占→发货→正式扣减"或"即时扣减"
   流程。
2. **库存预占内部接口（Stock_Service + Stock_Reservation）**：暴露
   `/internal/stock/**` 内部 REST 接口，提供库存查询、悲观锁预占、释放、
   正式扣减能力，并新增 `stock_reservation` 表持久化预占凭据；管理员调整
   库存接口（FR-PR5）一并纳入本特性。覆盖 FR-PR7、BR-6、BR-8、NFR-4、
   US-08 / US-14 / US-26 / US-28。
3. **内网鉴权过滤器（Internal_Auth_Filter）**：保护所有 `/internal/**`
   路径，仅放行来自 API 网关或内网的可信调用，外部直连请求一律拒绝。
   覆盖 `components.md` 第 0 节、`services.md` 第 4 节、`component-methods.md`
   第 7 节。

明确不在本特性范围内的项目：`/admin/**` / `/products/**` 路径分离、Test* 脚手架清理、ProductEntity 中可疑字段（`sku`/`brand`/`marketPrice`/`subtitle` 等）的清理；这些归 P2，留待后续 spec。

> **范围扩展（2026-06-10）**：依据用户决策（scope-decision-questions.md，Q1=A），
> 本 spec 扩展为 **Product Service 模块级**，在原 P0 三项之外补齐 plan Product Service
> 模块（components.md §2 + FR-PR1~PR7）的剩余能力，使本模块设计与 plan 完全一致：
> 二级分类管理（FR-PR3）、商品详情/编辑/上下架/删除（FR-PR1/PR4）、商品搜索增强
> （FR-PR4）、商品图片上传（FR-PR6）、售罄判定（FR-PR5）。对应 Requirement 11–16。

---

## Glossary

- **Product_Service**：本仓库 `awsome-shop-product-service` 整体微服务，
  包含 Interface / Application / Domain / Infrastructure / Bootstrap 各
  Maven 模块。
- **Order_Service**：调用方微服务 `awsome-shop-order-service`，作为兑换
  Saga 的协调者通过内部 REST 调用 Product_Service。
- **Stock_Service**：Domain 层的库存领域服务（`domain-api` 定义接口、
  `domain-impl` 实现），负责库存查询、预占、释放、正式扣减、调整等业务。
- **Internal_Stock_Controller**：Interface 层的 HTTP 控制器，暴露
  `/internal/stock/**` 路径，受 Internal_Auth_Filter 保护。
- **Internal_Auth_Filter**：Spring Security / Servlet Filter，校验请求是
  否来自内网可信来源；保护所有 `/internal/**` 路径。
- **Product_Type**：商品类型枚举值，仅可为 `PHYSICAL`（实物商品，需"预
  占→发货→正式扣减"流程）或 `VIRTUAL`（虚拟商品/卡券，下单时立即扣减）。
- **Stock_Reservation**：库存预占领域实体，持久化为 `stock_reservation`
  表的一行；含 `reservation_id`、`order_ref`、`product_id`、`quantity`、
  `status`、时间审计字段等。
- **Reservation_Id**：Stock_Reservation 的全局唯一标识，由 Product_Service
  在预占成功时生成（推荐 UUID 字符串），返回给 Order_Service 用于后续
  release / confirm。
- **Order_Ref**：调用方提供的业务幂等键，由 Order_Service 在每次首次
  reserve 调用时携带，唯一对应一次兑换订单的某一商品行。Product_Service
  以 `order_ref` 作为 reserve 操作的幂等去重依据。
- **Available_Stock**：商品当前可被新预占消耗的库存数量，等于商品总库存
  减去当前所有处于 `RESERVED` 状态的预占数量。
- **Pessimistic_Lock**：MySQL 行级排他锁，由 `SELECT ... FOR UPDATE`
  语句在数据库事务中获取，用于在并发预占时串行化对同一商品库存的检查与
  扣减。
- **Reservation_Status**：Stock_Reservation 的状态，取值为 `RESERVED`、
  `RELEASED`、`CONFIRMED` 之一。`RESERVED` 为初始状态；`RELEASED` 与
  `CONFIRMED` 均为终态，不可再迁移到其他状态。
- **Internal_Trust_Credential**：表征请求来自内网可信来源的凭证；具体
  形式（共享密钥请求头、来源 IP/容器网段判定、mTLS 等）由 Design 阶段
  决定，本需求文档不限定实现。
- **Idempotent**：对同一幂等键（reserve 用 Order_Ref，release / confirm
  用 Reservation_Id）多次调用返回与首次调用相同语义结果，且不产生重复
  的副作用（库存不被多次扣减、状态不被回退）。
- **Common_Module**：仓库内 `common` 模块，提供 `Result`、`PageResult`、
  `BusinessException`、`ParameterException`、`ErrorCode` 等基础类。

---

## Requirements

### Requirement 1: 商品类型字段（Product_Type）持久化与暴露

**User Story:** As an admin of Product_Service, I want every product to carry
a Product_Type value, so that Order_Service can choose between the
reserve→ship→confirm flow for physical goods and the immediate-deduct flow
for virtual goods.

**关联设计依据：** FR-PR2、AS-1、AS-2、AS-3、US-07、US-19、`components.md`
第 2 节"关键点"。

#### Acceptance Criteria

1. THE Product_Service SHALL persist a Product_Type attribute on every
   Product record, with valid values restricted to `PHYSICAL` or `VIRTUAL`.
2. WHEN an admin submits a create-product request without a Product_Type
   value, THE Product_Service SHALL reject the request with a
   ParameterException whose error code belongs to the Common_Module
   parameter-error category.
3. WHEN an admin submits a create-product or update-product request with a
   Product_Type value outside the set `{PHYSICAL, VIRTUAL}`, THE
   Product_Service SHALL reject the request with a ParameterException whose
   error code belongs to the Common_Module parameter-error category.
4. WHEN any client retrieves a product through the existing public product
   list interface (`POST /api/v1/public/product/list`), THE Product_Service
   SHALL include the Product_Type field in each ProductDTO of the response.
5. WHEN Order_Service retrieves stock or product information through any
   internal interface defined in this specification, THE Product_Service
   SHALL include the Product_Type field in the response payload.
6. THE Product_Service database schema SHALL ensure that the
   `product.product_type` column is non-null after the Flyway migration
   introduced by this feature has been applied.
7. WHERE existing product rows already exist in the database before the
   migration is applied, THE Flyway migration SHALL backfill their
   `product_type` to `PHYSICAL` so that constraint AC-1.6 holds without
   data loss.

---

### Requirement 2: 可用库存查询内部接口

**User Story:** As Order_Service, I want to query the current Available_Stock
of a given product through an internal endpoint, so that I can validate
stock sufficiency before initiating a redemption Saga and surface accurate
"sold-out" feedback to employees.

**关联设计依据：** FR-PR7、`component-methods.md` Stock_Service
`getAvailableStock`、`services.md` 第 1 节"商品服务核心职责"。

#### Acceptance Criteria

1. THE Internal_Stock_Controller SHALL expose an HTTP endpoint
   `GET /internal/stock/{productId}` that returns the current Available_Stock
   of the specified product.
2. WHEN Order_Service invokes `GET /internal/stock/{productId}` with a
   `productId` corresponding to an existing, non-deleted product,
   THE Product_Service SHALL respond within 500 ms with a Common_Module
   `Result` payload containing the product's Available_Stock and
   Product_Type.
3. IF Order_Service invokes `GET /internal/stock/{productId}` with a
   `productId` that does not correspond to any non-deleted product,
   THEN THE Product_Service SHALL respond with a BusinessException whose
   error code belongs to the Common_Module not-found category.
4. THE Available_Stock value returned by this endpoint SHALL equal the
   product's total stock minus the sum of `quantity` of all Stock_Reservation
   rows for that product whose Reservation_Status equals `RESERVED`.

---

### Requirement 3: 库存预占接口（reserve）- 成功与失败路径

**User Story:** As Order_Service, I want to reserve a quantity of a product's
stock against a specific Order_Ref through an internal endpoint, so that
during the redemption Saga the stock is locked for the order before
confirmation or release.

**关联设计依据：** FR-PR7、FR-O3、BR-6、`component-methods.md` Stock_Service
`reserveStock`、`services.md` 2.2 节"步骤 2"、US-08、US-28。

#### Acceptance Criteria

1. THE Internal_Stock_Controller SHALL expose an HTTP endpoint
   `POST /internal/stock/reserve` that accepts a request body containing
   `productId` (Long), `quantity` (positive Integer), and `orderRef` (non-blank
   String).
2. WHEN Order_Service invokes `POST /internal/stock/reserve` with a valid
   request whose `productId` exists and whose `quantity` is less than or
   equal to the product's Available_Stock at the moment the
   Pessimistic_Lock is held, THE Product_Service SHALL create a new
   Stock_Reservation row with Reservation_Status `RESERVED` and SHALL
   respond with a `Result` payload containing the newly generated
   Reservation_Id.
3. IF Order_Service invokes `POST /internal/stock/reserve` with a valid
   request whose `quantity` is greater than the product's Available_Stock
   at the moment the Pessimistic_Lock is held, THEN THE Product_Service
   SHALL NOT create any Stock_Reservation row and SHALL respond with a
   BusinessException whose error code belongs to the Common_Module
   business-error category and whose message identifies "stock
   insufficient".
4. IF Order_Service invokes `POST /internal/stock/reserve` with a
   `productId` that does not correspond to any non-deleted product,
   THEN THE Product_Service SHALL respond with a BusinessException whose
   error code belongs to the Common_Module not-found category.
5. IF Order_Service invokes `POST /internal/stock/reserve` with a
   non-positive `quantity` (zero or negative) or a blank `orderRef`,
   THEN THE Product_Service SHALL respond with a ParameterException whose
   error code belongs to the Common_Module parameter-error category.
6. WHEN a reserve request fails with any of AC-3.3, AC-3.4, or AC-3.5,
   THE Product_Service SHALL leave the product's Available_Stock unchanged.

---

### Requirement 4: 库存预占的幂等性（基于 Order_Ref）

**User Story:** As Order_Service, I want repeated reserve calls with the
same Order_Ref to be idempotent, so that retries due to network timeouts or
Saga re-execution do not double-reserve stock.

**关联设计依据：** NFR-5、`services.md` 第 3 节"幂等性"、`component-dependency.md`
第 2 节"幂等(reservationId / orderRef)"、US-28。

#### Acceptance Criteria

1. WHEN Order_Service invokes `POST /internal/stock/reserve` with an
   `orderRef` for which a Stock_Reservation already exists in the database
   for the same `productId`, THE Product_Service SHALL NOT create a new
   Stock_Reservation row.
2. WHEN the condition in AC-4.1 holds, THE Product_Service SHALL respond
   with the Reservation_Id of the previously created Stock_Reservation
   matching the `orderRef` and `productId`.
3. IF Order_Service invokes `POST /internal/stock/reserve` with an
   `orderRef` that already exists in the database but with a different
   `productId` or different `quantity` than the previously stored values,
   THEN THE Product_Service SHALL respond with a BusinessException whose
   error code belongs to the Common_Module business-error category and
   whose message identifies "orderRef already used with conflicting
   parameters".
4. THE Product_Service SHALL enforce uniqueness of `(order_ref, product_id)`
   on the `stock_reservation` table at the database level so that AC-4.1
   and AC-4.3 hold even under concurrent reserve attempts with the same
   `orderRef`.

---

### Requirement 5: 库存释放接口（release）

**User Story:** As Order_Service, I want to release a previously reserved
stock through an internal endpoint, so that when a redemption is cancelled
or a Saga step compensates, the reserved quantity is returned to
Available_Stock.

**关联设计依据：** FR-O7、BR-7、`component-methods.md` Stock_Service
`releaseStock`、`services.md` 2.3 节"取消"、US-14。

#### Acceptance Criteria

1. THE Internal_Stock_Controller SHALL expose an HTTP endpoint
   `POST /internal/stock/release/{reservationId}` that accepts no request
   body.
2. WHEN Order_Service invokes
   `POST /internal/stock/release/{reservationId}` with a `reservationId`
   that corresponds to a Stock_Reservation whose Reservation_Status equals
   `RESERVED`, THE Product_Service SHALL transition that
   Stock_Reservation's Reservation_Status to `RELEASED` and SHALL respond
   with a `Result.success`.
3. WHEN Order_Service invokes
   `POST /internal/stock/release/{reservationId}` with a `reservationId`
   that corresponds to a Stock_Reservation whose Reservation_Status equals
   `RELEASED`, THE Product_Service SHALL respond with `Result.success`
   without modifying any Stock_Reservation row and without modifying the
   product's stock (idempotent no-op).
4. IF Order_Service invokes
   `POST /internal/stock/release/{reservationId}` with a `reservationId`
   that corresponds to a Stock_Reservation whose Reservation_Status equals
   `CONFIRMED`, THEN THE Product_Service SHALL respond with a
   BusinessException whose error code belongs to the Common_Module
   business-error category and whose message identifies "cannot release a
   confirmed reservation".
5. IF Order_Service invokes
   `POST /internal/stock/release/{reservationId}` with a `reservationId`
   that does not correspond to any Stock_Reservation row,
   THEN THE Product_Service SHALL respond with a BusinessException whose
   error code belongs to the Common_Module not-found category.
6. WHEN AC-5.2 holds, THE Product_Service SHALL increase the product's
   Available_Stock by exactly the `quantity` of the released
   Stock_Reservation, by virtue of that Stock_Reservation no longer being
   counted in the `RESERVED` aggregate (per Requirement 2 AC-2.4).

---

### Requirement 6: 库存正式扣减接口（confirm）

**User Story:** As Order_Service, I want to convert a reserved stock into a
real deduction through an internal endpoint when an order ships, so that
the product's persisted stock truly reflects the fulfilled redemption.

**关联设计依据：** FR-O6、AS-1、`component-methods.md` Stock_Service
`confirmDeduct`、`services.md` 2.3 节"发货"、US-26。

#### Acceptance Criteria

1. THE Internal_Stock_Controller SHALL expose an HTTP endpoint
   `POST /internal/stock/confirm/{reservationId}` that accepts no request
   body.
2. WHEN Order_Service invokes
   `POST /internal/stock/confirm/{reservationId}` with a `reservationId`
   that corresponds to a Stock_Reservation whose Reservation_Status equals
   `RESERVED`, THE Product_Service SHALL transition that
   Stock_Reservation's Reservation_Status to `CONFIRMED`, SHALL deduct
   the reservation's `quantity` from the product's `stock` column, SHALL
   increase the product's `sold_count` column by the same `quantity`, and
   SHALL respond with `Result.success`.
3. WHEN Order_Service invokes
   `POST /internal/stock/confirm/{reservationId}` with a `reservationId`
   that corresponds to a Stock_Reservation whose Reservation_Status equals
   `CONFIRMED`, THE Product_Service SHALL respond with `Result.success`
   without modifying any Stock_Reservation row, without further deducting
   the product's `stock`, and without further incrementing the product's
   `sold_count` (idempotent no-op).
4. IF Order_Service invokes
   `POST /internal/stock/confirm/{reservationId}` with a `reservationId`
   that corresponds to a Stock_Reservation whose Reservation_Status equals
   `RELEASED`, THEN THE Product_Service SHALL respond with a
   BusinessException whose error code belongs to the Common_Module
   business-error category and whose message identifies "cannot confirm a
   released reservation".
5. IF Order_Service invokes
   `POST /internal/stock/confirm/{reservationId}` with a `reservationId`
   that does not correspond to any Stock_Reservation row,
   THEN THE Product_Service SHALL respond with a BusinessException whose
   error code belongs to the Common_Module not-found category.

---

### Requirement 7: 并发安全 - 悲观锁防超兑

**User Story:** As an employee redeeming a scarce product, I want concurrent
reserve attempts to be serialized so that the system never sells more units
than the on-hand stock, so that I receive a deterministic success or
sold-out outcome instead of inconsistent state.

**关联设计依据：** NFR-4、BR-8、`services.md` 第 3 节"故障与超时"、
`component-methods.md` Stock_Service `reserveStock`（"悲观锁"）、US-28。

#### Acceptance Criteria

1. WHEN the Product_Service handles `POST /internal/stock/reserve`,
   THE Product_Service SHALL acquire a Pessimistic_Lock on the target
   product row via `SELECT ... FOR UPDATE` within the same database
   transaction that reads Available_Stock, creates the Stock_Reservation,
   and commits.
2. WHILE the Pessimistic_Lock on a product row is held by one reserve
   transaction, THE Product_Service SHALL block any other reserve
   transaction targeting the same product row from reading Available_Stock
   until the holding transaction commits or rolls back.
3. WHEN N concurrent reserve requests target the same product whose
   on-hand Available_Stock is M and each request asks for 1 unit (with N
   greater than M and all `orderRef` values distinct), THE Product_Service
   SHALL accept exactly M of those requests with `Result.success` and SHALL
   reject the remaining `N - M` requests per Requirement 3 AC-3.3.
4. WHEN AC-7.3 holds, THE sum of `quantity` of Stock_Reservation rows in
   `RESERVED` state for that product SHALL be exactly M, and the product's
   `stock` column SHALL be unchanged from its pre-test value.

---

### Requirement 8: Stock_Reservation 状态机

**User Story:** As an operator of Product_Service, I want Stock_Reservation
state transitions to be strictly enforced, so that compensation and
fulfilment workflows cannot accidentally double-spend stock or leak
reservations.

**关联设计依据：** `components.md` 第 2 节"关键点"、`services.md` 2.2/2.3
节、NFR-5。

#### Acceptance Criteria

1. WHEN the Product_Service creates a new Stock_Reservation as part of a
   successful reserve operation, THE Product_Service SHALL set its initial
   Reservation_Status to `RESERVED`.
2. THE Product_Service SHALL allow Reservation_Status transitions only
   along these directed edges: `RESERVED → RELEASED` and
   `RESERVED → CONFIRMED`.
3. IF any caller attempts a state transition not listed in AC-8.2
   (including but not limited to `RELEASED → CONFIRMED`,
   `CONFIRMED → RELEASED`, `RELEASED → RESERVED`, or
   `CONFIRMED → RESERVED`), THEN THE Product_Service SHALL reject the
   attempt per the specific acceptance criteria in Requirements 5 and 6
   without modifying the Stock_Reservation row.
4. WHILE a Stock_Reservation is in Reservation_Status `RELEASED` or
   `CONFIRMED`, THE Product_Service SHALL treat that Stock_Reservation as
   terminal and SHALL NOT permit any further state change.
5. THE Product_Service SHALL persist Reservation_Status as a non-null
   column on the `stock_reservation` table.

---

### Requirement 9: 管理员调整库存

**User Story:** As an admin of Product_Service, I want to adjust a product's
absolute stock quantity through a management interface, so that I can
correct mistakes, replenish inventory, or remove damaged units without
going through the redemption flow.

**关联设计依据：** FR-PR5、`component-methods.md` Stock_Service
`adjustStock`、US-20。

#### Acceptance Criteria

1. THE Product_Service SHALL expose a management endpoint that accepts a
   `productId` (Long) and `newQty` (non-negative Integer) and adjusts the
   product's `stock` column to `newQty`.
2. WHEN an admin invokes the adjust-stock endpoint with a `productId`
   that corresponds to an existing non-deleted product and a non-negative
   `newQty`, THE Product_Service SHALL update that product's `stock`
   column to `newQty` and SHALL respond with `Result.success`.
3. IF an admin invokes the adjust-stock endpoint with a negative `newQty`,
   THEN THE Product_Service SHALL reject the request with a
   ParameterException whose error code belongs to the Common_Module
   parameter-error category.
4. IF an admin invokes the adjust-stock endpoint with a `productId` that
   does not correspond to any non-deleted product, THEN THE Product_Service
   SHALL respond with a BusinessException whose error code belongs to the
   Common_Module not-found category.
5. WHEN an admin's adjust-stock operation succeeds, THE Product_Service
   SHALL leave all existing Stock_Reservation rows for that product
   unchanged, including their Reservation_Status and `quantity` values.

---

### Requirement 10: 内网鉴权过滤器（Internal_Auth_Filter）

**User Story:** As an operator of Product_Service, I want all `/internal/**`
endpoints to be reachable only from API Gateway or other internal services,
so that external attackers cannot bypass JWT-based gateway authentication
and directly manipulate stock reservations.

**关联设计依据：** FR-G2、`components.md` 第 0 节、`services.md` 第 4 节、
`component-methods.md` 第 7 节"内部接口受 Internal_Auth_Filter 保护"、
`component-dependency.md` 第 2 节"内部接口仅内网可达"。

#### Acceptance Criteria

1. THE Internal_Auth_Filter SHALL intercept every HTTP request whose path
   matches `/internal/**` before the request reaches any
   Internal_Stock_Controller handler method.
2. WHEN a request whose path matches `/internal/**` arrives carrying a
   valid Internal_Trust_Credential, THE Internal_Auth_Filter SHALL allow
   the request to proceed to the handler.
3. IF a request whose path matches `/internal/**` arrives without a valid
   Internal_Trust_Credential, THEN THE Internal_Auth_Filter SHALL reject
   the request with HTTP status 401 (Unauthorized) or 403 (Forbidden) and
   SHALL NOT invoke any Internal_Stock_Controller handler method.
4. WHEN the Internal_Auth_Filter rejects a request per AC-10.3,
   THE Product_Service SHALL leave all Product and Stock_Reservation rows
   unchanged.
5. THE Internal_Auth_Filter SHALL NOT intercept requests whose path does
   not match `/internal/**` (for example, the existing
   `/api/v1/public/product/list` and `/api/v1/public/product/create`
   endpoints), so that public endpoints remain governed by the API Gateway
   and existing controllers behave unchanged.
6. WHEN the Product_Service emits a structured log entry for an
   Internal_Auth_Filter rejection, THE log entry SHALL include the request
   path and the rejection reason, so that operators can audit attempted
   external probes.

---

---

### Requirement 11: 二级分类管理（Category）

**User Story:** As an admin of Product_Service, I want to manage a two-level
category tree, so that products can be organized and employees can browse by
category.

**关联设计依据：** FR-PR3、`components.md` 第 2 节 CategoryController /
CategoryService / Category、US-05、US-21。

#### Acceptance Criteria

1. THE Product_Service SHALL persist a Category with `id`, `name`,
   `parentId` (nullable for top-level), and standard audit fields, supporting
   exactly two levels (a parent category and its child categories).
2. WHEN an admin creates a category with a `parentId` that refers to an
   existing top-level category, THE Product_Service SHALL create it as a
   second-level (child) category.
3. IF an admin creates a category with a `parentId` that itself already has a
   non-null `parentId` (i.e., would create a third level), THEN THE
   Product_Service SHALL reject the request with a BusinessException whose
   error code belongs to the Common_Module business-error category.
4. WHEN any client requests the category tree, THE Product_Service SHALL
   return the categories as a two-level tree (parents with nested children).
5. WHEN an admin updates a category's `name`, THE Product_Service SHALL
   persist the change and return the updated CategoryDTO.
6. IF an admin deletes a category that still has child categories or
   associated non-deleted products, THEN THE Product_Service SHALL reject the
   request with a BusinessException whose error code belongs to the
   Common_Module business-error category and whose message identifies
   "category in use".
7. WHEN an admin deletes a category that has no children and no associated
   products, THE Product_Service SHALL soft-delete it and respond with
   `Result.success`.

---

### Requirement 12: 商品详情查询（getById）

**User Story:** As an employee, I want to retrieve a single product's full
details, so that I can decide whether to redeem it.

**关联设计依据：** FR-PR4、`component-methods.md` ProductService `getProduct`、
US-07。

#### Acceptance Criteria

1. THE Product_Service SHALL expose `POST /api/v1/public/product/get`
   accepting a request body containing `productId` (Long, `@NotNull`).
2. WHEN a client requests an existing non-deleted product, THE Product_Service
   SHALL respond with a `Result` payload containing the full ProductDTO
   (including `productType` and `category`).
3. IF a client requests a `productId` that does not correspond to any
   non-deleted product, THEN THE Product_Service SHALL respond with a
   BusinessException whose error code belongs to the Common_Module not-found
   category.

---

### Requirement 13: 商品编辑、上下架与删除（CRUD 补全）

**User Story:** As an admin, I want to edit, change the listing status of, and
delete products, so that I can maintain the catalog.

**关联设计依据：** FR-PR1、`component-methods.md` ProductService
`updateProduct`/`changeStatus`/`deleteProduct`、US-19。

#### Acceptance Criteria

1. THE Product_Service SHALL expose `POST /api/v1/public/product/update`
   accepting an `UpdateProductRequest` containing `productId` and editable
   fields, and SHALL persist the changes for an existing product.
2. IF an update targets a `productId` that does not correspond to any
   non-deleted product, THEN THE Product_Service SHALL respond with a
   BusinessException whose error code belongs to the Common_Module not-found
   category.
3. IF an update sets `productType` to a value outside `{PHYSICAL, VIRTUAL}`,
   THEN THE Product_Service SHALL reject the request with a ParameterException
   whose error code belongs to the Common_Module parameter-error category.
4. THE Product_Service SHALL expose `POST /api/v1/public/product/change-status`
   accepting `productId` and `status` (0=下架/off-shelf, 1=上架/on-shelf), and
   SHALL update the product's `status` accordingly.
5. WHEN a product's `status` is 0 (off-shelf), THE Product_Service SHALL
   exclude it from the public product list (`/list`) results.
6. THE Product_Service SHALL expose `POST /api/v1/public/product/delete`
   accepting `productId`, and SHALL soft-delete the product (set `deleted=1`)
   so that it no longer appears in any query.
7. IF a delete or change-status targets a `productId` that does not correspond
   to any non-deleted product, THEN THE Product_Service SHALL respond with a
   BusinessException whose error code belongs to the Common_Module not-found
   category.

---

### Requirement 14: 商品搜索增强（关键字 + 分类）

**User Story:** As an employee, I want to search products by keyword and
filter by category, so that I can quickly find the products I want.

**关联设计依据：** FR-PR4、`component-methods.md` ProductService
`listProducts(page, categoryId, keyword)`、US-06。

#### Acceptance Criteria

1. THE `POST /api/v1/public/product/list` endpoint SHALL accept optional
   `keyword` and optional `categoryId` filter parameters in addition to
   pagination parameters.
2. WHEN a `keyword` is provided, THE Product_Service SHALL return only
   products whose `name` matches the keyword (substring match).
3. WHEN a `categoryId` is provided, THE Product_Service SHALL return only
   products belonging to that category.
4. WHEN both `keyword` and `categoryId` are provided, THE Product_Service
   SHALL return only products satisfying BOTH filters.
5. WHEN no products match the filters, THE Product_Service SHALL respond with
   an empty `PageResult` (not an error).

---

### Requirement 15: 商品图片上传（ImageStorageComponent）

**User Story:** As an admin, I want to upload a product image, so that the
product is displayed with an accurate picture.

**关联设计依据：** FR-PR6、`components.md` 第 2 节 ImageStorageComponent、
`component-methods.md` `uploadImage`、US-20。

#### Acceptance Criteria

1. THE Product_Service SHALL expose an endpoint that accepts a multipart image
   file for a given `productId` and stores the file on a local file volume.
2. WHEN an admin uploads a valid image for an existing product, THE
   Product_Service SHALL store the file, set the product's `imageUrl` to a
   generated access URL, and respond with a `Result` payload containing that
   URL.
3. IF an admin uploads a file whose content type is not an image (e.g., not
   `image/*`), THEN THE Product_Service SHALL reject the request with a
   ParameterException whose error code belongs to the Common_Module
   parameter-error category.
4. IF an admin uploads an image for a `productId` that does not correspond to
   any non-deleted product, THEN THE Product_Service SHALL respond with a
   BusinessException whose error code belongs to the Common_Module not-found
   category.

---

### Requirement 16: 售罄判定（Sold-Out）

**User Story:** As an employee, I want sold-out products to be clearly marked,
so that I do not attempt to redeem unavailable items.

**关联设计依据：** FR-PR5、`components.md` 第 2 节 ProductService「售罄判定」、
US-04。

#### Acceptance Criteria

1. WHEN the Product_Service returns a ProductDTO through any public endpoint,
   THE ProductDTO SHALL include a derived `soldOut` boolean that is true when
   the product's Available_Stock is 0 and false otherwise.
2. THE `soldOut` value SHALL be computed from the same Available_Stock
   definition as Requirement 2 AC-2.4 (total stock minus RESERVED quantity).

---

## Traceability Matrix（需求 → 设计依据）

| Requirement | FR / NFR / BR / AS | User Stories |
|-------------|---------------------|--------------|
| Req 1 商品类型字段 | FR-PR2, AS-1, AS-2, AS-3 | US-07, US-19 |
| Req 2 可用库存查询 | FR-PR7 | US-08, US-26 |
| Req 3 reserve 接口 | FR-PR7, FR-O3, BR-6 | US-08, US-28 |
| Req 4 reserve 幂等 | NFR-5 | US-28 |
| Req 5 release 接口 | FR-O7, BR-7 | US-14 |
| Req 6 confirm 接口 | FR-O6, AS-1 | US-26 |
| Req 7 并发安全悲观锁 | NFR-4, BR-8 | US-28 |
| Req 8 状态机 | NFR-5 | US-28 |
| Req 9 管理员调整库存 | FR-PR5 | US-20 |
| Req 10 内网鉴权过滤器 | FR-G2, NFR-3 | US-02, US-18（间接） |
| Req 11 二级分类管理 | FR-PR3 | US-05, US-21 |
| Req 12 商品详情查询 | FR-PR4 | US-07 |
| Req 13 商品 CRUD 补全 | FR-PR1 | US-19 |
| Req 14 商品搜索增强 | FR-PR4 | US-06 |
| Req 15 商品图片上传 | FR-PR6 | US-20 |
| Req 16 售罄判定 | FR-PR5 | US-04 |
