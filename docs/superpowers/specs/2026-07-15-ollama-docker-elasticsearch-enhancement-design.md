# AI 求职雷达 Ollama、Docker 与 Elasticsearch 增强设计

## 1. 目标与范围

在不改变六个业务功能模块、不升级课程规定核心框架版本的前提下，为现有 AI 求职雷达增加可真实演示的本地大模型能力、职位全文检索能力和一键基础设施环境。

本次增强必须满足以下目标：

- Spring Boot 保持 `2.3.12.RELEASE`，Spring Cloud 保持 `Hoxton.SR12`，Spring Cloud Alibaba 保持 `2.2.9.RELEASE`。
- 使用本机 Ollama `0.24.0`，聊天模型使用已安装的 `qwen3:8b`，向量模型使用已安装的 `qwen3-embedding:4b`。
- 智能匹配在现有规则分基础上增加语义分和大模型解释，不让大模型直接、不可解释地决定全部分数。
- 模拟面试使用大模型动态出题和评价答案，同时保留现有固定四题和关键词评分作为降级路径。
- Elasticsearch 只用于职位搜索索引；MySQL 仍是业务主数据源，Elasticsearch 不可用时自动回退 MySQL。
- Docker Compose 只编排 MySQL、Nacos、Redis、RabbitMQ、Elasticsearch。Java 服务仍在 IDEA 中启动，前端仍用 Node.js/Vite 启动。
- Ollama、Elasticsearch 或 Docker 任一不可用时，核心项目仍能启动和演示。

不在本次范围内：AI 聊天机器人、简历上传、自动投递、全量 RAG 知识库、所有业务服务容器化、升级 Spring Boot、引入 Spring AI。

## 2. 方案选择

### 2.1 采用的方案

新增公共 `ai-service` 微服务，在 Nacos 注册，监听 `9007` 端口。`match-service` 和 `interview-service` 使用 OpenFeign 调用 `ai-service`；`ai-service` 使用 Spring Web 的 HTTP 客户端调用本机 Ollama `http://127.0.0.1:11434`。

不引入当前版本 Spring AI。原因是项目必须保持 Spring Boot 2.3.12，直接调用 Ollama HTTP API可以避免依赖代际冲突，也更贴近课程已经学习的 Controller、Service、OpenFeign 和配置文件结构。

### 2.2 未采用的方案

- 直接在 `match-service`、`interview-service` 各自调用 Ollama：代码重复，提示词、超时、JSON 解析和降级策略分散。
- 让大模型直接给最终匹配分：结果波动大、难测试、难向教师解释。
- 全服务 Docker 化：不利于在 IDEA 中展示代码和启动过程，现场排错成本高。
- 只连接 Elasticsearch 不承担业务：属于为技术而技术，不能形成可说明的业务价值。

## 3. 总体架构

```text
Vue3
  |
Gateway :9000
  |-- user-service :9001
  |-- job-service :9002 -------- Elasticsearch :9200
  |-- match-service :9003 ------- OpenFeign ---- ai-service :9007
  |-- application-service :9004                     |
  |-- interview-service :9005 ---- OpenFeign -------|
  |-- notification-service :9006                    |
                                                    Ollama :11434
                                                    |-- qwen3:8b
                                                    `-- qwen3-embedding:4b

MySQL：业务主数据
Redis：职位缓存及可重复 AI 请求缓存
RabbitMQ：投递和状态变化异步通知
Nacos：Gateway、六个业务服务和 ai-service 的注册发现
```

六个业务功能模块保持不变。`ai-service` 是公共能力服务，不作为第七个业务分工模块。

## 4. ai-service 设计

### 4.1 配置

```yaml
server:
  port: 9007

ai:
  enabled: ${AI_ENABLED:true}
  ollama:
    base-url: ${OLLAMA_BASE_URL:http://127.0.0.1:11434}
    chat-model: ${OLLAMA_CHAT_MODEL:qwen3:8b}
    embedding-model: ${OLLAMA_EMBEDDING_MODEL:qwen3-embedding:4b}
    timeout-seconds: ${AI_TIMEOUT_SECONDS:15}
```

Ollama 请求统一使用：

- `stream: false`
- `think: false`
- `format: "json"`
- temperature `0.2`
- 最大输出长度受控

### 4.2 内部接口

`GET /internal/ai/health`

- 检查 Ollama 是否可访问以及两个模型是否存在。

`POST /internal/ai/embeddings`

- 输入用户画像文本和岗位文本。
- 一次调用 Ollama `/api/embed` 返回两个向量。
- 在 Java 中计算余弦相似度并转换为 `0～100` 的语义匹配分。

`POST /internal/ai/match-explanation`

- 输入用户画像、职位要求、规则分、语义分、已匹配技能、缺失技能。
- 输出 `summary`、`strengths`、`gaps`、`suggestions`。

`POST /internal/ai/interview-questions`

- 输入岗位名称、岗位描述、岗位技能。
- 固定输出四道题，每题包括 `order`、`question`、`referenceKeywords`。

`POST /internal/ai/interview-evaluation`

- 输入岗位、问题、参考关键词和用户答案。
- 输出 `score`、`strengths`、`weaknesses`、`suggestion`。

所有接口使用明确 DTO，不向调用方暴露 Ollama 原始响应。

### 4.3 错误处理

以下情况均返回“AI 未使用”的可识别结果，而不是拖垮业务服务：

- Ollama 未启动。
- 模型不存在。
- 请求超过 15 秒。
- 返回内容不是合法 JSON。
- 必填字段缺失或分数越界。

日志只记录错误类型、模型名和耗时，不记录用户完整回答。重复输入可按请求摘要在 Redis 中短期缓存。

## 5. 智能匹配增强

### 5.1 评分公式

现有规则分保持不变：

```text
规则分 = 技能 60 + 城市 15 + 薪资 15 + 经验 10
```

AI 可用时：

```text
最终分 = round(规则分 × 0.70 + 语义分 × 0.30)
```

语义分由用户画像文本和岗位文本的 embedding 余弦相似度计算，模型不直接生成分数。AI 只负责生成自然语言解释。

AI 不可用时：

```text
最终分 = 规则分
aiUsed = false
```

### 5.2 数据字段

`match_result` 增加：

- `rule_score`
- `semantic_score`，允许为空
- `ai_used`
- `strengths`
- `gaps`
- `suggestions`

原 `score` 字段继续表示页面展示的最终分，兼容原接口。

### 5.3 前端展示

匹配卡片增加但不新增页面：

- 综合匹配分
- 规则分
- AI 语义分（AI 可用时）
- “本地 AI 增强”或“规则模式”状态
- 优势、差距和建议

## 6. 模拟面试增强

### 6.1 创建面试

1. `interview-service` 通过 OpenFeign 获取岗位。
2. 调用 `ai-service` 生成四道结构化问题。
3. 校验必须正好四题、顺序为 1～4、文本不为空。
4. 校验成功后保存 AI 问题。
5. 调用失败时使用现有 `createQuestions` 固定四题。

### 6.2 提交答案

1. 调用 `ai-service` 获取结构化评价。
2. 校验分数在 `0～100`。
3. 保存得分、优点、问题、建议及 `aiUsed`。
4. AI 失败时使用现有关键词评分和固定反馈。

`interview_answer` 增加：

- `strengths`
- `weaknesses`
- `suggestion`
- `ai_used`

原 `feedback` 字段保留，用于兼容原前端和降级结果。

### 6.3 前端展示

提交答案后的反馈区显示：

- 得分
- 回答亮点
- 可以改进的地方
- 推荐表达方式
- 本次评价使用“本地 AI”还是“规则评分”

## 7. Elasticsearch 职位检索

### 7.1 版本与依赖

项目使用 Spring Boot 2.3.12 管理的 Elasticsearch `7.6.2`，不单独引入最新版客户端。`job-service` 增加 `spring-boot-starter-data-elasticsearch`。

### 7.2 数据职责

- MySQL 是职位真实数据源。
- Elasticsearch 保存职位搜索文档。
- Redis 继续缓存搜索结果和职位详情。

职位索引字段：

- `id`
- `title`
- `company`
- `city`
- `description`
- `requirements`
- `salaryMin`
- `salaryMax`
- `experienceYears`
- `education`
- `tags`

### 7.3 同步与降级

- `job-service` 启动后从 MySQL 重建演示数据索引。
- 搜索优先使用 Elasticsearch 的多字段全文搜索和过滤。
- Elasticsearch 连接、查询或解析失败时，记录警告并调用现有 MyBatis Plus 查询。
- 无关键词时仍可直接走现有 MySQL 分页，避免不必要的 Elasticsearch 请求。

不新增管理后台和职位写入接口，因此本阶段不实现复杂双写事务。

## 8. Docker Compose

根目录新增 `docker-compose.yml`，编排：

- MySQL 8
- Nacos 2.1.0 standalone
- Redis
- RabbitMQ management
- Elasticsearch 7.6.2 single-node

Elasticsearch JVM 内存限制为 `-Xms512m -Xmx512m`，避免与本地 Ollama 争抢内存。Ollama 保持 Windows 本机运行，不放入 Docker，以继续使用已安装模型和 RTX 4060 GPU。

Docker Compose 提供健康检查、固定端口和数据卷。Java 服务不制作镜像，仍在 IDEA 中启动。

## 9. 演示与启动顺序

```text
1. docker compose up -d
2. ollama list，确认 qwen3:8b 与 qwen3-embedding:4b
3. IDEA 启动 Gateway、六个业务服务和 ai-service
4. npm run dev
5. 演示 Elasticsearch 搜索
6. 演示 AI 匹配解释
7. 演示 AI 动态出题与答案评价
8. 停止 Ollama 后再次请求，展示规则降级仍可工作
```

环境检查脚本增加 Docker、Elasticsearch、Ollama 和模型检查，但不把它们全部设为阻断条件。

## 10. 测试策略

### ai-service

- Ollama 正常 JSON 响应解析。
- embedding 余弦相似度边界。
- 超时、连接失败、非法 JSON、字段缺失。
- `AI_ENABLED=false`。

### match-service

- `70% 规则分 + 30% 语义分`。
- AI 不可用时分数与旧算法一致。
- AI 解释字段保存和返回。

### interview-service

- AI 正好生成四题。
- AI 题目校验失败时使用固定四题。
- AI 评价保存结构化反馈。
- AI 失败时关键词评分保持不变。

### job-service

- Elasticsearch 查询成功。
- Elasticsearch 失败回退 MyBatis Plus。
- 无关键词搜索保持现有行为。

### 全链路

- 全量 Maven 测试。
- Vue 单元测试和构建。
- Ollama 开启与关闭两种状态的接口验证。
- Elasticsearch 开启与关闭两种状态的职位搜索验证。

## 11. 验收标准

- 原有六个业务页面和全部原功能继续可用。
- Nacos 可看到 `ai-service`。
- 匹配页能区分规则分、语义分和最终分。
- 模拟面试能够生成四道岗位相关问题并给出结构化评价。
- Ollama 关闭后匹配和面试仍走原规则逻辑。
- 职位关键词搜索优先命中 Elasticsearch，关闭 Elasticsearch 后仍可查询。
- Docker Compose 能启动五个基础设施服务。
- IDEA 能直接启动所有 Java 服务。
- 不引入 Spring AI，不升级课程核心框架版本。
