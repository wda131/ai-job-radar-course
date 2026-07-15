import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { chromium } from 'playwright'
import { extractBossCards } from '../src/bossParser.js'

test('extracts visible BOSS cards from a sanitized page', async () => {
  const browser = await chromium.launch({ headless: true })
  try {
    const page = await browser.newPage()
    await page.setContent(await readFile(new URL('./fixtures/boss-results.html', import.meta.url), 'utf8'))
    const jobs = await extractBossCards(page, '威海', 20)
    assert.equal(jobs.length, 2)
    assert.equal(jobs[0].externalId, 'abc123')
    assert.equal(jobs[0].salary, '10-16K')
  } finally {
    await browser.close()
  }
})
