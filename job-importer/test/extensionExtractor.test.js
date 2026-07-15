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
    assert.equal(jobs[0].city, '威海')
    assert.equal(jobs[0].sourceUrl, 'https://www.zhipin.com/job_detail/abc123.html')
  } finally {
    await browser.close()
  }
})

test('deduplicates external ids and caps the loaded page at fifty jobs', async () => {
  const browser = await chromium.launch({ headless: true })
  try {
    const page = await browser.newPage()
    const card = index => `
      <li class="job-card-wrapper">
        <a class="job-card-left" href="https://www.zhipin.com/job_detail/id-${index}.html">
          <span class="job-name">岗位 ${index}</span><span class="salary">10-15K</span>
        </a>
        <span class="company-name">公司 ${index}</span><span class="job-area">威海</span>
      </li>`
    const cards = Array.from({ length: 55 }, (_, index) => card(index)).join('')
    await page.setContent(`<ul class="job-list-box">${card(0)}${cards}</ul>`)
    await page.addScriptTag({ path: extensionScript })

    const jobs = await page.evaluate(() => globalThis.RadarBossExtractor.extractBossJobs(document, 50))

    assert.equal(jobs.length, 50)
    assert.equal(new Set(jobs.map(job => job.externalId)).size, 50)
  } finally {
    await browser.close()
  }
})

test('extracts the current BOSS card layout with boss and location fields', async () => {
  const browser = await chromium.launch({ headless: true })
  try {
    const page = await browser.newPage()
    await page.setContent(`
      <ul class="rec-job-list">
        <li class="job-card-box">
          <div class="job-info">
            <div class="job-title clearfix">
              <a class="job-name" href="https://www.zhipin.com/job_detail/current123.html">AI Agent 开发工程师</a>
              <span class="job-salary">\uE032\uE036-\uE033\uE031K</span>
            </div>
            <ul class="tag-list"><li>1-3年</li><li>本科</li></ul>
          </div>
          <div class="job-card-footer">
            <a class="boss-info"><span class="boss-name">当前页面公司</span></a>
            <span class="company-location">济南·历下区</span>
          </div>
        </li>
      </ul>`)
    await page.addScriptTag({ path: extensionScript })

    const jobs = await page.evaluate(() => globalThis.RadarBossExtractor.extractBossJobs(document, 50))

    assert.equal(jobs.length, 1)
    assert.equal(jobs[0].company, '当前页面公司')
    assert.equal(jobs[0].city, '济南·历下区')
    assert.equal(jobs[0].salary, '15-20K')
    assert.equal(jobs[0].experience, '1-3年')
    assert.equal(jobs[0].education, '本科')
  } finally {
    await browser.close()
  }
})
