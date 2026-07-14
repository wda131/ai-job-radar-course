# Mandatory Stack and Defense Delivery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐 Redis、RabbitMQ 和消息中心，完成可演示、可测试、可答辩并已发布到 GitHub 的 AI 求职雷达课程项目。

**Architecture:** job-service 通过 Spring Cache 使用 Redis 缓存职位查询；application-service 发布投递状态事件；notification-service 使用 RabbitMQ 异步消费并通过 MyBatis Plus 写入 MySQL。Vue 3 新增消息中心，所有请求继续经 Gateway，服务继续由 Nacos 发现。

**Tech Stack:** Spring Boot 2.3.12, Spring Cloud Hoxton.SR12, Spring Cloud Alibaba 2.2.9, MyBatis Plus 3.3.1, Redis, RabbitMQ, MySQL 8, Vue 3, Node.js/Vite, Nacos, Gateway, OpenFeign.

---

## File map

- `ai-job-radar-parent/common/.../event/ApplicationStatusEvent.java`: 跨服务消息契约。
- `ai-job-radar-parent/job-service/.../config/RedisCacheConfig.java`: JSON 缓存与 TTL。
- `ai-job-radar-parent/application-service/.../mq/ApplicationEventPublisher.java`: 投递事件发布。
- `ai-job-radar-parent/notification-service/`: 完整通知微服务，含 controller/service/mapper/entity/config/test。
- `ai-job-radar-web/src/views/NotificationsView.vue`: 消息中心页面。
- `sql/ai_job_radar.sql`: 通知表与索引。
- `scripts/start-backend.ps1`: 六个业务服务与网关启动。
- `docs/AI求职雷达课程项目答辩.pptx`: 最终答辩材料。

### Task 1: Redis 职位缓存

**Files:**
- Modify: `ai-job-radar-parent/job-service/pom.xml`
- Create: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/config/RedisCacheConfig.java`
- Modify: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/service/impl/JobServiceImpl.java`
- Modify: `ai-job-radar-parent/job-service/src/main/resources/bootstrap.yml`
- Create: `ai-job-radar-parent/job-service/src/test/java/cn/sdu/radar/config/RedisCacheConfigTest.java`

- [ ] **Step 1: Write the failing cache contract test**

```java
assertEquals("jobs", JobServiceImpl.class.getMethod("search", String.class, String.class,
        Integer.class, long.class, long.class).getAnnotation(Cacheable.class).cacheNames()[0]);
assertEquals(Duration.ofMinutes(10), RedisCacheConfig.CACHE_TTL);
```

- [ ] **Step 2: Run the focused test and confirm it fails**

Run: `mvn -pl job-service -am -Dtest=RedisCacheConfigTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because the configuration and annotations do not exist.

- [ ] **Step 3: Add the Redis starter, connection properties, JSON RedisCacheManager, and cache annotations**

```java
@Cacheable(cacheNames = "jobs", key = "#keyword + ':' + #city + ':' + #minSalary + ':' + #page + ':' + #size")
public PageResult<JobSummaryVO> search(...) { ... }

@Cacheable(cacheNames = "job-detail", key = "#id")
public JobSummaryVO getById(Long id) { ... }
```

- [ ] **Step 4: Run job-service tests and verify pass**

Run: `mvn -pl job-service -am test`
Expected: all job-service and dependency tests PASS.

- [ ] **Step 5: Commit**

```bash
git commit -am "feat: cache job queries with Redis"
```

### Task 2: RabbitMQ application event publisher

**Files:**
- Create: `ai-job-radar-parent/common/src/main/java/cn/sdu/radar/event/ApplicationStatusEvent.java`
- Modify: `ai-job-radar-parent/application-service/pom.xml`
- Create: `ai-job-radar-parent/application-service/src/main/java/cn/sdu/radar/config/RabbitMqConfig.java`
- Create: `ai-job-radar-parent/application-service/src/main/java/cn/sdu/radar/mq/ApplicationEventPublisher.java`
- Modify: `ai-job-radar-parent/application-service/src/main/java/cn/sdu/radar/service/impl/ApplicationServiceImpl.java`
- Modify: `ai-job-radar-parent/application-service/src/main/resources/bootstrap.yml`
- Modify: `ai-job-radar-parent/application-service/src/test/java/cn/sdu/radar/service/ApplicationServiceImplTest.java`

- [ ] **Step 1: Extend tests to require one event after create and update**

```java
verify(eventPublisher).publish(argThat(event ->
        event.getUserId().equals(1L) && "APPLIED".equals(event.getStatus())));
```

- [ ] **Step 2: Run the focused test and confirm constructor/type failures**

Run: `mvn -pl application-service -am -Dtest=ApplicationServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because the event publisher does not exist.

- [ ] **Step 3: Implement durable direct exchange, queue binding, event DTO, and tolerant publisher**

```java
rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
```

`ApplicationServiceImpl` publishes only after successful insert/update. `ApplicationEventPublisher` catches `AmqpException` and logs the event id so the main application action remains available during temporary broker outages.

- [ ] **Step 4: Run application-service tests and verify pass**

Run: `mvn -pl application-service -am test`
Expected: all tests PASS and publisher verification succeeds.

- [ ] **Step 5: Commit**

```bash
git commit -am "feat: publish application events with RabbitMQ"
```

### Task 3: Notification microservice

**Files:**
- Modify: `ai-job-radar-parent/pom.xml`
- Create: `ai-job-radar-parent/notification-service/pom.xml`
- Create: `ai-job-radar-parent/notification-service/src/main/java/cn/sdu/radar/NotificationApplication.java`
- Create: `ai-job-radar-parent/notification-service/src/main/java/cn/sdu/radar/config/WebConfig.java`
- Create: `ai-job-radar-parent/notification-service/src/main/java/cn/sdu/radar/config/RabbitMqConfig.java`
- Create: `ai-job-radar-parent/notification-service/src/main/java/cn/sdu/radar/controller/NotificationController.java`
- Create: `ai-job-radar-parent/notification-service/src/main/java/cn/sdu/radar/mapper/NotificationMapper.java`
- Create: `ai-job-radar-parent/notification-service/src/main/java/cn/sdu/radar/mq/ApplicationEventListener.java`
- Create: `ai-job-radar-parent/notification-service/src/main/java/cn/sdu/radar/pojo/Notification.java`
- Create: `ai-job-radar-parent/notification-service/src/main/java/cn/sdu/radar/service/NotificationService.java`
- Create: `ai-job-radar-parent/notification-service/src/main/java/cn/sdu/radar/service/impl/NotificationServiceImpl.java`
- Create: `ai-job-radar-parent/notification-service/src/main/resources/bootstrap.yml`
- Create: `ai-job-radar-parent/notification-service/src/test/java/cn/sdu/radar/service/NotificationServiceImplTest.java`

- [ ] **Step 1: Write service tests for idempotent consume, list, unread count, single read, and read all**

```java
service.consume(event);
verify(mapper).insert(argThat(n -> event.getEventId().equals(n.getEventId())));
when(mapper.selectCount(any())).thenReturn(1);
service.consume(event);
verify(mapper, times(1)).insert(any());
```

- [ ] **Step 2: Run notification tests and confirm the module is missing**

Run: `mvn -pl notification-service -am test`
Expected: FAIL because the module does not exist.

- [ ] **Step 3: Implement the course-style Controller/Service/Mapper module and Rabbit listener**

```java
@RabbitListener(queues = RabbitMqConfig.QUEUE)
public void onApplicationChanged(ApplicationStatusEvent event) {
    notificationService.consume(event);
}
```

All read/update queries include `user_id`; duplicate `event_id` is ignored before insert.

- [ ] **Step 4: Run module tests and verify pass**

Run: `mvn -pl notification-service -am test`
Expected: notification and common tests PASS.

- [ ] **Step 5: Commit**

```bash
git commit -am "feat: add asynchronous notification service"
```

### Task 4: Gateway, SQL, Vue message center

**Files:**
- Modify: `ai-job-radar-parent/gateway/src/main/resources/bootstrap.yml`
- Modify: `ai-job-radar-parent/gateway/src/test/java/cn/sdu/radar/gateway/GatewayConfigurationTest.java`
- Modify: `sql/ai_job_radar.sql`
- Modify: `ai-job-radar-web/src/api/index.js`
- Modify: `ai-job-radar-web/src/router/index.js`
- Modify: `ai-job-radar-web/src/App.vue`
- Create: `ai-job-radar-web/src/views/NotificationsView.vue`
- Modify: `ai-job-radar-web/test/ui-state.test.js`

- [ ] **Step 1: Write failing route/API/UI tests**

```js
assert.match(apiSource, /getNotifications/)
assert.match(routerSource, /\/notifications/)
assert.match(appSource, /消息中心/)
```

Add a gateway assertion that `/api/notifications/**` routes to `lb://notification-service`.

- [ ] **Step 2: Run Maven gateway and Node tests and confirm failure**

Run: `mvn -pl gateway -am test` and `npm test`
Expected: route and UI contract assertions FAIL.

- [ ] **Step 3: Add notification route, SQL table, API functions, nav item, and accessible message center page**

```js
export const getNotifications = () => request.get('/api/notifications')
export const getUnreadCount = () => request.get('/api/notifications/unread-count')
export const readNotification = id => request.put(`/api/notifications/${id}/read`)
export const readAllNotifications = () => request.put('/api/notifications/read-all')
```

- [ ] **Step 4: Run tests and production build**

Run: `mvn -pl gateway -am test`; `npm test`; `npm run build`
Expected: PASS and Vite emits `dist`.

- [ ] **Step 5: Commit**

```bash
git commit -am "feat: add notification center interface"
```

### Task 5: Local environment and end-to-end demonstration

**Files:**
- Modify: `scripts/start-backend.ps1`
- Create: `scripts/check-environment.ps1`
- Modify: `README.md`
- Modify: `docs/课程汇报说明.md`

- [ ] **Step 1: Add a script contract test or read-only preflight checks for ports 3306, 5672, 6379, and 8848**

```powershell
$dependencies = @{ MySQL = 3306; RabbitMQ = 5672; Redis = 6379; Nacos = 8848 }
```

- [ ] **Step 2: Run the preflight and record any missing dependency**

Run: `powershell -ExecutionPolicy Bypass -File scripts/check-environment.ps1`
Expected: each dependency reports READY or a precise startup action.

- [ ] **Step 3: Install/start missing RabbitMQ, import SQL, package services, and start six services plus gateway**

Run: `mvn clean package`; `powershell -File scripts/start-backend.ps1`; `npm run dev -- --host 127.0.0.1 --port 5174`.

- [ ] **Step 4: Exercise the core API flow and inspect Redis/Rabbit/notification evidence**

Expected: login succeeds; two identical job queries leave Redis keys; application creation returns APPLIED; notification endpoint contains the async event; read action changes unread count.

- [ ] **Step 5: Commit**

```bash
git commit -am "docs: add complete local demo workflow"
```

### Task 6: Defense presentation and Git evidence

**Files:**
- Create: `docs/AI求职雷达课程项目答辩.pptx`
- Create: `docs/答辩演示脚本.md`

- [ ] **Step 1: Capture current system, architecture, Redis/Rabbit evidence, test output, and GitHub commit history**

Use real screenshots only for the running UI and Git evidence. Use native PowerPoint shapes for architecture and flows.

- [ ] **Step 2: Build a 15-slide deck using the bundled Codex Grid layout library**

Slides: title, problem, project loop, course-stack alignment, architecture, six modules, business flow, Redis, RabbitMQ, engineering design, demo script, tests, Git evidence, six-person work assignment, summary/Q&A.

- [ ] **Step 3: Render every slide and inspect every image at full size**

Expected: no clipping, overlap, tiny text, mojibake, or placeholder text other than explicitly labeled member-name fields.

- [ ] **Step 4: Run presentation diagnostics**

Run the presentation skill's `slides_test.py` and render commands.
Expected: zero overflow and overlap errors.

- [ ] **Step 5: Commit**

```bash
git commit -am "docs: add polished course defense presentation"
```

### Task 7: Final verification and public GitHub delivery

**Files:**
- Verify all files above.

- [ ] **Step 1: Run full Maven, Node, and build verification from a clean state**

Run: `mvn clean test`; `npm test`; `npm run build`.
Expected: all tests and builds PASS.

- [ ] **Step 2: Run final API smoke test with live infrastructure**

Expected: Gateway, Nacos, Redis, RabbitMQ, all six business services, and the Vue UI are healthy.

- [ ] **Step 3: Verify Git cleanliness and review staged diff**

Run: `git status --short`; `git diff --check`.
Expected: no unstaged files and no whitespace errors.

- [ ] **Step 4: Split the course project subtree and push public `main`**

```bash
git subtree split --prefix=ai-job-radar-course -b codex/course-release
git push course-public codex/course-release:main
```

- [ ] **Step 5: Confirm public repository files and release commit**

Run: `gh api repos/wda131/ai-job-radar-course/commits/main --jq .sha`.
Expected: SHA matches the pushed subtree head.

## Self-review

- Spec coverage: every mandatory technology appears in Tasks 1–5; six modules and member work appear in Tasks 3, 4, and 6; local demonstration, Git evidence, PPT, tests, and public delivery appear in Tasks 5–7.
- Scope discipline: optional Elasticsearch/Docker and unrelated new business features are deliberately excluded.
- Type consistency: `ApplicationStatusEvent`, queue/exchange names, `/api/notifications` paths, and six-service naming are consistent across producer, consumer, gateway, frontend, SQL, and presentation.
