# BOSS 普通 Chrome 岗位导入扩展设计

## 1. 目标

在不使用自动化登录、浏览器指纹伪造、Cookie/token 导出或 BOSS 非公开接口的前提下，让用户在普通 Chrome 中正常登录 BOSS，主动将当前搜索结果页已经加载的真实岗位导入 AI 求职雷达课程项目。

成功标准：

- 用户能够在普通 Chrome 中正常扫码登录 BOSS。
- 扩展能够从当前搜索结果页读取最多 50 个已加载岗位。
- 岗位通过本机桥接程序和现有 Gateway/JWT 导入 `job-service`。
- 同一 BOSS 岗位重复导入时只更新，不新增重复记录。
- Redis 缓存、Elasticsearch 索引和前端来源标记继续沿用现有实现。
- 扩展停止或未安装时，课程项目其他功能不受影响。

该方案不能承诺 BOSS 页面结构永远不变，也不承诺外部网站永远可访问；它保证使用用户当前能够正常打开的页面，不主动绕过访问限制。

## 2. 方案选择

### 2.1 采用方案

采用“Manifest V3 Chrome 扩展 + 本地 Node.js 桥接程序”。

```text
普通 Chrome 中的 BOSS 搜索结果页
        │ 用户点击扩展
        ▼
Chrome 扩展内容脚本（最多 50 条）
        │ HTTP POST，仅发送岗位字段
        ▼
127.0.0.1:9011 本地导入桥接程序
        │ 规范化 + 课程账号登录
        ▼
Spring Cloud Gateway :9000
        ▼
job-service :9002
        ├─ MySQL 幂等新增/更新
        ├─ Redis 缓存失效
        └─ Elasticsearch 尽力同步
```

### 2.2 未采用方案

- 扩展直接调用 Gateway：需要在扩展中处理课程账号、JWT 和跨域，职责过重。
- 书签脚本：容易被页面 CSP 和跨域策略阻止，不适合作为答辩演示方案。
- Playwright 反检测流程：会继续触发平台风控，并涉及隐藏自动化和指纹伪造，不纳入课程项目。

## 3. 组件设计

### 3.1 Chrome 扩展

新增目录 `boss-chrome-extension/`，使用 Manifest V3，不引入构建工具。

主要文件：

- `manifest.json`：声明扩展信息、弹窗、BOSS 页面内容脚本以及本机桥接地址权限。
- `extractor.js`：纯 DOM 岗位提取器，便于使用本地 fixture 自动化测试。
- `content.js`：响应弹窗消息，在当前标签页调用提取器。
- `popup.html`、`popup.css`、`popup.js`：显示连接状态、检测数量、导入按钮和导入结果。
- `icons/`：简单的项目本地图标，不加载远程资源。

扩展只在以下页面启用：

```text
https://www.zhipin.com/web/geek/job*
https://www.zhipin.com/web/geek/jobs*
```

扩展读取当前 DOM 中已经加载的岗位卡片，不主动滚动、不自动翻页、不打开详情页。用户可以先手动滚动，再点击导入。

### 3.2 岗位提取器

提取器兼容 BOSS 当前常见卡片选择器，并要求每条记录必须具备：

- 可解析的 `/job_detail/<externalId>.html` 链接
- 岗位名称
- 公司名称

可选字段包括城市、薪资、经验、学历和卡片标签。列表页通常没有完整 JD，因此：

- `description` 使用“来自 BOSS 当前可见岗位列表，完整信息请查看来源链接”。
- `requirements` 使用当前卡片可见技能标签；没有标签时使用相同的来源提示。
- `sourceUrl` 保存真实岗位链接。
- `source` 固定为 `BOSS`。

提取器按 `externalId` 去重，最多返回 50 条当前页面已加载岗位。

### 3.3 本地导入桥接程序

在现有 `job-importer` 中新增桥接入口，监听：

```text
http://127.0.0.1:9011
```

端点：

- `GET /health`：扩展检查桥接程序是否启动。
- `POST /import`：接收扩展发送的原始岗位列表，规范化后调用现有 `loginAndImport()`。

桥接程序复用：

- `normalizeJob()`：薪资、经验、长度和默认字段处理。
- `loginAndImport()`：课程账号登录、JWT 获取与 Gateway 导入。

桥接程序从环境变量读取：

```text
API_BASE_URL=http://127.0.0.1:9000
RADAR_USERNAME=student
RADAR_PASSWORD=123456
```

课程密码和 JWT 不返回给扩展，也不写入日志。

### 3.4 启动脚本

新增 `scripts/start-boss-bridge.ps1`：

- 检查 Node.js 和依赖。
- 检查 Gateway 9000 端口。
- 检查三个课程系统环境变量。
- 启动仅绑定 `127.0.0.1:9011` 的桥接程序。
- 输出 Chrome 扩展加载目录和操作提示。

现有 Playwright fixture 模式保留，作为自动化回归和无网络答辩兜底；Playwright 真实登录模式在文档中标记为可能受平台风控影响，不作为推荐入口。

### 3.5 Java 后端

现有 `POST /api/jobs/import`、MySQL 幂等逻辑、Redis 缓存失效和 Elasticsearch 同步保持不变，只将单批上限从 20 调整为 50，并同步更新错误信息和测试。

数据库结构无需新增字段。

## 4. 数据流

1. 用户在普通 Chrome 中正常登录 BOSS。
2. 用户打开岗位搜索结果页并按需手动滚动。
3. 用户打开扩展弹窗，扩展先请求桥接程序 `/health`。
4. 用户点击“检测岗位”，内容脚本提取当前 DOM 数据并返回数量。
5. 用户点击“导入当前页面”，弹窗向 `/import` 提交最多 50 条岗位。
6. 桥接程序逐条调用 `normalizeJob()`，跳过无身份字段的数据。
7. 桥接程序使用课程账号登录 Gateway，并提交 `{jobs}`。
8. `job-service` 以 `(source, external_id)` 查询并新增或更新。
9. 导入事务成功后清除 `jobs`、`job-detail` Redis 缓存，并尽力写入 Elasticsearch。
10. 扩展显示新增、更新、拒绝数量；用户刷新课程项目岗位页查看结果。

## 5. 权限与安全边界

- 扩展不读取 `document.cookie`。
- 扩展不访问 Chrome Cookie API。
- 扩展不提取 localStorage、sessionStorage 或 BOSS token。
- 扩展不调用 BOSS XHR、GraphQL 或其他非公开接口。
- 扩展不修改 `navigator.webdriver`、Canvas、WebGL、User-Agent 或其他指纹。
- 扩展不自动导航、翻页、投递、聊天或填写表单。
- 桥接程序只绑定回环地址，不监听局域网或公网地址。
- 桥接程序只接受带自定义 `X-Radar-Bridge: 1` 请求头的 POST，并拒绝普通网页 Origin。
- CORS 只允许 `chrome-extension://` 来源；健康检查不返回敏感信息。
- 日志不输出课程密码、JWT、BOSS 页面内容或浏览器存储。

## 6. 错误处理

- 桥接程序未启动：弹窗显示 `请先运行 scripts/start-boss-bridge.ps1`。
- Gateway 未启动：启动脚本在运行前阻止继续。
- 课程账号错误：桥接程序返回安全的登录失败信息。
- 当前标签页不是支持的 BOSS 搜索页：弹窗提示打开岗位搜索结果。
- 未识别到岗位：不提交空批次，并提示页面结构可能变化或需要滚动加载。
- 超过 50 条：提取器在浏览器端截断，桥接程序和 Java 后端再次校验。
- 部分岗位无效：有效岗位继续导入，弹窗显示拒绝数量与简短原因。
- Elasticsearch 不可用：MySQL 导入继续成功，沿用现有降级逻辑。

## 7. 测试与验收

### 7.1 Chrome 扩展测试

- 使用本地 fixture 和真实 Chromium 页面运行提取器。
- 验证标题、公司、城市、薪资、经验、学历、标签和 externalId。
- 验证重复 externalId 去重。
- 验证最多返回 50 条。
- 验证空页面和非岗位页面返回明确错误。

### 7.2 桥接程序测试

- 使用 Node 本地 HTTP 测试 `/health`。
- 拒绝缺少 `X-Radar-Bridge` 的导入请求。
- 拒绝普通网页 Origin。
- 验证规范化后调用课程导入客户端。
- 验证 Gateway 错误不会泄露密码或 JWT。

### 7.3 Java 与前端回归

- Java 导入服务接受 1–50 条并拒绝 51 条。
- 新增、更新、部分拒绝、Redis 缓存和 Elasticsearch 降级测试继续通过。
- 前端来源标记和现有收藏、投递、匹配、面试测试继续通过。

### 7.4 手工验收

1. 在 Chrome 开发者模式加载 `boss-chrome-extension/`。
2. 启动桥接程序并确认扩展显示“本地桥接已连接”。
3. 普通 Chrome 登录 BOSS，打开搜索结果并滚动。
4. 扩展检测并导入真实岗位。
5. 在 MySQL 中确认 `source='BOSS'` 和真实 externalId/sourceUrl。
6. 重复导入同一页面，确认第二次为更新且总数不增加。
7. 刷新 `/jobs`，确认真实岗位与 BOSS 来源标记可见。

## 8. 不在本次范围内

- 自动登录、扫码识别或自动完成安全验证。
- 浏览器指纹伪造、反检测插件或代理池。
- BOSS Cookie/token 导出与注入。
- BOSS 非公开接口调用。
- 自动翻页、批量投递、自动聊天或简历发送。
- 新增课程项目管理后台或新的 Java 微服务。
