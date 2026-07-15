# AI 求职雷达：同学从零运行指南

这份说明面向第一次拿到项目的同学，默认使用 Windows 10/11、PowerShell 和 IDEA。请从仓库根目录执行本文命令。

> Chrome 扩展只用于导入真实 BOSS 岗位，不是启动主体项目的必需条件。先完成登录、职位、匹配、收藏投递和模拟面试，再按需安装扩展。

## 一、推荐方案

推荐使用以下组合，最少出现环境差异：

- Docker Desktop：运行 MySQL、Redis、Nacos、RabbitMQ 和 Elasticsearch；
- IDEA：运行八个 Java 进程；
- Node.js：运行 Vue3 前端；
- Ollama：可选，本地大模型不可用时系统自动使用规则引擎；
- 普通 Chrome 扩展：可选，仅用于手动导入真实 BOSS 岗位。

## 二、准备软件

| 软件 | 推荐版本 | 是否必需 | 用途 |
| --- | --- | --- | --- |
| Git | 当前稳定版 | 是 | 克隆项目 |
| JDK | 17 | 是 | IDEA 和 Java 微服务；项目源码兼容级别为 Java 8 |
| Maven | 3.8 或更高 | 是 | 下载并构建后端依赖 |
| Node.js | 24 | 是 | Vue3 前端和可选岗位导入桥接 |
| IntelliJ IDEA | 2023 或更高 | 是 | 课程要求的 Java 启动方式 |
| Docker Desktop | 当前稳定版 | 推荐 | 一次启动全部基础设施 |
| Ollama | 当前稳定版 | 否 | 本地大模型增强 |
| Google Chrome | 当前稳定版 | 否 | 安装 BOSS 岗位导入扩展 |

先检查命令：

```powershell
git --version
java -version
mvn -version
node -v
npm -v
docker version
```

如果选择无 Docker 路线，`docker version` 不存在是正常的。

## 三、克隆项目

```powershell
git clone https://github.com/wda131/ai-job-radar-course.git
cd ai-job-radar-course
```

后续命令都在这个目录执行，不要进入某个微服务目录后再运行根目录脚本。

## 四、启动基础设施

### 方案 A：Docker Desktop（推荐）

1. 启动 Docker Desktop，等待界面显示 Docker Engine 已运行。
2. 在仓库根目录执行：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\start-infrastructure.ps1
docker compose ps
```

首次启动需要下载镜像，时间取决于网络。等待 MySQL、Nacos、Redis、RabbitMQ 和 Elasticsearch 状态变成 `running` 或 `healthy`。

3. 检查课程必需端口：

```powershell
.\scripts\check-environment.ps1
```

看到 MySQL、RabbitMQ、Redis、Nacos 均为 `[READY]` 才继续。

新建的 MySQL Docker 数据卷会自动执行 `sql/ai_job_radar.sql`。如果以前启动过同名数据卷，但登录时提示表不存在，可以手动重新初始化：

```powershell
Get-Content .\sql\ai_job_radar.sql -Raw |
  docker exec -i radar-mysql mysql --default-character-set=utf8mb4 -uroot -proot
```

注意：该 SQL 会重建本项目的数据表并恢复演示数据，不要对需要保留的数据执行。

### 方案 B：不用 Docker

需要自行安装并启动：

- MySQL 8，端口 `3306`，root 密码建议设为 `root`；
- Redis 6.2，端口 `6379`；
- Nacos 2.1.0，端口 `8848`；
- RabbitMQ，端口 `5672`，默认账号 `guest / guest`；
- Elasticsearch 7.6.2 可不安装，职位服务会自动回退到 MySQL。

初始化数据库：

```powershell
Get-Content .\sql\ai_job_radar.sql -Raw |
  mysql --default-character-set=utf8mb4 -uroot -proot
```

启动 Nacos 前配置其解压目录：

```powershell
$env:NACOS_HOME = 'D:\Applications\nacos-2.1.0\nacos'
.\scripts\start-nacos.ps1
```

启动 RabbitMQ 前配置 Erlang 和 RabbitMQ 目录：

```powershell
$env:ERLANG_HOME = 'C:\Program Files\Erlang OTP'
$env:RABBITMQ_SERVER = 'D:\Applications\rabbitmq_server'
.\scripts\start-rabbitmq.ps1
```

这些路径只是格式示例，必须换成同学电脑上的真实安装目录。MySQL 和 Redis 需由各自服务或 Windows 服务管理器启动。

最后执行：

```powershell
.\scripts\check-environment.ps1
```

## 五、在 IDEA 启动后端

1. IDEA 选择 `File -> Open`，打开 `ai-job-radar-parent/pom.xml`。
2. 将 Project SDK 和 Maven Runner JRE 设置为 JDK 17。
3. 等待 Maven 依赖下载结束；右侧 Maven 面板不能有红色依赖。
4. 在 IDEA 中依次运行：

| 顺序 | 启动类 | 端口 |
| --- | --- | --- |
| 1 | `UserApplication` | 9001 |
| 2 | `JobApplication` | 9002 |
| 3 | `AiApplication` | 9007 |
| 4 | `MatchApplication` | 9003 |
| 5 | `ApplicationServiceApplication` | 9004 |
| 6 | `InterviewApplication` | 9005 |
| 7 | `NotificationApplication` | 9006 |
| 8 | `GatewayApplication` | 9000 |

每个控制台看到 `Started ...Application` 后再启动下一个。全部服务会注册到 Nacos：

```text
http://127.0.0.1:8848/nacos
```

如果不使用 IDEA，也可先构建 JAR，再使用脚本启动：

```powershell
mvn -f .\ai-job-radar-parent\pom.xml clean package -DskipTests
.\scripts\start-backend.ps1
```

课程展示推荐仍使用 IDEA，便于老师查看微服务启动类和控制台。

## 六、启动前端

新开一个 PowerShell，在仓库根目录执行：

```powershell
.\scripts\start-frontend.ps1
```

脚本首次运行会自动执行 `npm install`，随后前端固定启动在：

```text
http://127.0.0.1:5174
```

演示账号：

```text
用户名：student
密码：123456
```

不要随意改成其他前端端口；Gateway 的课程 CORS 配置已允许 `5173` 和 `5174`，展示脚本固定使用 `5174`。

## 七、验证主体项目

登录页面能打开后，在仓库根目录执行：

```powershell
node .\scripts\smoke-test.mjs
```

成功输出应包含：

- `gateway: "ok"`；
- `jobs` 大于 0；
- `questionCount: 4`；
- 匹配分数和面试回答分数。

然后人工检查：

1. 岗位页能看到岗位与薪资；
2. 收藏后按钮状态改变；
3. 投递后能在求职进度中看到记录；
4. 模拟面试能切换并查看四道题；
5. 提交答案后能看到评分、优点、问题和优化建议；
6. 消息中心能看到投递状态通知。

## 八、可选：启用 Ollama 本地大模型

```powershell
ollama pull qwen3:8b
ollama pull qwen3-embedding:4b
ollama serve
```

可用下面命令预热：

```powershell
ollama run qwen3:8b '只回答：模型预热完成'
```

Ollama 默认地址为 `http://127.0.0.1:11434`。没有安装或模型超时时，系统会显示“规则引擎兜底”，主体功能仍能运行。

## 九、可选：安装 Chrome 扩展并导入真实 BOSS 岗位

### 1. 安装导入器依赖

```powershell
npm --prefix .\job-importer install
```

### 2. 加载扩展

1. 使用普通 Google Chrome 打开 `chrome://extensions`；
2. 开启右上角“开发者模式”；
3. 点击“加载已解压的扩展程序”；
4. 选择仓库中的 `boss-chrome-extension` 文件夹；
5. 将“AI 求职雷达 BOSS 导入器”固定到工具栏。

项目更新后，如果扩展代码发生变化，在 `chrome://extensions` 点击该扩展的“重新加载”。

### 3. 启动本地桥接

先确认 Gateway 端口 `9000` 已启动，再新开 PowerShell：

```powershell
$env:API_BASE_URL = 'http://127.0.0.1:9000'
$env:RADAR_USERNAME = 'student'
$env:RADAR_PASSWORD = '123456'
.\scripts\start-boss-bridge.ps1
```

保持这个终端开启。出现以下提示表示桥接成功：

```text
BOSS 导入桥接已启动：http://127.0.0.1:9011
```

### 4. 自己控制导入

1. 在普通 Chrome 中正常登录 BOSS；
2. 打开岗位搜索结果页，选择关键词、城市等条件；
3. 向下滚动，让页面加载需要的岗位；
4. 点击扩展，先点“检测当前页面”；
5. 确认数量后点“导入当前页面”；
6. 刷新 `http://127.0.0.1:5174/jobs`。

单次最多读取当前页面已加载的 50 个岗位。相同 `source + external_id` 会更新而不会重复新增。扩展不导出 BOSS Cookie、密码或令牌，不自动登录、翻页、聊天或投递，也不绕过验证码和安全验证。

## 十、常见问题

### `Port already in use`

```powershell
Get-NetTCPConnection -State Listen |
  Where-Object LocalPort -in 3306,5174,5672,6379,8848,9000,9001,9002,9003,9004,9005,9006,9007,9011
```

关闭重复启动的旧进程，不要同时在 IDEA 和 `start-backend.ps1` 启动同一批 Java 服务。

### 登录请求超时或返回 404

检查 `UserApplication:9001`、`GatewayApplication:9000` 和 Nacos 是否正常。Nacos 晚于微服务启动时，重启对应 Java 服务。

### 提示数据库连接失败

项目默认使用 `root / root` 和数据库 `ai_job_radar`。Docker 路线会自动配置；原生 MySQL 请确保密码一致并执行 `sql/ai_job_radar.sql`。

### 岗位列表为空

检查数据库是否已有 `jobs` 表及演示数据，随后重启 `JobApplication`。Elasticsearch 失败不会导致列表为空，服务会回退到 MySQL。

### 前端依赖安装失败

确认 `node -v` 为 Node.js 24，再删除前端目录中不完整的 `node_modules` 后重新执行：

```powershell
npm --prefix .\ai-job-radar-web install
.\scripts\start-frontend.ps1
```

### 扩展提示“本地桥接未连接”

确认 `start-boss-bridge.ps1` 的终端仍在运行，并访问：

```text
http://127.0.0.1:9011/health
```

### 扩展检测到 0 个岗位

确认当前是 BOSS 岗位搜索结果页而不是首页或岗位详情页；刷新页面、向下滚动后重新检测。如果刚更新过仓库，请在扩展管理页点击“重新加载”。

### AI 一直显示规则引擎兜底

这是允许的降级状态。若要启用大模型，检查 Ollama `11434` 端口和两个模型是否已经下载。

## 十一、端口速查

| 端口 | 组件 | 必需性 |
| --- | --- | --- |
| 3306 | MySQL | 必需 |
| 5672 / 15672 | RabbitMQ / 管理页 | 必需 |
| 6379 | Redis | 必需 |
| 8848 | Nacos | 必需 |
| 9000 | Gateway | 必需 |
| 9001—9007 | 七个业务/AI服务 | 必需 |
| 5174 | Vue3 前端 | 必需 |
| 9011 | BOSS 本地桥接 | 可选 |
| 11434 | Ollama | 可选 |
| 9200 | Elasticsearch | 可选 |

## 十二、发给同学前的检查清单

- [ ] GitHub 仓库能正常克隆；
- [ ] JDK、Maven、Node.js 版本检查通过；
- [ ] Docker 五个容器正常，或四个原生必需组件端口正常；
- [ ] SQL 已初始化，`student / 123456` 能登录；
- [ ] IDEA 八个启动类全部显示 `Started`；
- [ ] 前端能访问 `http://127.0.0.1:5174`；
- [ ] `node scripts/smoke-test.mjs` 通过；
- [ ] 需要真实岗位时再安装 Chrome 扩展并启动 `9011` 桥接；
- [ ] 不需要 BOSS 数据时，无需安装扩展和 Playwright。
