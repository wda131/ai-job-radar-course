# Local AI, Search, and Infrastructure Enhancement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a fault-tolerant Ollama-backed AI capability, Elasticsearch job search, and Docker Compose infrastructure without changing the course framework versions or breaking the existing six business modules.

**Architecture:** Add an `ai-service` registered in Nacos on port 9007. Match and interview services call it with OpenFeign and keep their current rule/template implementations as fallbacks. Job service uses Boot-managed Elasticsearch 7.6.2 for keyword search with MySQL fallback; Docker Compose runs infrastructure while IDEA runs Java services.

**Tech Stack:** Spring Boot 2.3.12, Spring Cloud Hoxton, Nacos, Gateway, OpenFeign, MyBatis Plus 3.3.1, Redis, RabbitMQ, Ollama HTTP API, qwen3:8b, qwen3-embedding:4b, Elasticsearch 7.6.2, Docker Compose, Vue 3, Node.js.

---

### Task 1: Add shared AI contracts and the ai-service module

**Files:**
- Modify: `ai-job-radar-parent/pom.xml`
- Create: `ai-job-radar-parent/common/src/main/java/cn/sdu/radar/ai/dto/AiEmbeddingRequest.java`
- Create: `ai-job-radar-parent/common/src/main/java/cn/sdu/radar/ai/dto/AiEmbeddingResponse.java`
- Create: `ai-job-radar-parent/common/src/main/java/cn/sdu/radar/ai/dto/AiMatchRequest.java`
- Create: `ai-job-radar-parent/common/src/main/java/cn/sdu/radar/ai/dto/AiMatchResponse.java`
- Create: `ai-job-radar-parent/common/src/main/java/cn/sdu/radar/ai/dto/AiQuestionRequest.java`
- Create: `ai-job-radar-parent/common/src/main/java/cn/sdu/radar/ai/dto/AiQuestionResponse.java`
- Create: `ai-job-radar-parent/common/src/main/java/cn/sdu/radar/ai/dto/AiEvaluationRequest.java`
- Create: `ai-job-radar-parent/common/src/main/java/cn/sdu/radar/ai/dto/AiEvaluationResponse.java`
- Create: `ai-job-radar-parent/ai-service/pom.xml`
- Create: `ai-job-radar-parent/ai-service/src/main/java/cn/sdu/radar/AiApplication.java`
- Create: `ai-job-radar-parent/ai-service/src/main/resources/bootstrap.yml`

- [ ] **Step 1: Add tests that compile DTOs and require ai-service configuration**

Create a context-free DTO test that verifies defaults such as `aiUsed=false`, four ordered questions, and bounded scores.

- [ ] **Step 2: Run the common and ai-service tests and verify failure**

Run: `mvn -f ai-job-radar-parent/pom.xml -pl common,ai-service -am test`

Expected: failure because `ai-service` and shared DTOs do not exist.

- [ ] **Step 3: Add the module and DTOs**

Add `<module>ai-service</module>` to the parent and create Lombok `@Data` DTOs. `AiMatchResponse` contains `available`, `semanticScore`, `summary`, `strengths`, `gaps`, and `suggestions`. `AiEvaluationResponse` contains `available`, `score`, `strengths`, `weaknesses`, and `suggestion`.

- [ ] **Step 4: Run the module tests**

Run: `mvn -f ai-job-radar-parent/pom.xml -pl common,ai-service -am test`

Expected: PASS.

- [ ] **Step 5: Commit**

Run: `git add ai-job-radar-parent && git commit -m "feat: add shared AI service module"`

### Task 2: Implement the Ollama client with validation and fallback responses

**Files:**
- Create: `ai-job-radar-parent/ai-service/src/main/java/cn/sdu/radar/config/AiProperties.java`
- Create: `ai-job-radar-parent/ai-service/src/main/java/cn/sdu/radar/ollama/OllamaClient.java`
- Create: `ai-job-radar-parent/ai-service/src/main/java/cn/sdu/radar/ollama/OllamaChatRequest.java`
- Create: `ai-job-radar-parent/ai-service/src/main/java/cn/sdu/radar/ollama/OllamaChatResponse.java`
- Create: `ai-job-radar-parent/ai-service/src/main/java/cn/sdu/radar/ollama/OllamaEmbedRequest.java`
- Create: `ai-job-radar-parent/ai-service/src/main/java/cn/sdu/radar/ollama/OllamaEmbedResponse.java`
- Create: `ai-job-radar-parent/ai-service/src/main/java/cn/sdu/radar/service/AiService.java`
- Create: `ai-job-radar-parent/ai-service/src/main/java/cn/sdu/radar/service/impl/AiServiceImpl.java`
- Create: `ai-job-radar-parent/ai-service/src/main/java/cn/sdu/radar/controller/AiController.java`
- Test: `ai-job-radar-parent/ai-service/src/test/java/cn/sdu/radar/service/AiServiceImplTest.java`

- [ ] **Step 1: Write failing tests**

Cover valid chat JSON, invalid JSON, connection exception, disabled AI, embedding cosine similarity, score clamping, and exactly four questions.

- [ ] **Step 2: Verify failure**

Run: `mvn -f ai-job-radar-parent/pom.xml -pl ai-service -am test`

Expected: FAIL because `AiServiceImpl` does not exist.

- [ ] **Step 3: Implement minimal Ollama integration**

Use `RestTemplateBuilder.setConnectTimeout` and `setReadTimeout`. Send `stream=false`, `think=false`, `format="json"`, and temperature `0.2`. Catch HTTP, timeout, and Jackson parsing errors and return `available=false`.

- [ ] **Step 4: Verify tests**

Run: `mvn -f ai-job-radar-parent/pom.xml -pl ai-service -am test`

Expected: PASS.

- [ ] **Step 5: Commit**

Run: `git add ai-job-radar-parent/ai-service && git commit -m "feat: integrate local Ollama service"`

### Task 3: Enhance matching while preserving rule fallback

**Files:**
- Create: `ai-job-radar-parent/match-service/src/main/java/cn/sdu/radar/feign/AiClient.java`
- Modify: `ai-job-radar-parent/match-service/src/main/java/cn/sdu/radar/service/impl/MatchServiceImpl.java`
- Modify: `ai-job-radar-parent/match-service/src/main/java/cn/sdu/radar/pojo/MatchResult.java`
- Modify: `ai-job-radar-parent/match-service/src/main/java/cn/sdu/radar/pojo/vo/MatchReportVO.java`
- Modify: `ai-job-radar-parent/match-service/src/test/java/cn/sdu/radar/service/MatchServiceImplTest.java`
- Modify: `sql/ai_job_radar.sql`

- [ ] **Step 1: Write failing tests for blended and fallback scores**

Verify `round(ruleScore * 0.70 + semanticScore * 0.30)` when AI is available and exact legacy `ruleScore` when it is unavailable.

- [ ] **Step 2: Verify failure**

Run: `mvn -f ai-job-radar-parent/pom.xml -pl match-service -am test`

Expected: FAIL because `AiClient` and new fields do not exist.

- [ ] **Step 3: Implement OpenFeign integration and persistence fields**

Add `ruleScore`, `semanticScore`, `aiUsed`, `strengths`, `gaps`, and `suggestions`; retain `score` and `summary` for compatibility. Catch Feign exceptions and continue with the rule result.

- [ ] **Step 4: Verify tests**

Run: `mvn -f ai-job-radar-parent/pom.xml -pl match-service -am test`

Expected: PASS.

- [ ] **Step 5: Commit**

Run: `git add ai-job-radar-parent/match-service sql/ai_job_radar.sql && git commit -m "feat: add explainable AI matching"`

### Task 4: Enhance interview generation and evaluation with fallback

**Files:**
- Create: `ai-job-radar-parent/interview-service/src/main/java/cn/sdu/radar/feign/AiClient.java`
- Modify: `ai-job-radar-parent/interview-service/src/main/java/cn/sdu/radar/service/impl/InterviewServiceImpl.java`
- Modify: `ai-job-radar-parent/interview-service/src/main/java/cn/sdu/radar/pojo/InterviewAnswer.java`
- Modify: `ai-job-radar-parent/interview-service/src/main/java/cn/sdu/radar/pojo/vo/InterviewAnswerVO.java`
- Modify: `ai-job-radar-parent/interview-service/src/test/java/cn/sdu/radar/service/InterviewServiceImplTest.java`
- Modify: `sql/ai_job_radar.sql`

- [ ] **Step 1: Write failing tests**

Cover four valid AI questions, invalid question count fallback, AI evaluation persistence, invalid score fallback, and Feign failure fallback.

- [ ] **Step 2: Verify failure**

Run: `mvn -f ai-job-radar-parent/pom.xml -pl interview-service -am test`

Expected: FAIL because AI integration fields do not exist.

- [ ] **Step 3: Implement AI-first/fallback-second workflow**

Keep `createQuestions`, `calculateScore`, and `feedback` as fallback methods. Only accept AI output after validating four ordered questions and a score from 0 through 100.

- [ ] **Step 4: Verify tests**

Run: `mvn -f ai-job-radar-parent/pom.xml -pl interview-service -am test`

Expected: PASS.

- [ ] **Step 5: Commit**

Run: `git add ai-job-radar-parent/interview-service sql/ai_job_radar.sql && git commit -m "feat: add local AI interview feedback"`

### Task 5: Add Elasticsearch job search with MySQL fallback

**Files:**
- Modify: `ai-job-radar-parent/job-service/pom.xml`
- Modify: `ai-job-radar-parent/job-service/src/main/resources/bootstrap.yml`
- Create: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/search/JobDocument.java`
- Create: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/search/JobSearchRepository.java`
- Create: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/search/JobSearchService.java`
- Create: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/search/JobIndexInitializer.java`
- Modify: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/service/impl/JobServiceImpl.java`
- Modify: `ai-job-radar-parent/job-service/src/test/java/cn/sdu/radar/service/JobServiceImplTest.java`

- [ ] **Step 1: Write failing tests for ES success and fallback**
- [ ] **Step 2: Run `mvn -f ai-job-radar-parent/pom.xml -pl job-service -am test` and verify failure**
- [ ] **Step 3: Add Boot-managed `spring-boot-starter-data-elasticsearch` and implement full-text search**
- [ ] **Step 4: Catch all ES availability exceptions and invoke the existing MyBatis query**
- [ ] **Step 5: Run the job-service tests and verify PASS**
- [ ] **Step 6: Commit with `git commit -m "feat: add Elasticsearch job search fallback"`**

### Task 6: Update the Vue presentation without adding a new page

**Files:**
- Modify: `ai-job-radar-web/src/views/MatchesView.vue`
- Modify: `ai-job-radar-web/src/views/InterviewsView.vue`
- Modify: `ai-job-radar-web/src/assets/pages.css`
- Modify: `ai-job-radar-web/test/ui-state.test.js`

- [ ] **Step 1: Add failing state-formatting tests for AI and fallback badges**
- [ ] **Step 2: Run `npm --prefix ai-job-radar-web test` and verify failure**
- [ ] **Step 3: Show rule score, semantic score, AI status, strengths, gaps, and interview suggestions**
- [ ] **Step 4: Run tests and `npm --prefix ai-job-radar-web run build`**
- [ ] **Step 5: Commit with `git commit -m "feat: show local AI matching and interview evidence"`**

### Task 7: Add Docker Compose and environment checks

**Files:**
- Create: `docker-compose.yml`
- Modify: `scripts/check-environment.ps1`
- Create: `scripts/start-infrastructure.ps1`
- Modify: `scripts/start-backend.ps1`
- Modify: `README.md`

- [ ] **Step 1: Add a config validation command**

Run: `docker compose config`

Expected before implementation: failure because the file does not exist.

- [ ] **Step 2: Add MySQL, Nacos, Redis, RabbitMQ, and ES 7.6.2 services**

Use health checks and `ES_JAVA_OPTS=-Xms512m -Xmx512m`. Do not containerize Ollama or Java services.

- [ ] **Step 3: Validate Compose**

Run: `docker compose config`

Expected: exit 0.

- [ ] **Step 4: Update environment scripts and documentation**

Treat Ollama and Elasticsearch as optional-enhancement checks and core MySQL/Nacos/Redis/RabbitMQ as required for the full demo.

- [ ] **Step 5: Commit with `git commit -m "feat: add Docker infrastructure profile"`**

### Task 8: Full verification, runtime demo, and presentation refresh

**Files:**
- Modify: `docs/课程汇报说明.md`
- Modify: `docs/AI求职雷达课程项目答辩-最终展示版.pptx`

- [ ] **Step 1: Run all backend tests**

Run: `mvn -f ai-job-radar-parent/pom.xml clean test`

Expected: all tests pass with 0 failures and 0 errors.

- [ ] **Step 2: Run frontend tests and build**

Run: `npm --prefix ai-job-radar-web test`

Run: `npm --prefix ai-job-radar-web run build`

Expected: both exit 0.

- [ ] **Step 3: Verify Ollama live path**

Run AI health, matching, question generation, and evaluation requests against `qwen3:8b` and `qwen3-embedding:4b`.

- [ ] **Step 4: Verify Ollama fallback**

Set `AI_ENABLED=false`, call matching and interview endpoints, and confirm legacy scoring and four questions remain available.

- [ ] **Step 5: Verify Elasticsearch live path and fallback**

Search with ES running, stop only ES, repeat the search, and confirm MySQL results remain available.

- [ ] **Step 6: Refresh screenshots, architecture counts, test totals, and demonstration commands in the final PPT**

- [ ] **Step 7: Render and visually inspect every final slide before delivery**

