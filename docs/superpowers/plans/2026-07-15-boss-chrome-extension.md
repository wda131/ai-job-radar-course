# BOSS Chrome Extension Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local Manifest V3 Chrome extension and loopback Node.js bridge that import up to 50 real, user-visible BOSS jobs into the existing Spring Cloud course project without browser automation or credential extraction.

**Architecture:** A content script extracts jobs from the BOSS result-page DOM only after the user clicks the extension. The popup sends those jobs to a Node.js bridge bound to `127.0.0.1:9011`; the bridge reuses the existing normalizer and authenticated Gateway client, while `job-service` remains the authority for validation, MySQL upsert, Redis eviction, and best-effort Elasticsearch indexing.

**Tech Stack:** Chrome Manifest V3, plain HTML/CSS/JavaScript, Node.js 24 built-in HTTP and test runner, Playwright 1.49.1 fixture tests, Spring Boot 2.3.12.RELEASE, MyBatis Plus 3.3.1, Redis, Elasticsearch 7.6.2.

---

## File map

### New Chrome extension

- `boss-chrome-extension/manifest.json`: minimal Manifest V3 permissions and supported BOSS result pages.
- `boss-chrome-extension/extractor.js`: pure DOM extraction and external-ID deduplication.
- `boss-chrome-extension/content.js`: message boundary between popup and current BOSS tab.
- `boss-chrome-extension/bridge-client.js`: health and import calls to the loopback bridge.
- `boss-chrome-extension/popup.html`: compact status and action markup.
- `boss-chrome-extension/popup.css`: readable popup styling with the course cyan palette.
- `boss-chrome-extension/popup.js`: active-tab detection, extraction, confirmation, and result rendering.
- `boss-chrome-extension/icons/radar.svg`: local extension icon.

### Node bridge and tests

- `job-importer/src/bridge.js`: safe loopback HTTP server and authenticated import orchestration.
- `job-importer/test/extensionExtractor.test.js`: Playwright fixture extraction, deduplication, and 50-row cap.
- `job-importer/test/extensionContracts.test.js`: manifest permission and bridge-client behavior checks.
- `job-importer/test/bridge.test.js`: real local HTTP security, validation, and delegation tests.
- `job-importer/package.json`: add `bridge:boss` script.

### Course backend and operations

- `job-importer/src/normalizeJob.js`: increase the common bounded limit to 50.
- `job-importer/test/normalizeJob.test.js`: verify the new limit.
- `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/service/JobImportService.java`: accept at most 50 rows.
- `ai-job-radar-parent/job-service/src/test/java/cn/sdu/radar/service/JobImportServiceTest.java`: reject 51 rows.
- `scripts/start-boss-bridge.ps1`: environment and Gateway checks, then run the local bridge.
- `README.md`: Chrome installation, bridge startup, real import, validation, and fallback instructions.

---

### Task 1: Raise the shared import boundary to 50

**Files:**
- Modify: `job-importer/test/normalizeJob.test.js`
- Modify: `job-importer/src/normalizeJob.js`
- Modify: `ai-job-radar-parent/job-service/src/test/java/cn/sdu/radar/service/JobImportServiceTest.java`
- Modify: `ai-job-radar-parent/job-service/src/main/java/cn/sdu/radar/service/JobImportService.java`

- [ ] **Step 1: Change both tests to require the 50-row contract**

Change the Node assertion to:

```js
test('normalizes experience and clamps import limit', () => {
  assert.equal(parseExperience('3-5年'), 3)
  assert.equal(parseExperience('应届生'), 0)
  assert.equal(normalizeLimit('80'), 50)
  assert.equal(normalizeLimit('0'), 1)
})
```

Replace the Java oversized-batch test with:

```java
@Test
void rejectsBatchLargerThanFiftyRows() {
    List<JobImportDTO> jobs = new ArrayList<>();
    for (int index = 0; index < 51; index++) jobs.add(validJob("id-" + index));

    BusinessException exception = assertThrows(BusinessException.class,
            () -> service.importJobs(jobs));

    assertEquals(400, exception.getCode());
}
```

- [ ] **Step 2: Run focused tests and verify RED**

Run:

```powershell
npm --prefix job-importer test
mvn -f ai-job-radar-parent/pom.xml -pl job-service -am "-Dtest=JobImportServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: Node reports `20 !== 50`; Java does not reject a 51-row batch because its current cap is 20 only after the old 21-row test is replaced, so the new test still throws but its message remains stale. Add this message assertion before proceeding:

```java
assertEquals("每次必须导入 1 到 50 个岗位", exception.getMessage());
```

Expected after the message assertion: FAIL with `每次必须导入 1 到 20 个岗位`.

- [ ] **Step 3: Implement the new constants**

In `normalizeJob.js`:

```js
export function normalizeLimit(value) {
  const parsed = Number.parseInt(value || '10', 10)
  return Math.min(50, Math.max(1, Number.isFinite(parsed) ? parsed : 10))
}
```

In `JobImportService.java`:

```java
private static final int MAX_BATCH_SIZE = 50;
```

and:

```java
throw new BusinessException(400, "每次必须导入 1 到 50 个岗位");
```

- [ ] **Step 4: Run focused tests and verify GREEN**

Run the two commands from Step 2.

Expected: importer tests and all `JobImportServiceTest` methods pass.

- [ ] **Step 5: Commit**

```powershell
git add ai-job-radar-course/job-importer ai-job-radar-course/ai-job-radar-parent/job-service
git commit -m "feat: allow fifty-job imports"
```

### Task 2: Extract real jobs from the current Chrome tab

**Files:**
- Create: `boss-chrome-extension/extractor.js`
- Create: `job-importer/test/extensionExtractor.test.js`
- Read: `job-importer/test/fixtures/boss-results.html`

- [ ] **Step 1: Write a failing real-browser extractor test**

Create `extensionExtractor.test.js`:

```js
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright'

const extensionScript = fileURLToPath(new URL('../../boss-chrome-extension/extractor.js', import.meta.url))
const fixture = new URL('./fixtures/boss-results.html', import.meta.url)

test('extracts real BOSS card fields from the current DOM', async () => {
  const browser = await chromium.launch({ headless: true })
  try {
    const page = await browser.newPage()
    await page.setContent(await readFile(fixture, 'utf8'))
    await page.addScriptTag({ path: extensionScript })
    const jobs = await page.evaluate(() => globalThis.RadarBossExtractor.extractBossJobs(document, 50))
    assert.equal(jobs.length, 2)
    assert.equal(jobs[0].externalId, 'abc123')
    assert.equal(jobs[0].title, 'Java 开发工程师')
    assert.equal(jobs[0].company, '海纳科技')
    assert.equal(jobs[0].sourceUrl, 'https://www.zhipin.com/job_detail/abc123.html')
  } finally {
    await browser.close()
  }
})

test('deduplicates external ids and caps the loaded page at fifty jobs', async () => {
  const browser = await chromium.launch({ headless: true })
  try {
    const page = await browser.newPage()
    const cards = Array.from({ length: 55 }, (_, index) => `
      <li class="job-card-wrapper">
        <a class="job-card-left" href="https://www.zhipin.com/job_detail/id-${index}.html">
          <span class="job-name">岗位 ${index}</span><span class="salary">10-15K</span>
        </a><span class="company-name">公司 ${index}</span><span class="job-area">威海</span>
      </li>`).join('')
    await page.setContent(`<ul class="job-list-box">${cards}${cards.slice(0, cards.indexOf('</li>') + 5)}</ul>`)
    await page.addScriptTag({ path: extensionScript })
    const jobs = await page.evaluate(() => globalThis.RadarBossExtractor.extractBossJobs(document, 50))
    assert.equal(jobs.length, 50)
    assert.equal(new Set(jobs.map(job => job.externalId)).size, 50)
  } finally {
    await browser.close()
  }
})
```

- [ ] **Step 2: Run the test and verify RED**

Run: `npm --prefix job-importer test`

Expected: FAIL because `boss-chrome-extension/extractor.js` does not exist.

- [ ] **Step 3: Implement the pure DOM extractor**

Create `extractor.js`:

```js
(function attachBossExtractor(root) {
  const CARD_SELECTOR = '.job-card-wrapper, .job-card-box, .job-list-box > li, [class*="job-card-wrapper"]'

  const clean = value => String(value || '').replace(/\s+/g, ' ').trim()
  const text = (node, selector) => clean(node.querySelector(selector)?.textContent)
  const texts = (node, selector) => Array.from(node.querySelectorAll(selector))
    .map(item => clean(item.textContent)).filter(Boolean)

  function externalIdFromUrl(url) {
    return String(url || '').match(/\/job_detail\/([^/?#]+?)(?:\.html)?(?:[?#]|$)/)?.[1] || ''
  }

  function extractBossJobs(documentRoot, requestedLimit = 50) {
    const limit = Math.min(50, Math.max(1, Number(requestedLimit) || 50))
    const seen = new Set()
    const jobs = []
    for (const card of documentRoot.querySelectorAll(CARD_SELECTOR)) {
      if (jobs.length >= limit) break
      const link = card.querySelector('a.job-card-left, a[href*="/job_detail/"]')
      if (!link) continue
      const sourceUrl = link.href || link.getAttribute('href') || ''
      const externalId = externalIdFromUrl(sourceUrl)
      if (!externalId || seen.has(externalId)) continue
      const jobInfo = texts(card, '.job-info li, .job-info span')
      const tags = texts(card, '.tag-list li, .job-card-footer li, [class*="tag-list"] li')
      const requirements = tags.join(' ') || '完整要求请查看 BOSS 来源链接'
      jobs.push({
        externalId,
        title: text(card, '.job-name'),
        company: text(card, '.company-name'),
        city: text(card, '.job-area'),
        salary: text(card, '.salary'),
        experience: jobInfo[0] || '',
        education: jobInfo[1] || '',
        description: '来自 BOSS 当前可见岗位列表，完整信息请查看来源链接',
        requirements,
        welfareTags: tags,
        sourceUrl
      })
      seen.add(externalId)
    }
    return jobs.filter(job => job.title && job.company)
  }

  root.RadarBossExtractor = { extractBossJobs, externalIdFromUrl }
})(globalThis)
```

- [ ] **Step 4: Run importer tests and verify GREEN**

Run: `npm --prefix job-importer test`

Expected: existing tests plus the two extension extractor tests pass.

- [ ] **Step 5: Commit**

```powershell
git add ai-job-radar-course/boss-chrome-extension/extractor.js ai-job-radar-course/job-importer/test/extensionExtractor.test.js
git commit -m "feat: extract jobs from BOSS Chrome pages"
```

### Task 3: Define the minimal extension permissions and page-message boundary

**Files:**
- Create: `boss-chrome-extension/manifest.json`
- Create: `boss-chrome-extension/content.js`
- Create: `boss-chrome-extension/bridge-client.js`
- Create: `job-importer/test/extensionContracts.test.js`

- [ ] **Step 1: Write failing permission and bridge-client tests**

Create `extensionContracts.test.js`:

```js
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright'

const extensionRoot = new URL('../../boss-chrome-extension/', import.meta.url)

test('manifest has only the required BOSS and loopback permissions', async () => {
  const manifest = JSON.parse(await readFile(new URL('manifest.json', extensionRoot), 'utf8'))
  assert.equal(manifest.manifest_version, 3)
  assert.deepEqual(manifest.permissions, ['activeTab'])
  assert.deepEqual(manifest.host_permissions, [
    'https://www.zhipin.com/*',
    'http://127.0.0.1:9011/*'
  ])
  assert.deepEqual(manifest.content_scripts[0].js, ['extractor.js', 'content.js'])
  assert.ok(!JSON.stringify(manifest).includes('cookies'))
  assert.ok(!JSON.stringify(manifest).includes('webRequest'))
})

test('bridge client sends only jobs and the CSRF-resistant custom header', async () => {
  const browser = await chromium.launch({ headless: true })
  try {
    const page = await browser.newPage()
    await page.setContent('<main></main>')
    await page.evaluate(() => {
      globalThis.capturedRequest = null
      globalThis.fetch = async (url, options = {}) => {
        globalThis.capturedRequest = { url, options }
        return { ok: true, json: async () => ({ code: 200, data: { created: 1 } }) }
      }
    })
    await page.addScriptTag({ path: fileURLToPath(new URL('bridge-client.js', extensionRoot)) })
    const result = await page.evaluate(async () => {
      await globalThis.RadarBridgeClient.importJobs([{ externalId: 'abc123' }])
      return globalThis.capturedRequest
    })
    assert.equal(result.url, 'http://127.0.0.1:9011/import')
    assert.equal(result.options.headers['X-Radar-Bridge'], '1')
    assert.deepEqual(JSON.parse(result.options.body), { jobs: [{ externalId: 'abc123' }] })
  } finally {
    await browser.close()
  }
})
```

- [ ] **Step 2: Run importer tests and verify RED**

Run: `npm --prefix job-importer test`

Expected: FAIL because the manifest and bridge client are missing.

- [ ] **Step 3: Create the extension manifest**

```json
{
  "manifest_version": 3,
  "name": "AI 求职雷达 BOSS 导入器",
  "version": "1.0.0",
  "description": "将普通 Chrome 当前页面中已加载的 BOSS 岗位导入本地课程项目。",
  "permissions": ["activeTab"],
  "host_permissions": [
    "https://www.zhipin.com/*",
    "http://127.0.0.1:9011/*"
  ],
  "action": {
    "default_title": "导入当前 BOSS 岗位",
    "default_popup": "popup.html",
    "default_icon": "icons/radar.svg"
  },
  "icons": {
    "16": "icons/radar.svg",
    "48": "icons/radar.svg",
    "128": "icons/radar.svg"
  },
  "content_scripts": [
    {
      "matches": [
        "https://www.zhipin.com/web/geek/job*",
        "https://www.zhipin.com/web/geek/jobs*"
      ],
      "js": ["extractor.js", "content.js"],
      "run_at": "document_idle"
    }
  ]
}
```

- [ ] **Step 4: Implement the content-script message boundary**

```js
chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  if (message?.type !== 'RADAR_EXTRACT_BOSS_JOBS') return false
  try {
    const jobs = globalThis.RadarBossExtractor.extractBossJobs(document, message.limit || 50)
    sendResponse({ ok: true, jobs })
  } catch (error) {
    sendResponse({ ok: false, message: error.message || '岗位解析失败' })
  }
  return false
})
```

- [ ] **Step 5: Implement the loopback bridge client**

```js
(function attachBridgeClient(root) {
  const BASE_URL = 'http://127.0.0.1:9011'

  async function request(path, options = {}) {
    const response = await fetch(`${BASE_URL}${path}`, options)
    const payload = await response.json().catch(() => ({ message: '本地桥接返回无法解析的响应' }))
    if (!response.ok || payload.code !== 200) {
      throw new Error(payload.message || `本地桥接请求失败（HTTP ${response.status}）`)
    }
    return payload.data
  }

  const health = () => request('/health')
  const importJobs = jobs => request('/import', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Radar-Bridge': '1' },
    body: JSON.stringify({ jobs })
  })

  root.RadarBridgeClient = { health, importJobs }
})(globalThis)
```

- [ ] **Step 6: Run tests and verify GREEN**

Run: `npm --prefix job-importer test`

Expected: manifest and bridge-client tests pass; no forbidden permission is present.

- [ ] **Step 7: Commit**

```powershell
git add ai-job-radar-course/boss-chrome-extension ai-job-radar-course/job-importer/test/extensionContracts.test.js
git commit -m "feat: define safe BOSS Chrome extension"
```

### Task 4: Add the secured loopback import bridge

**Files:**
- Create: `job-importer/src/bridge.js`
- Create: `job-importer/test/bridge.test.js`
- Modify: `job-importer/package.json`

- [ ] **Step 1: Write failing real-HTTP bridge tests**

Create `bridge.test.js` with these helpers and cases:

```js
import assert from 'node:assert/strict'
import test from 'node:test'
import { createBossBridge } from '../src/bridge.js'

async function startBridge(importer = async jobs => ({ created: jobs.length, updated: 0, rejected: 0, errors: [] })) {
  const server = createBossBridge({
    env: { API_BASE_URL: 'http://127.0.0.1:9000', RADAR_USERNAME: 'student', RADAR_PASSWORD: 'secret-value' },
    importer
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  return { server, baseUrl: `http://127.0.0.1:${server.address().port}` }
}

const validRawJob = index => ({
  externalId: `id-${index}`,
  title: `Java 岗位 ${index}`,
  company: `公司 ${index}`,
  city: '威海', salary: '10-15K', experience: '1-3年', education: '本科',
  description: '来自 BOSS 当前可见岗位列表，完整信息请查看来源链接',
  requirements: 'Java Spring Boot', welfareTags: ['Java'],
  sourceUrl: `https://www.zhipin.com/job_detail/id-${index}.html`
})

test('reports a non-sensitive health response', async () => {
  const { server, baseUrl } = await startBridge()
  try {
    const response = await fetch(`${baseUrl}/health`)
    assert.equal(response.status, 200)
    assert.deepEqual((await response.json()).data, { service: 'boss-import-bridge', limit: 50 })
  } finally { await new Promise(resolve => server.close(resolve)) }
})

test('rejects ordinary web origins and missing bridge headers', async () => {
  const { server, baseUrl } = await startBridge()
  try {
    const ordinary = await fetch(`${baseUrl}/import`, {
      method: 'POST', headers: { Origin: 'http://evil.example', 'X-Radar-Bridge': '1' },
      body: JSON.stringify({ jobs: [validRawJob(1)] })
    })
    assert.equal(ordinary.status, 403)
    const missingHeader = await fetch(`${baseUrl}/import`, {
      method: 'POST', body: JSON.stringify({ jobs: [validRawJob(1)] })
    })
    assert.equal(missingHeader.status, 403)
  } finally { await new Promise(resolve => server.close(resolve)) }
})

test('normalizes and delegates extension jobs', async () => {
  let received
  const { server, baseUrl } = await startBridge(async jobs => {
    received = jobs
    return { created: 1, updated: 0, rejected: 0, errors: [] }
  })
  try {
    const response = await fetch(`${baseUrl}/import`, {
      method: 'POST',
      headers: { Origin: 'chrome-extension://test-id', 'Content-Type': 'application/json', 'X-Radar-Bridge': '1' },
      body: JSON.stringify({ jobs: [validRawJob(1)] })
    })
    assert.equal(response.status, 200)
    assert.equal(received[0].source, 'BOSS')
    assert.equal(received[0].salaryMin, 10000)
  } finally { await new Promise(resolve => server.close(resolve)) }
})

test('rejects more than fifty jobs', async () => {
  const { server, baseUrl } = await startBridge()
  try {
    const response = await fetch(`${baseUrl}/import`, {
      method: 'POST',
      headers: { Origin: 'chrome-extension://test-id', 'Content-Type': 'application/json', 'X-Radar-Bridge': '1' },
      body: JSON.stringify({ jobs: Array.from({ length: 51 }, (_, index) => validRawJob(index)) })
    })
    assert.equal(response.status, 400)
  } finally { await new Promise(resolve => server.close(resolve)) }
})

test('redacts the configured password from failures', async () => {
  const { server, baseUrl } = await startBridge(async () => { throw new Error('login failed: secret-value') })
  try {
    const response = await fetch(`${baseUrl}/import`, {
      method: 'POST',
      headers: { Origin: 'chrome-extension://test-id', 'Content-Type': 'application/json', 'X-Radar-Bridge': '1' },
      body: JSON.stringify({ jobs: [validRawJob(1)] })
    })
    const text = await response.text()
    assert.equal(response.status, 502)
    assert.ok(!text.includes('secret-value'))
  } finally { await new Promise(resolve => server.close(resolve)) }
})
```

- [ ] **Step 2: Run tests and verify RED**

Run: `npm --prefix job-importer test`

Expected: FAIL because `src/bridge.js` does not exist.

- [ ] **Step 3: Implement the bridge server**

Implement these exact boundaries in `bridge.js`:

```js
import { createServer } from 'node:http'
import { pathToFileURL } from 'node:url'
import { loginAndImport } from './importClient.js'
import { normalizeJob } from './normalizeJob.js'

const json = (response, status, payload, origin) => {
  response.statusCode = status
  response.setHeader('Content-Type', 'application/json;charset=utf-8')
  response.setHeader('Vary', 'Origin')
  if (origin?.startsWith('chrome-extension://')) response.setHeader('Access-Control-Allow-Origin', origin)
  response.end(JSON.stringify(payload))
}

const readJson = async request => {
  const chunks = []
  let bytes = 0
  for await (const chunk of request) {
    bytes += chunk.length
    if (bytes > 1024 * 1024) throw new Error('请求数据超过 1MB')
    chunks.push(chunk)
  }
  return JSON.parse(Buffer.concat(chunks).toString('utf8') || '{}')
}

const safeMessage = (error, env) => {
  let message = String(error?.message || '导入失败')
  for (const secret of [env.RADAR_PASSWORD].filter(Boolean)) message = message.split(secret).join('[REDACTED]')
  return message
}

export function createBossBridge({ env = process.env, importer } = {}) {
  const delegate = importer || (jobs => loginAndImport({
    baseUrl: env.API_BASE_URL || 'http://127.0.0.1:9000',
    username: env.RADAR_USERNAME,
    password: env.RADAR_PASSWORD,
    jobs
  }))
  return createServer(async (request, response) => {
    const origin = request.headers.origin || ''
    if (request.method === 'OPTIONS') {
      if (!origin.startsWith('chrome-extension://')) return json(response, 403, { code: 403, message: '来源不允许' })
      response.setHeader('Access-Control-Allow-Origin', origin)
      response.setHeader('Access-Control-Allow-Methods', 'GET,POST,OPTIONS')
      response.setHeader('Access-Control-Allow-Headers', 'Content-Type,X-Radar-Bridge')
      response.statusCode = 204
      return response.end()
    }
    if (request.method === 'GET' && request.url === '/health') {
      return json(response, 200, { code: 200, data: { service: 'boss-import-bridge', limit: 50 } }, origin)
    }
    if (request.method !== 'POST' || request.url !== '/import') {
      return json(response, 404, { code: 404, message: '接口不存在' }, origin)
    }
    if ((origin && !origin.startsWith('chrome-extension://')) || request.headers['x-radar-bridge'] !== '1') {
      return json(response, 403, { code: 403, message: '来源不允许' }, origin)
    }
    try {
      const body = await readJson(request)
      if (!Array.isArray(body.jobs) || body.jobs.length < 1 || body.jobs.length > 50) {
        return json(response, 400, { code: 400, message: '每次必须导入 1 到 50 个岗位' }, origin)
      }
      const errors = []
      const jobs = []
      body.jobs.forEach((raw, index) => {
        try { jobs.push(normalizeJob(raw)) }
        catch (error) { errors.push(`第 ${index + 1} 条：${error.message}`) }
      })
      if (!jobs.length) return json(response, 400, { code: 400, message: errors[0] || '没有有效岗位' }, origin)
      const result = await delegate(jobs)
      return json(response, 200, { code: 200, data: { ...result, errors: [...errors, ...(result.errors || [])] } }, origin)
    } catch (error) {
      return json(response, 502, { code: 502, message: safeMessage(error, env) }, origin)
    }
  })
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  if (!process.env.RADAR_USERNAME || !process.env.RADAR_PASSWORD) {
    console.error('请先设置 RADAR_USERNAME 和 RADAR_PASSWORD')
    process.exitCode = 1
  } else {
    createBossBridge().listen(9011, '127.0.0.1', () => {
      console.log('BOSS 导入桥接已启动：http://127.0.0.1:9011')
    })
  }
}
```

- [ ] **Step 4: Add the package script**

Add to `package.json`:

```json
"bridge:boss": "node src/bridge.js"
```

- [ ] **Step 5: Run tests and verify GREEN**

Run: `npm --prefix job-importer test`

Expected: bridge HTTP tests and all existing importer tests pass.

- [ ] **Step 6: Commit**

```powershell
git add ai-job-radar-course/job-importer
git commit -m "feat: add secured local BOSS import bridge"
```

### Task 5: Build the extension popup and local icon

**Files:**
- Create: `boss-chrome-extension/popup.html`
- Create: `boss-chrome-extension/popup.css`
- Create: `boss-chrome-extension/popup.js`
- Create: `boss-chrome-extension/icons/radar.svg`
- Modify: `job-importer/test/extensionContracts.test.js`

- [ ] **Step 1: Add a failing popup contract test**

Append:

```js
test('popup detects before import and never requests BOSS credentials', async () => {
  const html = await readFile(new URL('popup.html', extensionRoot), 'utf8')
  const script = await readFile(new URL('popup.js', extensionRoot), 'utf8')
  assert.match(html, /id="detect"/)
  assert.match(html, /id="import"/)
  assert.match(html, /id="status"/)
  assert.match(script, /RADAR_EXTRACT_BOSS_JOBS/)
  assert.match(script, /RadarBridgeClient\.importJobs/)
  assert.doesNotMatch(`${html}\n${script}`, /cookie|password|token/i)
})
```

- [ ] **Step 2: Run tests and verify RED**

Run: `npm --prefix job-importer test`

Expected: FAIL because popup files are missing.

- [ ] **Step 3: Create the popup markup**

```html
<!doctype html>
<html lang="zh-CN">
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><link rel="stylesheet" href="popup.css"><title>BOSS 岗位导入</title></head>
<body>
  <main>
    <header><img src="icons/radar.svg" alt=""><div><strong>AI 求职雷达</strong><small>当前页面真实岗位导入</small></div></header>
    <p id="status" class="status">正在检查本地桥接程序…</p>
    <p id="count" class="count">尚未检测岗位</p>
    <div class="actions">
      <button id="detect" type="button">检测当前页面</button>
      <button id="import" type="button" disabled>导入当前页面</button>
    </div>
    <p class="hint">请先正常登录 BOSS 并滚动加载岗位，单次最多 50 条。</p>
  </main>
  <script src="bridge-client.js"></script><script src="popup.js"></script>
</body>
</html>
```

- [ ] **Step 4: Create compact popup behavior**

`popup.js` keeps `detectedJobs` only in popup memory, calls `health()` on load, sends `RADAR_EXTRACT_BOSS_JOBS` to the active tab, and enables import only when jobs exist. On success it renders:

```text
导入完成：新增 N，更新 N，拒绝 N
```

Use this exact active-tab helper:

```js
const [tab] = await chrome.tabs.query({ active: true, currentWindow: true })
if (!tab?.id || !tab.url?.startsWith('https://www.zhipin.com/web/geek/job')) {
  throw new Error('请打开 BOSS 岗位搜索结果页')
}
const result = await chrome.tabs.sendMessage(tab.id, { type: 'RADAR_EXTRACT_BOSS_JOBS', limit: 50 })
```

The import handler calls only:

```js
const result = await globalThis.RadarBridgeClient.importJobs(detectedJobs)
```

- [ ] **Step 5: Add CSS and SVG**

Create a 340px-wide popup with dark navy background, cyan borders, 14px body text, clear disabled state, and no animation. Create a simple SVG radar circle using only local vector elements and colors `#21d4c2`, `#163f46`, and `#07151d`.

- [ ] **Step 6: Run tests and verify GREEN**

Run: `npm --prefix job-importer test`

Expected: popup contract and all extension tests pass.

- [ ] **Step 7: Load the unpacked extension and inspect it**

Open `chrome://extensions`, enable developer mode, choose “加载已解压的扩展程序”, and select the absolute `boss-chrome-extension` directory. Expected: Chrome reports no manifest errors and the popup shows a bridge-offline message until Task 6 is started.

- [ ] **Step 8: Commit**

```powershell
git add ai-job-radar-course/boss-chrome-extension ai-job-radar-course/job-importer/test/extensionContracts.test.js
git commit -m "feat: add BOSS import extension popup"
```

### Task 6: Add the bridge launcher and demonstration instructions

**Files:**
- Create: `scripts/start-boss-bridge.ps1`
- Modify: `README.md`
- Modify: `ai-job-radar-web/test/ui-state.test.js`

- [ ] **Step 1: Write a failing launcher/documentation regression test**

Append to `ui-state.test.js`:

```js
test('documents the Chrome extension bridge without exposing BOSS credentials', () => {
  const launcher = readFileSync(new URL('../../scripts/start-boss-bridge.ps1', import.meta.url), 'utf8')
  const readme = readFileSync(new URL('../../README.md', import.meta.url), 'utf8')
  assert.match(launcher, /bridge:boss/)
  assert.match(launcher, /127\.0\.0\.1/)
  assert.match(readme, /chrome:\/\/extensions/)
  assert.match(readme, /加载已解压的扩展程序/)
  assert.match(readme, /最多 50 条/)
  assert.doesNotMatch(launcher, /zhipin.*cookie|zp_token/i)
})
```

- [ ] **Step 2: Run frontend tests and verify RED**

Run: `npm --prefix ai-job-radar-web test`

Expected: FAIL because `start-boss-bridge.ps1` is missing.

- [ ] **Step 3: Create the ASCII-compatible PowerShell launcher**

The script must contain only ASCII so Windows PowerShell 5.1 does not misread UTF-8 text. It:

```powershell
$ErrorActionPreference = "Stop"
$project = Split-Path -Parent $PSScriptRoot
$importer = Join-Path $project "job-importer"
if (-not (Get-Command node -ErrorAction SilentlyContinue)) { throw "Node.js was not found." }
if (-not (Test-Path (Join-Path $importer "node_modules"))) {
    & npm.cmd --prefix $importer install
    if ($LASTEXITCODE -ne 0) { throw "Failed to install job-importer dependencies." }
}
if (-not (Get-NetTCPConnection -State Listen -LocalPort 9000 -ErrorAction SilentlyContinue)) {
    throw "Gateway port 9000 is not listening."
}
if (-not $env:RADAR_USERNAME -or -not $env:RADAR_PASSWORD) {
    throw "Set RADAR_USERNAME and RADAR_PASSWORD first."
}
$env:API_BASE_URL = if ($env:API_BASE_URL) { $env:API_BASE_URL } else { "http://127.0.0.1:9000" }
& npm.cmd --prefix $importer run bridge:boss
exit $LASTEXITCODE
```

- [ ] **Step 4: Add exact README instructions**

Document:

1. Start services and set `RADAR_USERNAME`, `RADAR_PASSWORD`, and optional `API_BASE_URL`.
2. Run `.\scripts\start-boss-bridge.ps1` and keep the terminal open.
3. Open `chrome://extensions`, enable developer mode, and load `boss-chrome-extension/` unpacked.
4. Use ordinary Chrome to log in to BOSS, open a result page, and scroll.
5. Click the extension, detect jobs, then import up to 50.
6. Refresh `http://127.0.0.1:5174/jobs`.
7. Explain that no Cookie/token, automation login, internal API, auto-page, chat, or application action is used.
8. Keep the fixture command as the offline demonstration fallback.

- [ ] **Step 5: Run frontend tests and build**

```powershell
npm --prefix ai-job-radar-web test
npm --prefix ai-job-radar-web run build
```

Expected: all tests pass and Vite exits 0.

- [ ] **Step 6: Commit**

```powershell
git add ai-job-radar-course/scripts/start-boss-bridge.ps1 ai-job-radar-course/README.md ai-job-radar-course/ai-job-radar-web/test/ui-state.test.js
git commit -m "docs: add Chrome import demonstration flow"
```

### Task 7: Live local integration and idempotency verification

**Files:**
- Read: `boss-chrome-extension/manifest.json`
- Read: `scripts/start-boss-bridge.ps1`
- Read: `sql/upgrade-boss-import.sql`

- [ ] **Step 1: Run all automated verification**

```powershell
mvn -f ai-job-radar-parent/pom.xml test
npm --prefix ai-job-radar-web test
npm --prefix ai-job-radar-web run build
npm --prefix job-importer test
```

Expected: zero failures across all commands.

- [ ] **Step 2: Start the bridge against the running Gateway**

In a dedicated terminal:

```powershell
$env:API_BASE_URL='http://127.0.0.1:9000'
$env:RADAR_USERNAME='student'
$env:RADAR_PASSWORD='123456'
.\scripts\start-boss-bridge.ps1
```

Expected: `BOSS 导入桥接已启动：http://127.0.0.1:9011` and only `127.0.0.1:9011` is listening.

- [ ] **Step 3: Verify the bridge with an extension-shaped fixture request**

Use PowerShell to POST two fixture jobs with:

```powershell
$headers = @{ Origin='chrome-extension://manual-test'; 'X-Radar-Bridge'='1' }
$body = @{ jobs = @(
  @{ externalId='chrome-real-1'; title='Chrome真实导入测试1'; company='测试公司'; city='威海'; salary='10-15K'; experience='1-3年'; education='本科'; description='来自 BOSS 当前可见岗位列表，完整信息请查看来源链接'; requirements='Java'; welfareTags=@('Java'); sourceUrl='https://www.zhipin.com/job_detail/chrome-real-1.html' },
  @{ externalId='chrome-real-2'; title='Chrome真实导入测试2'; company='测试公司'; city='青岛'; salary='12-18K'; experience='3-5年'; education='本科'; description='来自 BOSS 当前可见岗位列表，完整信息请查看来源链接'; requirements='Vue3'; welfareTags=@('Vue3'); sourceUrl='https://www.zhipin.com/job_detail/chrome-real-2.html' }
) } | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:9011/import' -Headers $headers -ContentType 'application/json' -Body $body
```

Run twice. Expected: first response creates two; second updates two; MySQL contains exactly two matching external IDs.

- [ ] **Step 4: Manually verify the ordinary-Chrome flow**

Load the unpacked extension, use the user's normal authenticated BOSS tab, scroll to load cards, click detect, and import. Expected:

- Popup reports the detected count, no more than 50.
- No automated browser window is opened.
- MySQL rows have real BOSS external IDs and source URLs.
- Refreshing `/jobs` shows BOSS source badges.
- Repeating import updates existing rows.

If BOSS is unavailable in ordinary Chrome, record the external-site condition and verify the extension against `job-importer/test/fixtures/boss-results.html`; do not add anti-detection.

- [ ] **Step 5: Run the existing business smoke test**

Run: `node scripts/smoke-test.mjs`

Expected: jobs, keyword search, AI match, and four-question interview all succeed.

- [ ] **Step 6: Verify repository hygiene**

Run:

```powershell
git status --short
git ls-files | rg "(^|/)(\.env|node_modules|target|dist|runtime-logs)(/|$)|cookie|storageState|browser-data"
```

Expected: only known unrelated PPT files are untracked; the sensitive-file query returns no tracked files.

### Task 8: Publish the course subtree

**Files:**
- Read: repository status and `course-public` remote state.

- [ ] **Step 1: Create a safe fast-forward publication commit**

From the repository top level:

```powershell
git fetch course-public main
$remoteHead = git rev-parse course-public/main
$courseTree = git rev-parse HEAD:ai-job-radar-course
$files = git ls-tree -r --name-only $courseTree
if (-not ($files -contains 'boss-chrome-extension/manifest.json')) { throw 'extension missing' }
$forbidden = @($files | Where-Object { $_ -match '(^|/)(\.env|node_modules|target|dist|runtime-logs)(/|$)|cookie|storageState|browser-data' })
if ($forbidden.Count) { throw "forbidden files: $($forbidden -join ', ')" }
$publishCommit = 'feat: add ordinary Chrome BOSS imports' | git commit-tree $courseTree -p $remoteHead
git push course-public "$publishCommit`:refs/heads/main"
```

- [ ] **Step 2: Verify public repository state**

```powershell
git ls-remote course-public refs/heads/main
gh repo view wda131/ai-job-radar-course --json url,visibility,defaultBranchRef
```

Expected: remote main equals the publication commit and visibility is `PUBLIC`.
