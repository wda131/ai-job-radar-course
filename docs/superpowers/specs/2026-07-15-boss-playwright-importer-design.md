# BOSS 职位 Playwright 导入器设计

## 1. 目标与定位

在不改变课程项目 Spring Cloud 主架构的前提下，增加一个可选的 Node.js Playwright 职位导入工具。工具使用用户手动授权的 BOSS 登录态读取当前页面中公开可见的职位信息，将职位规范化后通过 Gateway 调用 `job-service`，最终写入现有 MySQL `jobs` 表，并继续参与职位检索、收藏、投递、AI 匹配和模拟面试。

该工具是数据入口，不是新的核心微服务。即使 Playwright、BOSS 页面或网络不可用，项目原有演示数据与全部主业务仍可运行。

## 2. 明确不做的功能

- 不自动输入或保存 BOSS 账号密码。
- 不绕过验证码、滑块、风控或访问限制。
- 不自动投递、自动沟通、自动交换联系方式。
- 不批量采集个人信息、聊天信息或非职位数据。
- 不让后端服务直接控制用户日常浏览器。
- 不把 Python/FastAPI 重新引入课程重制版。
- 不承诺适配除 BOSS 以外的招聘平台。

## 3. 方案比较与结论

### 方案 A：独立 Node.js Playwright 导入器（采用）

新增 `job-importer/`，由命令行手动启动。它负责浏览器生命周期、页面解析和调用课程项目导入接口。该方案可以直接体现必选技术 Node.js，同时与 Spring Boot 微服务隔离，故障不会影响主系统。

### 方案 B：在 `job-service` 中使用 Playwright Java

浏览器进程、登录态和页面解析会进入核心微服务，增加启动时间、依赖和故障面，不利于课堂演示和服务职责划分，因此不采用。

### 方案 C：复用原项目 Python 服务

原项目只有 Playwright 依赖、浏览器状态骨架和 Mock 数据源，没有真实 BOSS 采集实现。继续扩展 Python 会形成第二套后端技术路线，与课程要求不一致，因此不采用。

## 4. 总体架构

```text
job-importer（Node.js + Playwright）
  ├─ 持久化 Chromium 用户目录
  ├─ 用户手动扫码登录 BOSS
  ├─ 解析当前搜索结果和职位详情
  ├─ 规范化职位字段并限制单次最多 20 条
  └─ 使用课程账号登录并携带 JWT 调用 Gateway
                         ↓
Gateway :9000 → job-service :9002
                         ├─ 按 source + external_id 新增或更新 MySQL
                         ├─ 清理 Redis 职位缓存
                         └─ 尝试更新 Elasticsearch；失败时保留 MySQL 路径
```

## 5. Node.js 导入器

### 5.1 目录职责

```text
job-importer/
  package.json
  src/cli.js                 参数解析和执行汇总
  src/browserSession.js      Playwright 持久化浏览器上下文
  src/bossSource.js          BOSS 页面读取与受控翻页
  src/normalizeJob.js        薪资、经验、城市和字段标准化
  src/importClient.js        登录课程系统并提交批量导入
  test/fixtures/             脱离外部网站的 HTML 样本
  test/*.test.js             Node 内置测试
```

使用 Node.js 24 和 Playwright 1.49.1。浏览器用户目录为 `job-importer/.browser-data/`，必须加入 `.gitignore`，不得上传 Cookie、缓存或登录状态。

### 5.2 命令行接口

```powershell
npm run import:boss -- --keyword Java --city 威海 --limit 20
```

- `keyword` 必填。
- `city` 可选。
- `limit` 默认 10，最大 20。
- 默认显示浏览器，方便用户扫码、处理普通登录提示并观察采集页面。
- 课程系统登录信息从 `RADAR_USERNAME`、`RADAR_PASSWORD` 读取；本地演示默认使用 `student / 123456`。
- 导入器只在内存中保存本次 JWT，进程退出即丢弃。

### 5.3 页面读取边界

- 只读取用户当前可见的职位列表和职位详情。
- 页面要求登录时暂停并提示用户手动完成扫码登录。
- 页面出现验证码、滑块或风控提示时停止本次任务并输出原因，不尝试规避。
- 每读取一个详情加入小幅间隔，单次最多 20 条，不实现持续后台爬取。
- 某一职位结构缺失时记录错误并继续处理其他职位。

### 5.4 标准化规则

导入器生成以下字段：

| 字段 | 规则 |
| --- | --- |
| `source` | 固定为 `BOSS` |
| `externalId` | 优先使用职位链接中的平台职位编号，否则对规范化链接生成稳定摘要 |
| `title` | 去除首尾空白，不能为空 |
| `company` | 去除首尾空白，不能为空 |
| `city` | 取页面工作城市，缺失时使用命令行城市 |
| `salaryMin/Max` | 将 `K`、月薪范围转换为整数元；面议或无法解析时记 0 |
| `experienceYears` | `经验不限/应届` 为 0，范围取下限 |
| `education` | 缺失时使用 `不限` |
| `description` | 职位描述，最大 1000 字符 |
| `requirements` | 技能与任职要求合并，最大 1000 字符 |
| `welfareTags` | 福利标签以逗号分隔，最大 300 字符 |
| `sourceUrl` | 规范化后的职位详情链接 |
| `status` | 固定为 `OPEN` |

解析结果必须经过本地校验，缺少标题、公司或外部编号的记录不得提交。

## 6. `job-service` 导入接口

新增已登录用户可调用的接口：

```http
POST /api/jobs/import
Authorization: Bearer <course JWT>
Content-Type: application/json
```

请求体为最多 20 条职位数组。服务端再次执行数量、长度、来源和 URL 校验，不能信任导入器输入。

返回结构：

```json
{
  "created": 8,
  "updated": 2,
  "rejected": 1,
  "errors": ["第 6 条：职位标题不能为空"]
}
```

一次请求在事务中逐条处理。单条业务校验失败记入 `errors`，其他合法记录继续导入；数据库异常导致整个事务回滚。

## 7. 数据库与幂等性

给 `jobs` 表增加：

```text
source       VARCHAR(20)  NOT NULL DEFAULT 'LOCAL'
external_id  VARCHAR(100) NULL
source_url   VARCHAR(1000) NOT NULL DEFAULT ''
imported_at  DATETIME NULL
```

增加唯一索引：

```text
UNIQUE KEY uk_jobs_source_external (source, external_id)
```

本地种子数据的 `external_id` 保持 `NULL`；MySQL 允许唯一索引中存在多条 `NULL`。BOSS 导入记录必须具有非空 `external_id`。

导入时先按 `source + external_id` 查询：不存在则新增，存在则只更新职位展示字段，不修改用户收藏、投递、匹配或面试历史。

## 8. Redis 与 Elasticsearch 一致性

批量导入事务成功后清理 `jobs` 和 `job-detail` 两类 Redis 缓存，确保下一次查询读取新数据。

随后尝试将本次新增或更新的职位写入 Elasticsearch。Elasticsearch 不可用时记录警告但不回滚 MySQL；关键词搜索继续使用现有 MySQL 回退逻辑。MySQL 始终是唯一真实数据源。

## 9. 前端展示

不增加独立管理后台。导入完成后刷新“职位雷达”即可看到新职位。职位卡片增加一个小型数据来源标记，例如 `BOSS` 或 `本地数据`，用于答辩时说明真实数据入口；收藏、投递、AI 匹配和模拟面试操作保持不变。

## 10. 安全与合规边界

- 登录完全由用户在可见浏览器中完成。
- Cookie 和浏览器数据只保存在本机被忽略的目录中。
- 不打印 Cookie、Token、账号密码或完整浏览器存储。
- 不包含验证码识别、代理池、指纹伪装或反检测代码。
- 工具用于本地课程演示和用户主动执行，不作为无人值守采集服务。
- 用户应根据目标网站当时有效的使用规则决定是否执行真实导入；工具检测到访问限制时主动停止。

## 11. 测试策略

### Node.js

- 使用保存的脱敏 HTML fixture 测试职位卡片和详情解析。
- 测试薪资、经验、城市、标签和 URL 标准化。
- 测试缺失关键字段时拒绝记录。
- 测试批量上限为 20。
- 测试导入客户端正确登录并提交 JWT，但使用本地假 HTTP 服务，不访问 BOSS。

### Java

- `JobImportServiceTest` 覆盖新增、重复更新、非法来源、单条拒绝和数据库异常。
- Controller 测试覆盖最多 20 条、JWT 拦截和返回汇总。
- 验证导入成功后缓存失效。
- 验证 Elasticsearch 写入失败时 MySQL 数据仍然成功保存。

### 集成验证

1. 先用 fixture 模式导入测试职位并运行全量后端、前端测试。
2. 启动全部微服务，执行冒烟测试确认原功能不回归。
3. 最后在可见浏览器中由用户手动登录，受控导入少量真实职位。
4. 在职位雷达中确认来源标记、去重、搜索、收藏、匹配和面试均可用。

## 12. 完成标准

- 原课程项目在不启动导入器时保持完全可运行。
- fixture 导入和真实手动导入使用同一套解析与提交路径。
- 同一 BOSS 职位重复导入只更新一行。
- 导入后刷新职位雷达即可查询新职位。
- Redis 不返回导入前的旧列表。
- Elasticsearch 不可用时不影响导入和 MySQL 搜索。
- 项目不包含账号密码、Cookie、验证码绕过或自动投递能力。
