# AI 求职雷达（课程框架重制版）

面向高校求职者的一站式求职辅助系统，覆盖职位检索、可解释匹配、收藏投递、进度管理、模拟面试和异步通知。项目严格保留课程技术版本，并在原业务上增加可关闭、可降级的本地大模型与 Elasticsearch 能力。

## 技术路线

| 层级 | 技术与版本 |
| --- | --- |
| 前端 | Vue 3.5.38、Vue Router 4.6.4、Axios 1.18.1、Vite 8.0.16、Node.js 24 |
| 微服务 | Spring Boot 2.3.12.RELEASE、Spring Cloud Hoxton.SR12、Spring Cloud Alibaba 2.2.9.RELEASE |
| 服务治理 | Nacos 2.1.0、Gateway、OpenFeign |
| 数据访问 | MyBatis Plus 3.3.1、MySQL 8、Redis |
| 消息队列 | RabbitMQ |
| 本地 AI | Ollama、qwen3:8b、qwen3-embedding:4b |
| 选做技术 | Elasticsearch 7.6.2、Docker Compose |
| 可选数据工具 | Playwright 1.49.1（Node.js 独立进程） |

Spring AI 未被引入，因为当前课程基线是 Spring Boot 2.3.12；项目使用课程中已有的 `RestTemplate` 风格直接调用 Ollama HTTP API，避免为了 AI 破坏课程依赖版本。

## 服务架构

```text
Vue :5173/5174 -> Gateway :9000
                     ├─ user-service :9001
                     ├─ job-service :9002 -> Redis / MySQL / Elasticsearch（失败回 MySQL）
                     ├─ match-service :9003 ─┐
                     ├─ application-service :9004 -> RabbitMQ
                     ├─ interview-service :9005 ─┤ OpenFeign -> ai-service :9007 -> Ollama :11434
                     └─ notification-service :9006

全部 Java 服务注册到 Nacos :8848
```

匹配最终分数为 `规则分 × 70% + 语义分 × 30%`。Ollama 不可用时最终分数完全等于原规则分；模拟面试同样保留原四题模板和关键词评分。

## 推荐展示方式：IDEA 启动 Java

1. IDEA 打开 `ai-job-radar-parent/pom.xml`，等待 Maven 导入完成。
2. 启动 Nacos 与 RabbitMQ，并确认 MySQL、Redis、Ollama 状态：

```powershell
.\scripts\start-nacos.ps1
.\scripts\start-rabbitmq.ps1
.\scripts\check-environment.ps1
```

3. 在 IDEA 依次运行以下启动类：

```text
UserApplication             9001
JobApplication              9002
AiApplication               9007
MatchApplication            9003
ApplicationApplication      9004
InterviewApplication        9005
NotificationApplication     9006
GatewayApplication          9000
```

4. 新终端启动前端（固定使用 5174 端口）：

```powershell
.\scripts\start-frontend.ps1
```

5. 打开 `http://127.0.0.1:5174`，演示账号：`student / 123456`。

服务全部就绪后，可在新终端执行一条冒烟测试，确认职位、匹配和四题面试链路：

```powershell
node .\scripts\smoke-test.mjs
```

## Ollama 本地大模型

本机已准备推荐模型时，直接启动 Ollama 即可：

```powershell
ollama serve
ollama list
```

默认环境变量：

```text
AI_ENABLED=true
OLLAMA_BASE_URL=http://127.0.0.1:11434
OLLAMA_CHAT_MODEL=qwen3:8b
OLLAMA_EMBEDDING_MODEL=qwen3-embedding:4b
AI_TIMEOUT_SECONDS=15
```

答辩现场如需保证响应速度，可先执行一次模型预热：

```powershell
ollama run qwen3:8b "只回答：模型预热完成"
```

如模型临时不可用，系统自动显示“规则引擎兜底”，职位、匹配与面试主流程仍可展示。

## Docker 与 Elasticsearch（选做）

当前设计只用 Docker 启动基础设施，Java 服务仍由 IDEA 运行，最贴合课堂演示。安装并启动 Docker Desktop 后执行：

```powershell
.\scripts\start-infrastructure.ps1
docker compose ps
```

Compose 包含 MySQL 8.0.26、Nacos 2.1.0、Redis 6.2、RabbitMQ 3.12 和 Elasticsearch 7.6.2。Ollama 使用 Windows 本机 GPU，不放入容器。

当前数据库若由旧版 SQL 初始化，只需执行一次：

```powershell
mysql --default-character-set=utf8mb4 -uroot -proot < sql/upgrade-ai-search.sql
```

## BOSS 普通 Chrome 岗位导入（可选）

`boss-chrome-extension` 和 `job-importer` 本地桥接是 Node.js 辅助工具，不属于新增微服务；停止它们不影响职位检索、匹配、收藏、投递和面试。扩展只读取用户在普通 Chrome 当前 BOSS 搜索页中已经看见并加载完成的岗位卡片，单次最多 50 条，再由仅监听 `127.0.0.1:9011` 的桥接程序通过 Gateway 和 JWT 调用 `job-service`。

MySQL 按 `source + external_id` 去重，同一岗位再次导入会更新而不会重复新增，同时清理 Redis 职位缓存并尽力同步 Elasticsearch。导入字段包括岗位名称、公司、城市、薪资、经验、学历、技能/福利标签、来源链接和导入时间，职位卡片会显示 `BOSS` 来源标记。

### 首次准备

安装固定版本依赖，并对旧数据库执行一次迁移：

```powershell
npm --prefix job-importer install
Get-Content .\sql\upgrade-boss-import.sql -Raw | mysql --default-character-set=utf8mb4 -uroot -proot ai_job_radar
```

打开 `chrome://extensions`，开启右上角“开发者模式”，点击“加载已解压的扩展程序”，选择项目中的 `boss-chrome-extension` 目录。安装后建议把“AI 求职雷达 BOSS 导入器”固定到浏览器工具栏。

### 每次真实导入

1. 确认 Gateway 已在 `9000` 端口运行，在新 PowerShell 终端设置课程系统账号并启动桥接，保持该终端开启：

```powershell
$env:API_BASE_URL = 'http://127.0.0.1:9000'
$env:RADAR_USERNAME = 'student'
$env:RADAR_PASSWORD = '123456'
.\scripts\start-boss-bridge.ps1
```

2. 用日常 Chrome 正常打开 BOSS 并自行登录，进入岗位搜索结果页；按需要向下滚动，让浏览器加载更多岗位。
3. 点击扩展，先点“检测当前页面”，确认数量后点“导入当前页面”。扩展不会自动翻页，所以页面实际加载多少就导入多少，最多 50 条。
4. 刷新 `http://127.0.0.1:5174/jobs`，即可看到带 `BOSS` 标记的岗位；重复导入相同岗位时会更新原记录。

此流程不读取或导出 BOSS Cookie、令牌和登录凭据，不自动登录、翻页、聊天或投递，不调用非公开接口，也不绕过验证码、安全验证或访问限制。课程系统账号只用于本机 Gateway 登录，桥接不会在响应或日志中返回密码。

### 离线展示备用方案

BOSS 外部站点临时不可访问时，可用本地 fixture 演示同一后端导入、去重、缓存清理和搜索链路：

```powershell
.\scripts\start-job-importer.ps1 --keyword Java --city 威海 --limit 2 --fixture job-importer/test/fixtures/boss-results.html
```

fixture 只用于稳定答辩，不应称为实时 BOSS 数据。

## 自动化验证

```powershell
mvn -f ai-job-radar-parent/pom.xml clean test
npm --prefix ai-job-radar-web test
npm --prefix ai-job-radar-web run build
npm --prefix job-importer test
```

核心演示顺序建议：登录与画像 → Elasticsearch 职位检索 → 本地 AI 匹配报告 → 收藏与投递 → RabbitMQ 通知 → Ollama 四题面试与结构化反馈 → 关闭增强能力展示自动降级。
