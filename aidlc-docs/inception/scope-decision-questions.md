# 范围决策问题 - 达成 Product Service 模块级完全一致

> 阶段：INCEPTION - 范围澄清
> 目标：使 product-service 设计与 plan 的「Product Service 模块」(components.md §2 +
> FR-PR1~PR7) 完全一致。
> 待补齐的缺口：FR-PR3 二级分类、FR-PR1/PR4 商品 CRUD 补全、FR-PR6 图片上传、
> FR-PR5 售罄判定（FR-PR2/PR5 调库存/PR7 库存预占/内网鉴权已在现有 design 覆盖）。
> 请在每个 [Answer]: 后填字母，完成后回复"完成"。

---

## Question 1
如何组织"补齐缺口"的设计与实现产物，以达到模块级完全一致？

A) 在现有 `product-stock-reservation-api` spec 内扩展：把分类/CRUD/图片/售罄并入同一 requirements+design+tasks（单一大 spec，模块视角最集中）
B) 新建一个配套 spec（如 `product-catalog-completion`），与现有 P0 spec 并存；两者合起来 = 完整模块（关注点分离，P0 已批准部分不被改动）
C) 重构为单一「模块级」spec（如 `product-service-module`），把现有 P0 内容并入，形成唯一权威的模块设计（最贴合"模块完全一致"，但需重组已批准的 P0 文档）
X) Other（在 [Answer]: 后描述）

[Answer]: 

---

## Question 2
二级分类（FR-PR3）在本服务的范围内确认要完整实现吗？（plan 的 CategoryController/CategoryService/Category 实体/category 表 + Product↔Category 关联）

A) 是，完整实现（与 plan 完全一致）
B) 仅设计、暂不纳入本轮实现（design 写全，tasks 标记为后续）
X) Other（在 [Answer]: 后描述）

[Answer]: 

---

## Question 3
图片上传（FR-PR6 / ImageStorageComponent）的落地方式？plan 描述为"本地卷 + URL 生成"。

A) 按 plan：本地文件卷（Docker Volume）存储 + 返回访问 URL
B) 仅在 design 中定义组件契约，实现留待基础设施就绪
X) Other（在 [Answer]: 后描述）

[Answer]: 

---

## Question 4
商品公开 CRUD 补全的 URL 风格遵循现有 `layer-interface.md` 约定（`/api/v1/public/product/{action}`，全 POST）吗？

A) 是，沿用现有约定（get/update/changeStatus/delete/list 均 POST）
B) 改为 RESTful 风格（GET/PUT/DELETE + 路径变量）
X) Other（在 [Answer]: 后描述）

[Answer]: 
