# BOSS Salary Decoding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Decode the salary digits already visible on the current BOSS job-list page so imported jobs store and display their real monthly salary range.

**Architecture:** Decode BOSS private-use digit characters at the browser extraction boundary and repeat the same normalization at the importer boundary as defense in depth. Preserve the decoded salary text through the existing bridge, Spring service, MySQL, search document, and frontend so daily-pay jobs are not mislabeled as `0-0K`.

**Tech Stack:** Chrome Extension Manifest V3, JavaScript, Node.js built-in test runner, Playwright test fixtures

---

### Task 1: Reproduce the private-use salary failure

**Files:**
- Modify: `job-importer/test/extensionExtractor.test.js`
- Modify: `job-importer/test/normalizeJob.test.js`

- [ ] **Step 1: Write the failing extractor test**

Use a current-layout card whose salary is `\uE032\uE036-\uE033\uE031K` and assert that `extractBossJobs` returns `15-20K`.

- [ ] **Step 2: Write the failing normalizer test**

Call `parseSalary('\uE032\uE036-\uE033\uE031K')` and assert `{ salaryMin: 15000, salaryMax: 20000 }`.

- [ ] **Step 3: Verify RED**

Run: `node --test test/extensionExtractor.test.js test/normalizeJob.test.js`

Expected: both new assertions fail because the private-use characters are not decoded.

### Task 2: Decode salary at both input boundaries

**Files:**
- Modify: `boss-chrome-extension/extractor.js`
- Modify: `job-importer/src/normalizeJob.js`

- [ ] **Step 1: Add the minimal decoder to the extension**

Add a pure function that maps code points `0xE031..0xE03A` to strings `0..9`, leaves other characters unchanged, and call it for the salary field.

- [ ] **Step 2: Add the same defensive normalization to the importer**

Decode the input at the start of `parseSalary` before applying its existing salary-range regular expression.

- [ ] **Step 3: Verify GREEN**

Run: `node --test test/extensionExtractor.test.js test/normalizeJob.test.js`

Expected: all focused tests pass.

- [ ] **Step 4: Run the importer suite**

Run: `npm test`

Expected: zero failed tests.

### Task 3: Verify the complete project and live update path

**Files:**
- No production file changes expected.

- [ ] **Step 1: Run backend tests**

Run Maven tests from `ai-job-radar-parent` and require zero failures and errors.

- [ ] **Step 2: Run frontend tests and build**

Run the existing test and build commands from `ai-job-radar-web` and require successful exits.

- [ ] **Step 3: Reload the normal Chrome extension and import the visible BOSS cards**

Use the existing logged-in `AIJobRadarExtensionCheck` profile without remote-debugging flags, reopen the BOSS list, and trigger the extension import once.

- [ ] **Step 4: Verify persistence and UI**

Check that the current external IDs have nonzero `salary_min` and `salary_max` values and that `/jobs` shows readable salary ranges rather than `0-0K` for those rows.

- [ ] **Step 5: Commit and publish**

Commit only the salary fix and its tests, preserve unrelated PPT files, then update the public course repository.

### Task 4: Preserve non-monthly salary formats

**Files:**
- Modify: `job-importer/src/normalizeJob.js`
- Modify: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/pojo/Job.java`
- Modify: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/pojo/dto/JobImportDTO.java`
- Modify: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/service/JobImportService.java`
- Modify: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/search/JobDocument.java`
- Modify: `ai-job-radar-parent/common/src/main/java/cn/sdu/radar/vo/JobSummaryVO.java`
- Modify: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/service/impl/JobServiceImpl.java`
- Modify: `sql/ai_job_radar.sql`
- Modify: `ai-job-radar-web/src/utils/format.js`
- Test: importer, job-service, and frontend existing test files

- [ ] **Step 1: Write failing tests for decoded daily salary text and frontend fallback**

Assert that the importer preserves `200-600元/天`, the backend copies it into query output, and the frontend prefers it over numeric monthly fields while using `薪资面议` only when all salary fields are empty.

- [ ] **Step 2: Verify RED**

Run the focused Node and Maven tests and require the new assertions to fail for the missing `salaryText` path.

- [ ] **Step 3: Implement the end-to-end salary text field**

Add one nullable/default-empty `salary_text VARCHAR(50)` column and matching Java/JavaScript fields. Do not convert daily salary to monthly salary.

- [ ] **Step 4: Verify GREEN and rerun the live import**

Run the complete suites, migrate the local table, import the same 15 cards, and verify that both `K` and `元/天` values are readable on `/jobs`.
