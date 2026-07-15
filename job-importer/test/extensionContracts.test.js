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

    const request = await page.evaluate(async () => {
      await globalThis.RadarBridgeClient.importJobs([{ externalId: 'abc123' }])
      return globalThis.capturedRequest
    })

    assert.equal(request.url, 'http://127.0.0.1:9011/import')
    assert.equal(request.options.headers['X-Radar-Bridge'], '1')
    assert.deepEqual(JSON.parse(request.options.body), { jobs: [{ externalId: 'abc123' }] })
  } finally {
    await browser.close()
  }
})

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
