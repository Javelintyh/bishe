## 轻量级生产管理平台（东莞中小制造企业）

### 项目概述

本项目为面向东莞中小制造企业的**轻量级生产管理平台**，采用前后端分离架构，覆盖从**基础数据→订单→工单→报工/质量→库存→采购→设备→看板/报表**的完整业务闭环，并针对三类核心角色提供精简界面：

- **管理端（ADMIN）**：老板/管理员，负责系统配置、全局监控、数据分析与管理操作。
- **车间端（WORKSHOP）**：车间主管/工人，聚焦生产执行与报工。
- **仓库端（WAREHOUSE）**：仓管员，聚焦出入库与库存管理。

技术栈：

- **前端**：Vite + Vue 3 + TypeScript + Pinia + Vue-Router + Element-Plus + Axios + ECharts
- **后端**：Spring Boot 4 + Spring Security + MyBatis-Plus（boot4 starter）+ JWT
- **数据库**：MySQL（推荐 8.x）

### 目录结构

- `frontend/`：前端工程（Vite 项目）
  - `src/main.ts`：应用入口
  - `src/router/index.ts`：路由配置（按角色分模块）
  - `src/layouts/MainLayout.vue`：三端共用主布局
  - `src/views/`：各端页面（管理端/车间端/仓库端/登录）
- `backend/`：后端工程（Maven 项目）
  - `pom.xml`：依赖管理（Spring Boot、MyBatis-Plus、JWT 等）
  - `src/main/resources/application.properties`：后端配置（含 MySQL 连接、JWT 配置）
  - `src/main/resources/mes_full.sql`：**完整 MySQL 表结构 + 示例数据脚本**
  - `src/main/java/com/example/backend/module/**/*`：按业务模块划分的 Controller / Service / Mapper / Entity

### 环境要求

- Node.js：建议 `>=20`
- npm / pnpm：任选其一（项目默认使用 npm 脚本）
- JDK：`17`
- Maven：`>=3.8`
- MySQL：`8.x`（其他版本需自行验证）

### 一、数据库初始化（MySQL）

1. 登录 MySQL，创建数据库并切到该库（如已存在可跳过创建，仅执行 `USE`）：

```sql
CREATE DATABASE IF NOT EXISTS mes
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE mes;
```

2. 在数据库客户端中执行脚本文件 `backend/src/main/resources/mes_full.sql`：

- 该脚本将：
  - **清理并重建全部业务表结构**（多次执行不会报错）。
  - 插入**示例数据**（角色、管理员账户、部分物料/BOM/客户/供应商/质量原因/设备/库存/订单等）。

> 如不需要示例数据，可手动删除/注释文件末尾的 `INSERT` 段，仅保留建表语句。

### 二、配置后端 MySQL 连接

编辑 `backend/src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mes?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
spring.datasource.username=你的用户名
spring.datasource.password=你的密码
```

其他关键配置（已内置）：

- `app.jwt.secret`：JWT 密钥（默认是开发环境用的字符串，如需生产使用请自行替换为足够复杂的随机串）。
- `app.jwt.ttl-minutes`：登录 Token 有效期（分钟）。

### 三、启动后端（Spring Boot）

1. 进入后端目录并构建：

```bash
cd backend
mvn -DskipTests package
```

2. 启动开发服务：

```bash
mvn spring-boot:run
```

默认：

- 端口：`8080`
- 核心接口前缀：`/api/**`
- 鉴权：基于 JWT 的 RBAC（角色：`ADMIN` / `WORKSHOP` / `WAREHOUSE`）

### 四、启动前端（Vite + Vue 3）

1. 安装依赖：

```bash
cd frontend
npm install
```

2. 启动开发服务：

```bash
npm run dev
```

默认会在浏览器中打开一个本地地址（例如 `http://localhost:5173`），前端会通过 Axios 调用 `http://localhost:8080` 的后端 API。

如后端不是本机或端口不同，可以在前端 `.env` 中配置：

```bash
VITE_API_BASE_URL=http://your-backend-host:8080
```

### 五、默认登录账号与角色说明

执行 `mes_full.sql` 后，会有一个默认管理员账号：

- **用户名**：`admin`
- **密码**：`admin123`
- **角色**：`ADMIN`（管理端）

登录成功后即可通过「用户管理」功能创建车间端与仓库端账号，并分配：

- `WORKSHOP`：车间端用户（车间看板、待办工单、报工、质量登记）。
- `WAREHOUSE`：仓库端用户（库存看板、出入库操作、库存流水）。

### 六、主要功能模块说明（按角色）

#### 1. 管理端（ADMIN）

- **系统管理**
  - 用户管理：用户增删改查、角色分配、启用/停用、重置密码。
  - 个人中心：修改密码。
- **基础数据**
  - 物料管理：原材料/成品维护，含编码、名称、规格、单位、安全库存。
  - BOM 管理：单层 BOM 维护（产品→组成物料清单）。
  - 客户管理：客户信息维护。
  - 供应商管理：供应商信息维护。
- **订单管理**
  - 订单录入：客户、产品、数量、交货期等。
  - 订单列表/详情：按状态筛选，查看明细与关联工单。
  - 状态管理：待处理 → 生产中 → 已完成 → 已交付。
- **生产管理**
  - 工单管理：根据订单自动生成工单；查看工单列表/详情。
  - 生产进度监控：查看各工单状态、完成数量。
  - 报工记录查询：按工单/日期查询报工与质量记录。
  - **物料齐套检查**：对指定工单执行 BOM × 工单数量 vs 库存的齐套性检查，输出缺料清单。
- **库存管理**
  - 库存总览：所有物料当前库存。
  - 库存流水：按物料/业务类型（采购入库、生产入库、生产领料、销售出库、调整）查询。
- **采购管理（简化）**
  - 采购订单维护：创建 PO、维护明细。
  - 收货登记：采购收货并关联入库。
- **报表与看板**
  - 订单准时交付率：基于订单计划/实际交期统计。
  - 生产计划完成率：计划工单 vs 实际完工。
  - 库存周转情况：通过库存流水与数量估算周转。
  - ECharts 可视化看板：订单进度、生产进度等折线/柱状图。

#### 2. 车间端（WORKSHOP）

- 今日生产任务：待办工单列表（按状态/负责人过滤）。
- 工单详情：查看关联订单、产品、数量、工序。
- 生产报工：
  - 支持输入工单号/扫码录入（前端提供输入框，扫码等可拓展）。
  - 填写工序、合格数量、不良数量、不良原因（下拉+文本）。
- 质量登记：报工时直接录入质量信息；可通过原因字典统一管理。
- 历史记录：个人报工记录列表。
- 车间看板：当天计划 vs 实际完成量的简单图表。

#### 3. 仓库端（WAREHOUSE）

- 库存看板：物料库存列表，低于安全库存高亮预警。
- 入库管理：
  - 采购入库：选择采购单/手工选择物料入库。
  - 生产入库：选择已完工工单入库。
- 出库管理：
  - 生产领料：根据工单/BOM 出库物料。
  - 销售出库：根据订单出库成品。
- 库存流水：按物料/单据类型过滤查看历史出入库记录。

### 七、打印与消息提醒（后端支持）

- **打印**
  - 后端预留了工单打印、出入库单据打印的数据接口，前端可开发专用打印页面并使用浏览器打印功能即可。
- **消息提醒**
  - 已有 `message_notice` 表和基础接口，可用于记录工单超期提醒、库存预警等消息。
  - 可通过定时任务或手工触发在表中插入提醒记录，前端做「消息中心」列表展示。

### 八、如何扩展或二次开发

- 如需增加字段：
  - 先在 MySQL 中修改对应表结构，再同步更新 MyBatis-Plus 的实体类字段和前端表单/表格。
- 如需增加新角色（如质检员、采购员）：
  - 在 `sys_role` 中新增角色编码。
  - 在后端使用 Spring Security 的 `@PreAuthorize` 注解增加权限判断。
  - 在前端根据 `roleCode` 动态控制菜单和路由访问。

### 九、常见问题

- **启动时报数据库连接错误**：检查 `application.properties` 中的 URL、用户名、密码与 MySQL 实际配置是否一致；确认已执行 `mes_full.sql`。
- **无法登录**：确认已执行示例数据中的用户插入语句，或自行在 `sys_user` 表中插入一条带 BCrypt 密码的管理员账号。
- **前端 401 未授权**：确保登录接口返回的 token 被正确存入浏览器，并在后续请求中通过 `Authorization: Bearer xxx` 头部发送（前端已在 Axios 拦截器中处理，一般只需确认后端地址正确）。 

