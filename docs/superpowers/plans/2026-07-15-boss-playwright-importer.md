# BOSS Playwright Job Importer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an optional Node.js Playwright tool that reads a small user-authorized BOSS job result set and imports normalized, deduplicated jobs through the existing Spring Cloud Gateway into `job-service`.

**Architecture:** Keep browser automation in a standalone `job-importer` process. It uses a persistent local Chromium profile, pure normalization/parsing modules, and the existing course JWT login endpoint; `job-service` owns validation, MySQL upsert, Redis eviction, and best-effort Elasticsearch indexing. The importer is manual and read-only toward BOSS, so all existing course flows remain available when it is stopped.

**Tech Stack:** Node.js 24, Playwright 1.49.1, Spring Boot 2.3.12.RELEASE, Spring MVC, MyBatis Plus 3.3.1, MySQL 8, Redis, Elasticsearch 7.6.2, Vue 3, Node built-in test runner.

---

## File map

### New Node.js importer

- `job-importer/package.json`: scripts and pinned Playwright dependency.
- `job-importer/src/normalizeJob.js`: pure normalization and validation functions.
- `job-importer/src/bossParser.js`: parse visible BOSS cards and detail text from a Playwright page.
- `job-importer/src/browserSession.js`: persistent Chromium context and login/verification boundary.
- `job-importer/src/importClient.js`: course login and batch import HTTP client.
- `job-importer/src/cli.js`: argument parsing, bounded collection, import, and result output.
- `job-importer/test/fixtures/boss-results.html`: sanitized local result page.
- `job-importer/test/normalizeJob.test.js`: normalization unit tests.
- `job-importer/test/bossParser.test.js`: real Playwright fixture parsing test.
- `job-importer/test/importClient.test.js`: real local HTTP contract test.

### Spring course project

- `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/pojo/dto/JobImportDTO.java`: one normalized imported job.
- `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/pojo/dto/JobImportBatchDTO.java`: bounded batch request.
- `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/pojo/vo/JobImportResultVO.java`: created/updated/rejected summary.
- `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/service/JobImportService.java`: validation, upsert, cache eviction, and best-effort indexing.
- `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/controller/JobController.java`: authenticated `POST /api/jobs/import` endpoint.
- `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/pojo/Job.java`: import metadata columns.
- `ai-job-radar-parent/common/src/main/java/cn/sdu/radar/vo/JobSummaryVO.java`: expose source and source URL.
- `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/search/JobDocument.java`: preserve source fields through Elasticsearch.
- `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/service/impl/JobServiceImpl.java`: map source fields to API output.
- `ai-job-radar-parent/job-service/src/test/java/cn/sdu/radar/service/JobImportServiceTest.java`: import behavior tests.
- `sql/ai_job_radar.sql`: fresh-install schema.
- `sql/upgrade-boss-import.sql`: current database migration.

### Vue and operations

- `ai-job-radar-web/src/components/JobCard.vue`: compact `BOSS` or `本地数据` source marker.
- `ai-job-radar-web/test/ui-state.test.js`: source marker regression test.
- `.gitignore`: exclude Playwright browser state.
- `scripts/start-job-importer.ps1`: dependency check and manual importer launcher.
- `README.md`: install, login, fixture, real import, and safety instructions.
- `scripts/smoke-test.mjs`: assert that a keyword search still returns jobs after import support.

---

### Task 1: Pure job normalization

**Files:**
- Create: `job-importer/package.json`
- Create: `job-importer/src/normalizeJob.js`
- Create: `job-importer/test/normalizeJob.test.js`

- [ ] **Step 1: Create the package manifest and write failing normalization tests**

`job-importer/package.json`:

```json
{
  "name": "ai-job-radar-job-importer",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "test": "node --test test/*.test.js",
    "import:boss": "node src/cli.js"
  },
  "dependencies": {
    "playwright": "1.49.1"
  }
}
```

`job-importer/test/normalizeJob.test.js`:

```js
import assert from 'node:assert/strict'
import test from 'node:test'
import { normalizeJob, normalizeLimit, parseExperience, parseSalary } from '../src/normalizeJob.js'

test('parses monthly K salary ranges', () => {
  assert.deepEqual(parseSalary('10-16K'), { salaryMin: 10000, salaryMax: 16000 })
  assert.deepEqual(parseSalary('面议'), { salaryMin: 0, salaryMax: 0 })
})

test('normalizes experience and clamps import limit', () => {
  assert.equal(parseExperience('3-5年'), 3)
  assert.equal(parseExperience('应届生'), 0)
  assert.equal(normalizeLimit('50'), 20)
})

test('builds a validated BOSS import record', () => {
  const result = normalizeJob({
    externalId: 'abc123', title: ' Java开发 ', company: ' 海纳科技 ', city: '威海',
    salary: '10-16K', experience: '1-3年', education: '本科',
    description: '负责后端服务', requirements: 'Java Spring Boot',
    welfareTags: ['双休', '五险一金'], sourceUrl: 'https://www.zhipin.com/job_detail/abc123.html'
  })
  assert.equal(result.source, 'BOSS')
  assert.equal(result.salaryMin, 10000)
  assert.equal(result.welfareTags, '双休,五险一金')
})

test('rejects records without identity fields', () => {
  assert.throws(() => normalizeJob({ title: '', company: '公司' }), /职位标题不能为空/)
})
```

- [ ] **Step 2: Run the test and verify the missing-module failure**

Run:

```powershell
npm --prefix job-importer test
```

Expected: FAIL because `src/normalizeJob.js` does not exist.

- [ ] **Step 3: Implement the minimal normalizer**

`job-importer/src/normalizeJob.js` exports:

```js
const text = value => String(value || '').trim()
const trimTo = (value, max) => text(value).slice(0, max)

export function parseSalary(value) {
  const match = text(value).toUpperCase().match(/(\d+(?:\.\d+)?)\s*-\s*(\d+(?:\.\d+)?)\s*K/)
  if (!match) return { salaryMin: 0, salaryMax: 0 }
  return {
    salaryMin: Math.round(Number(match[1]) * 1000),
    salaryMax: Math.round(Number(match[2]) * 1000)
  }
}

export function parseExperience(value) {
  const normalized = text(value)
  if (!normalized || /不限|应届|在校/.test(normalized)) return 0
  const match = normalized.match(/(\d+)/)
  return match ? Number(match[1]) : 0
}

export function normalizeLimit(value) {
  const parsed = Number.parseInt(value || '10', 10)
  return Math.min(20, Math.max(1, Number.isFinite(parsed) ? parsed : 10))
}

export function normalizeJob(raw, fallbackCity = '') {
  const title = trimTo(raw.title, 100)
  const company = trimTo(raw.company, 100)
  const externalId = trimTo(raw.externalId, 100)
  if (!title) throw new Error('职位标题不能为空')
  if (!company) throw new Error('公司名称不能为空')
  if (!externalId) throw new Error('外部职位编号不能为空')
  const salary = parseSalary(raw.salary)
  return {
    source: 'BOSS', externalId, title, company,
    city: trimTo(raw.city || fallbackCity || '不限', 50),
    ...salary,
    experienceYears: parseExperience(raw.experience),
    education: trimTo(raw.education || '不限', 50),
    description: trimTo(raw.description || '来源职位页面未提供完整描述', 1000),
    requirements: trimTo(raw.requirements || raw.description || '以来源职位页面为准', 1000),
    welfareTags: trimTo(Array.isArray(raw.welfareTags) ? raw.welfareTags.join(',') : raw.welfareTags, 300),
    sourceUrl: trimTo(raw.sourceUrl, 1000), status: 'OPEN'
  }
}
```

- [ ] **Step 4: Run the normalizer tests**

Run: `npm --prefix job-importer test`

Expected: 4 tests pass.

- [ ] **Step 5: Commit**

```powershell
git add ai-job-radar-course/job-importer
git commit -m "feat: normalize imported BOSS jobs"
```

### Task 2: Playwright fixture parser and browser boundary

**Files:**
- Create: `job-importer/test/fixtures/boss-results.html`
- Create: `job-importer/test/bossParser.test.js`
- Create: `job-importer/src/bossParser.js`
- Create: `job-importer/src/browserSession.js`

- [ ] **Step 1: Add a sanitized fixture and a failing real-Playwright parser test**

The fixture contains two `.job-card-wrapper` elements with `.job-name`, `.company-name`, `.salary`, `.job-area`, `.job-info li`, `.tag-list li`, and an `a.job-card-left` link. The test launches Chromium, calls `page.setContent(fixture)`, then expects two normalized records and the first external ID `abc123`.

```js
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { chromium } from 'playwright'
import { extractBossCards } from '../src/bossParser.js'

test('extracts visible BOSS cards from a sanitized page', async () => {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage()
  await page.setContent(await readFile(new URL('./fixtures/boss-results.html', import.meta.url), 'utf8'))
  const jobs = await extractBossCards(page, '威海', 20)
  await browser.close()
  assert.equal(jobs.length, 2)
  assert.equal(jobs[0].externalId, 'abc123')
  assert.equal(jobs[0].salary, '10-16K')
})
```

- [ ] **Step 2: Install dependencies and verify RED**

Run:

```powershell
npm --prefix job-importer install
npx --prefix job-importer playwright install chromium
npm --prefix job-importer test
```

Expected: normalization tests pass and parser test fails because `bossParser.js` is missing.

- [ ] **Step 3: Implement bounded card extraction**

`extractBossCards(page, fallbackCity, limit)` uses one `locator('.job-card-wrapper, .job-card-box, .job-list-box li')`, checks its count, and for each bounded card reads the named child selectors. It derives `externalId` from `/job_detail/<id>.html`, removes duplicate external IDs, and returns raw records for `normalizeJob`.

The module also exports `detectAccessRestriction(page)`, which reads the visible title and first 1000 characters of body text and returns true for `验证码`, `安全验证`, `访问异常`, or `请稍后再试`.

- [ ] **Step 4: Implement the persistent browser context**

`browserSession.js` exports:

```js
import path from 'node:path'
import { chromium } from 'playwright'

export async function openBossSession(profileDir) {
  return chromium.launchPersistentContext(path.resolve(profileDir), {
    headless: false,
    viewport: { width: 1365, height: 850 },
    locale: 'zh-CN'
  })
}
```

No stealth plugin, proxy, CAPTCHA solver, cookie export, or credential injection is added.

- [ ] **Step 5: Run all importer tests**

Run: `npm --prefix job-importer test`

Expected: 5 tests pass.

- [ ] **Step 6: Commit**

```powershell
git add ai-job-radar-course/job-importer
git commit -m "feat: parse BOSS job pages with Playwright"
```

### Task 3: Database and API contracts

**Files:**
- Modify: `sql/ai_job_radar.sql`
- Create: `sql/upgrade-boss-import.sql`
- Modify: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/pojo/Job.java`
- Modify: `ai-job-radar-parent/common/src/main/java/cn/sdu/radar/vo/JobSummaryVO.java`
- Create: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/pojo/dto/JobImportDTO.java`
- Create: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/pojo/dto/JobImportBatchDTO.java`
- Create: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/pojo/vo/JobImportResultVO.java`
- Test: `ai-job-radar-parent/job-service/src/test/java/cn/sdu/radar/pojo/JobImportContractsTest.java`

- [ ] **Step 1: Write a failing contract test**

The test creates `JobImportDTO`, sets every importer field, wraps it in `JobImportBatchDTO`, and verifies Lombok getters plus `JobSummaryVO.source` and `sourceUrl`. It fails until the DTOs and fields exist.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
mvn -f ai-job-radar-parent/pom.xml -pl job-service -am -Dtest=JobImportContractsTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: test compilation fails because the import DTOs and source fields do not exist.

- [ ] **Step 3: Add the schema and Java contracts**

Add the four import columns and `uk_jobs_source_external` to the fresh schema. Create `upgrade-boss-import.sql` with the same `ALTER TABLE`. Add matching fields to `Job` and `JobSummaryVO`.

`JobImportDTO` contains `source`, `externalId`, `title`, `company`, `city`, `salaryMin`, `salaryMax`, `experienceYears`, `education`, `description`, `requirements`, `welfareTags`, `sourceUrl`, and `status`. `JobImportBatchDTO` contains `List<JobImportDTO> jobs`. `JobImportResultVO` contains integer counters and `List<String> errors`.

- [ ] **Step 4: Run the focused contract tests**

Run: `mvn -f ai-job-radar-parent/pom.xml -pl job-service -am test`

Expected: contract and existing job-service tests pass.

- [ ] **Step 5: Commit**

```powershell
git add ai-job-radar-course/sql ai-job-radar-course/ai-job-radar-parent
git commit -m "feat: add imported job data contracts"
```

### Task 4: Test-first import service

**Files:**
- Create: `ai-job-radar-parent/job-service/src/test/java/cn/sdu/radar/service/JobImportServiceTest.java`
- Create: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/service/JobImportService.java`
- Modify: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/search/JobDocument.java`
- Modify: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/search/JobSearchService.java`
- Modify: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/service/impl/JobServiceImpl.java`

- [ ] **Step 1: Write failing service tests**

Cover these behaviors with mocked `JobMapper` and `JobSearchRepository` dependencies:

1. Missing existing row calls `insert` and increments `created`.
2. Existing `BOSS + externalId` row calls `updateById` and increments `updated`.
3. Missing title is rejected while another valid row succeeds.
4. More than 20 rows throws `BusinessException(400)`.
5. Elasticsearch `saveAll` failure does not undo the successful mapper calls.
6. Reflection verifies `importJobs` has `@Transactional` and `@CacheEvict(allEntries = true)` for both cache names.

- [ ] **Step 2: Run the service test and verify RED**

Run:

```powershell
mvn -f ai-job-radar-parent/pom.xml -pl job-service -am -Dtest=JobImportServiceTest test
```

Expected: test compilation fails because `JobImportService` is missing.

- [ ] **Step 3: Implement minimal import behavior**

`JobImportService.importJobs(List<JobImportDTO>)`:

- Rejects null, empty, or more than 20 entries with `BusinessException(400, ...)`.
- Accepts only source `BOSS`.
- Validates external ID, title, company, salary range, and field lengths.
- Uses `QueryWrapper<Job>().eq("source", source).eq("external_id", externalId)`.
- Inserts a new `Job` or updates the existing row's display fields.
- Sets `importedAt` and `postedAt` to the current time when absent.
- Collects row-level validation errors without inserting invalid rows.
- Calls `JobSearchRepository.saveAll` after mapper operations and catches only Elasticsearch runtime failures.
- Uses `@Transactional` and `@CacheEvict(cacheNames = {"jobs", "job-detail"}, allEntries = true)`.

- [ ] **Step 4: Preserve source fields in search results**

Add `source` and `sourceUrl` to `JobDocument`, `from(Job)`, `toSummary()`, and `JobServiceImpl.toSummary()`. Add assertions to existing `JobServiceImplTest` that a BOSS job returns source fields through both MyBatis and Elasticsearch paths.

- [ ] **Step 5: Run job-service tests**

Run: `mvn -f ai-job-radar-parent/pom.xml -pl job-service -am test`

Expected: all common and job-service tests pass.

- [ ] **Step 6: Commit**

```powershell
git add ai-job-radar-course/ai-job-radar-parent
git commit -m "feat: import and index external jobs"
```

### Task 5: Authenticated controller and importer HTTP client

**Files:**
- Create: `ai-job-radar-parent/job-service/src/test/java/cn/sdu/radar/controller/JobControllerTest.java`
- Modify: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/controller/JobController.java`
- Create: `job-importer/test/importClient.test.js`
- Create: `job-importer/src/importClient.js`

- [ ] **Step 1: Write failing Java controller and Node client tests**

The controller test constructs `JobController` with mocked `JobService` and `JobImportService`, submits a one-item `JobImportBatchDTO`, and verifies delegation plus the `CommonResult` data.

The Node test starts a local `node:http` server. `/api/user/login` returns `{code:200,data:{token:"test-token"}}`; `/api/jobs/import` asserts `Authorization: test-token` and returns a summary. The test calls `loginAndImport()` and expects `created === 1`.

- [ ] **Step 2: Run both focused tests and verify RED**

Run:

```powershell
mvn -f ai-job-radar-parent/pom.xml -pl job-service -am -Dtest=JobControllerTest test
npm --prefix job-importer test
```

Expected: Java fails because the import route is missing; Node fails because `importClient.js` is missing.

- [ ] **Step 3: Add `POST /api/jobs/import`**

Inject `JobImportService` into `JobController` and add:

```java
@PostMapping("/import")
public CommonResult<JobImportResultVO> importJobs(@RequestBody JobImportBatchDTO batch) {
    return CommonResult.success(jobImportService.importJobs(
            batch == null ? null : batch.getJobs()));
}
```

The existing `JwtInterceptor` already protects the route through `/api/jobs/**`.

- [ ] **Step 4: Implement the HTTP client**

`loginAndImport({baseUrl, username, password, jobs})` uses built-in `fetch`, checks both HTTP status and `CommonResult.code`, logs in at `/api/user/login`, then posts `{jobs}` to `/api/jobs/import` with the returned token. It never logs credentials or tokens.

- [ ] **Step 5: Run both test suites**

Run:

```powershell
mvn -f ai-job-radar-parent/pom.xml -pl job-service -am test
npm --prefix job-importer test
```

Expected: Java and Node tests pass.

- [ ] **Step 6: Commit**

```powershell
git add ai-job-radar-course/ai-job-radar-parent ai-job-radar-course/job-importer
git commit -m "feat: expose authenticated job import"
```

### Task 6: Manual CLI orchestration

**Files:**
- Create: `job-importer/test/cli.test.js`
- Create: `job-importer/src/cli.js`
- Modify: `.gitignore`
- Create: `scripts/start-job-importer.ps1`

- [ ] **Step 1: Write failing argument tests**

Export `parseArgs(argv)` from `cli.js`. Test `--keyword Java --city 威海 --limit 8 --fixture test/fixtures/boss-results.html`, verify all values, and verify missing keyword throws `必须提供 --keyword`.

- [ ] **Step 2: Run importer tests and verify RED**

Run: `npm --prefix job-importer test`

Expected: CLI test fails because `cli.js` is missing.

- [ ] **Step 3: Implement fixture and live modes**

The CLI:

1. Parses `--keyword`, `--city`, `--limit`, optional `--url`, and optional `--fixture`.
2. Fixture mode opens a temporary headless browser page and calls the same `extractBossCards` function.
3. Live mode launches `.browser-data`, opens `--url` or `https://www.zhipin.com/web/geek/job?query=<keyword>`, and waits up to three minutes for visible job cards while the user logs in.
4. Stops immediately when `detectAccessRestriction` reports a security verification page.
5. Normalizes and deduplicates at most 20 records.
6. Calls `loginAndImport` using `API_BASE_URL`, `RADAR_USERNAME`, and `RADAR_PASSWORD`.
7. Prints only counts and row errors; it never prints cookies, JWT, credentials, or browser storage.
8. Closes the Playwright context in `finally`.

- [ ] **Step 4: Add the safe launcher and ignore browser state**

Append `job-importer/.browser-data/` to `.gitignore`.

`scripts/start-job-importer.ps1` checks `node`, installs dependencies when `node_modules` is absent, checks Gateway port 9000, and runs:

```powershell
npm --prefix "$project\job-importer" run import:boss -- @args
```

- [ ] **Step 5: Run fixture mode against the live Gateway**

Run:

```powershell
.\scripts\start-job-importer.ps1 --keyword Java --city 威海 --limit 2 --fixture job-importer/test/fixtures/boss-results.html
```

Expected: two fixture jobs are created or updated and the command prints a successful summary.

- [ ] **Step 6: Commit**

```powershell
git add ai-job-radar-course/job-importer ai-job-radar-course/scripts ai-job-radar-course/.gitignore
git commit -m "feat: add manual Playwright import command"
```

### Task 7: Source badge and documentation

**Files:**
- Modify: `ai-job-radar-web/test/ui-state.test.js`
- Modify: `ai-job-radar-web/src/components/JobCard.vue`
- Modify: `README.md`
- Modify: `scripts/smoke-test.mjs`

- [ ] **Step 1: Write a failing source-badge frontend test**

Add a file-content assertion that `JobCard.vue` renders `job.source === 'BOSS' ? 'BOSS' : '本地数据'` and contains `.source-pill` styling.

- [ ] **Step 2: Run frontend tests and verify RED**

Run: `npm --prefix ai-job-radar-web test`

Expected: the new source badge test fails.

- [ ] **Step 3: Add the compact source marker**

Place the marker next to the company/city line and add scoped styling that reuses the existing cyan palette. Do not add a new page, filter, modal, or navigation item.

- [ ] **Step 4: Update run instructions and smoke output**

README documents:

- `npm --prefix job-importer install`
- `npx --prefix job-importer playwright install chromium`
- fixture import command
- live command with optional `--url`
- manual login and no-CAPTCHA-bypass boundary
- imported fields and deduplication
- the importer is optional and not a microservice

Update `smoke-test.mjs` to include the first returned job source in its JSON output without requiring BOSS data to exist.

- [ ] **Step 5: Run frontend tests and production build**

Run:

```powershell
npm --prefix ai-job-radar-web test
npm --prefix ai-job-radar-web run build
```

Expected: all tests pass and Vite exits successfully.

- [ ] **Step 6: Commit**

```powershell
git add ai-job-radar-course/ai-job-radar-web ai-job-radar-course/README.md ai-job-radar-course/scripts/smoke-test.mjs
git commit -m "feat: display imported job sources"
```

### Task 8: Database migration and end-to-end verification

**Files:**
- Read: `sql/upgrade-boss-import.sql`
- Read: `scripts/smoke-test.mjs`
- Read: `docs/superpowers/specs/2026-07-15-boss-playwright-importer-design.md`

- [ ] **Step 1: Stop only `job-service` and apply the migration**

Stop the process listening on 9002, then run:

```powershell
Get-Content .\sql\upgrade-boss-import.sql -Raw | mysql --default-character-set=utf8mb4 -uroot -proot ai_job_radar
```

Expected: `jobs` contains `source`, `external_id`, `source_url`, and `imported_at`.

- [ ] **Step 2: Run all automated tests**

```powershell
mvn -f ai-job-radar-parent/pom.xml test
npm --prefix ai-job-radar-web test
npm --prefix ai-job-radar-web run build
npm --prefix job-importer test
```

Expected: all four commands exit 0.

- [ ] **Step 3: Package and restart `job-service`**

```powershell
mvn -f ai-job-radar-parent/pom.xml -pl job-service -am -DskipTests package
```

Start the new JAR, wait for port 9002, and confirm one healthy `job-service` instance in Nacos.

- [ ] **Step 4: Run an idempotent fixture import twice**

Execute the two-item fixture command twice. Query MySQL by `source='BOSS'` and the fixture external IDs. Expected: exactly two rows exist; the second run reports updates rather than additional rows.

- [ ] **Step 5: Verify the user-visible business flow**

Run `node scripts/smoke-test.mjs`, open `/jobs`, and confirm:

- BOSS source badges are visible.
- Keyword search finds imported jobs.
- Existing local jobs remain visible.
- Favorite, application, AI match, and interview actions still work.
- No browser console errors occur.

- [ ] **Step 6: Perform a controlled real-site check**

Run live mode with `--limit 2`. The user manually completes login in the visible browser. If BOSS shows security verification or denies access, record the controlled stop as the correct result; do not attempt a bypass. If two visible jobs are read, verify their source URLs and import summary.

- [ ] **Step 7: Commit verification documentation if runtime instructions changed**

```powershell
git add ai-job-radar-course/README.md ai-job-radar-course/docs
git commit -m "docs: explain controlled BOSS imports"
```

### Task 9: Publish the course subtree

**Files:**
- Read: repository status and remote configuration

- [ ] **Step 1: Verify only intended files are tracked**

Run `git status --short` and ensure `.browser-data`, cookies, credentials, build outputs, and unrelated PPT working files are absent from the staged changes.

- [ ] **Step 2: Create a fast-forward course subtree publication commit**

Split `ai-job-radar-course`, create a commit whose parent is `course-public/main`, and verify the resulting tree contains `job-importer` but no browser state.

- [ ] **Step 3: Push and verify public main**

Push the publication commit to `course-public/main`, then verify the remote head and public repository visibility with GitHub CLI.
