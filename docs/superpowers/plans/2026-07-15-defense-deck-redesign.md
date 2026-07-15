# AI 求职雷达答辩 PPT 重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 生成一份可以直接用于课程答辩、以真实运行截图为证据且表达自然的 AI 求职雷达终稿 PPT。

**Architecture:** 从当前本地运行系统采集页面截图，在外部临时目录中使用 `@oai/artifact-tool` 构建 16:9 演示文稿，再输出到项目 `docs` 目录。生成后同时运行结构检测、逐页渲染和视觉复查，不修改原有 PPT。

**Tech Stack:** Codex in-app Browser、JavaScript ES Modules、`@oai/artifact-tool`、PowerPoint PPTX、项目现有 Vue 3 页面

---

### Task 1: 核验运行链路

**Files:**
- Read: `scripts/smoke-test.mjs`
- Read: `ai-job-radar-parent/logs/*.log`

- [ ] **Step 1: 检查依赖端口**

运行 `netstat -ano`，确认 8848、3306、6379、5672、9000—9007、9011、5174 正在监听。

- [ ] **Step 2: 运行完整冒烟测试**

运行：`node scripts/smoke-test.mjs`

预期：`gateway` 为 `ok`，职位列表非空，匹配结果 `aiUsed=true`，面试题数量为 4，回答反馈 `aiUsed=true`。

### Task 2: 采集真实功能截图

**Files:**
- Create: `%TEMP%/codex-presentations/ai-job-radar-defense-rework/tmp/assets/dashboard.png`
- Create: `%TEMP%/codex-presentations/ai-job-radar-defense-rework/tmp/assets/jobs.png`
- Create: `%TEMP%/codex-presentations/ai-job-radar-defense-rework/tmp/assets/matches.png`
- Create: `%TEMP%/codex-presentations/ai-job-radar-defense-rework/tmp/assets/applications.png`
- Create: `%TEMP%/codex-presentations/ai-job-radar-defense-rework/tmp/assets/interviews.png`
- Create: `%TEMP%/codex-presentations/ai-job-radar-defense-rework/tmp/assets/interview-feedback.png`

- [ ] **Step 1: 采集总览与职位页**

在已登录的本地系统中分别打开 `/` 与 `/jobs`，确认职位页显示 32 个机会、BOSS 来源和正确薪资后保存截图。

- [ ] **Step 2: 采集匹配与求职进度页**

打开 `/matches` 与 `/applications`，确认页面有实际记录后保存截图。

- [ ] **Step 3: 采集模拟面试与反馈页**

打开 `/interviews`，保存四道题页面和回答反馈页面。截图只使用当前系统真实内容。

### Task 3: 编写演示文稿生成模块

**Files:**
- Create: `%TEMP%/codex-presentations/ai-job-radar-defense-rework/tmp/build-defense-deck.mjs`
- Create: `%TEMP%/codex-presentations/ai-job-radar-defense-rework/tmp/source-notes.txt`
- Create: `docs/AI求职雷达课程项目答辩-答辩终稿.pptx`

- [ ] **Step 1: 初始化 artifact-tool 工作区**

运行：

```powershell
$env:HOME=$env:USERPROFILE
node "$SKILL_DIR/container_tools/setup_artifact_tool_workspace.mjs" --workspace "$TMP_DIR"
```

预期：`$TMP_DIR/node_modules/@oai/artifact-tool` 可解析。

- [ ] **Step 2: 实现统一版式函数**

在 `build-defense-deck.mjs` 中实现 `addTitle`、`addFooter`、`addScreenshot`、`addCallout` 与 `addSectionRule`。统一使用 1280×720、白色背景、深蓝标题、青色重点和不小于 18pt 的正文。

- [ ] **Step 3: 实现 13 页叙事**

按设计文档生成封面、项目缘起、业务闭环、课程架构、总览、职位雷达、智能匹配、收藏投递、模拟面试、回答反馈、数据链路、验证结果、团队分工与结论。功能页以大截图为主体，不使用重复卡片矩阵。

- [ ] **Step 4: 导出 PPTX 和预览图**

运行：`node "$TMP_DIR/build-defense-deck.mjs"`

预期：生成最终 PPTX、13 张 PNG、13 份布局 JSON 和一张 montage。

### Task 4: 逐页质量检查

**Files:**
- Read: `%TEMP%/codex-presentations/ai-job-radar-defense-rework/tmp/preview/*.png`
- Read: `%TEMP%/codex-presentations/ai-job-radar-defense-rework/tmp/layout/*.json`
- Modify: `%TEMP%/codex-presentations/ai-job-radar-defense-rework/tmp/build-defense-deck.mjs`

- [ ] **Step 1: 运行画布越界检查**

运行：

```powershell
python "$SKILL_DIR/container_tools/slides_test.py" "docs/AI求职雷达课程项目答辩-答辩终稿.pptx"
```

预期：无越界或裁切错误。

- [ ] **Step 2: 检查每一页全尺寸渲染**

逐页确认标题不换行、正文不截断、截图不变形、没有内部说明和制作建议。

- [ ] **Step 3: 修复问题并重新导出**

对发现的问题修改 `build-defense-deck.mjs`，重新执行生成和检测，直到全部通过。

### Task 5: 最终交付核验

**Files:**
- Verify: `docs/AI求职雷达课程项目答辩-答辩终稿.pptx`

- [ ] **Step 1: 重新运行项目冒烟测试**

运行：`node scripts/smoke-test.mjs`

预期：核心链路仍然通过。

- [ ] **Step 2: 核对交付文件**

确认最终 PPTX 可以渲染为 13 页，文件大小非零，原有 PPT 文件未被覆盖。

- [ ] **Step 3: 提交最终 PPTX**

```powershell
git add docs/AI求职雷达课程项目答辩-答辩终稿.pptx docs/superpowers/plans/2026-07-15-defense-deck-redesign.md
git commit -m "docs: rebuild final course defense deck"
```
