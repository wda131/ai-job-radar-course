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

## BOSS 岗位受控导入（可选）

`job-importer` 是独立的 Node.js 辅助工具，不属于新增微服务；它停止时不影响职位检索、匹配、收藏、投递和面试。工具只读取用户已经在可见 Chromium 页面中打开的岗位卡片，每次最多 20 条，然后通过 Gateway 和 JWT 调用 `job-service`。MySQL 按 `source + external_id` 去重，同一岗位再次导入会更新而不会重复新增，同时清理 Redis 职位缓存并尽力同步 Elasticsearch。

首次使用安装固定版本依赖与 Chromium：

```powershell
npm --prefix job-importer install
npx --prefix job-importer playwright install chromium
```

当前数据库只需迁移一次：

```powershell
Get-Content .\sql\upgrade-boss-import.sql -Raw | mysql --default-character-set=utf8mb4 -uroot -proot ai_job_radar
```

设置课程系统登录信息。脚本只把它们用于本机 Gateway 登录，不会打印账号、密码、JWT、Cookie 或浏览器存储：

```powershell
$env:API_BASE_URL = 'http://127.0.0.1:9000'
$env:RADAR_USERNAME = 'student'
$env:RADAR_PASSWORD = '123456'
```

答辩时推荐先用本地 fixture 稳定演示完整导入链路：

```powershell
.\scripts\start-job-importer.ps1 --keyword Java --city 威海 --limit 2 --fixture job-importer/test/fixtures/boss-results.html
```

需要检查真实站点时运行：

```powershell
.\scripts\start-job-importer.ps1 --keyword Java --city 威海 --limit 2
```

程序会打开可见浏览器并保留本机 `job-importer/.browser-data` 登录状态，用户自行扫码或登录。也可以传入本人有权访问的搜索结果地址：`--url 'https://www.zhipin.com/...'`。工具不绕过验证码、安全验证或访问限制，不使用隐身插件、代理，也不自动沟通或投递；检测到限制时会主动停止。页面结构变化时 fixture 自动化测试仍可用于课程展示，真实页面解析器则需要按当时公开页面结构维护。

导入字段包括岗位名称、公司、城市、薪资、经验、学历、技能/福利标签、来源链接和导入时间。职位卡片会显示 `BOSS` 或 `本地数据` 来源标记。

## 自动化验证

```powershell
mvn -f ai-job-radar-parent/pom.xml clean test
npm --prefix ai-job-radar-web test
npm --prefix ai-job-radar-web run build
npm --prefix job-importer test
```

核心演示顺序建议：登录与画像 → Elasticsearch 职位检索 → 本地 AI 匹配报告 → 收藏与投递 → RabbitMQ 通知 → Ollama 四题面试与结构化反馈 → 关闭增强能力展示自动降级。
