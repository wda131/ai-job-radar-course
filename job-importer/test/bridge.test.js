import assert from 'node:assert/strict'
import test from 'node:test'
import { createBossBridge } from '../src/bridge.js'

async function startBridge(importer = async jobs => ({ created: jobs.length, updated: 0, rejected: 0, errors: [] })) {
  const server = createBossBridge({
    env: {
      API_BASE_URL: 'http://127.0.0.1:9000',
      RADAR_USERNAME: 'student',
      RADAR_PASSWORD: 'secret-value'
    },
    importer
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  return { server, baseUrl: `http://127.0.0.1:${server.address().port}` }
}

const validRawJob = index => ({
  externalId: `id-${index}`,
  title: `Java 岗位 ${index}`,
  company: `公司 ${index}`,
  city: '威海',
  salary: '10-15K',
  experience: '1-3年',
  education: '本科',
  description: '来自 BOSS 当前可见岗位列表，完整信息请查看来源链接',
  requirements: 'Java Spring Boot',
  welfareTags: ['Java'],
  sourceUrl: `https://www.zhipin.com/job_detail/id-${index}.html`
})

test('reports a non-sensitive health response', async () => {
  const { server, baseUrl } = await startBridge()
  try {
    const response = await fetch(`${baseUrl}/health`)
    assert.equal(response.status, 200)
    assert.deepEqual((await response.json()).data, { service: 'boss-import-bridge', limit: 50 })
  } finally {
    await new Promise(resolve => server.close(resolve))
  }
})

test('rejects ordinary web origins and missing bridge headers', async () => {
  const { server, baseUrl } = await startBridge()
  try {
    const ordinary = await fetch(`${baseUrl}/import`, {
      method: 'POST',
      headers: { Origin: 'http://evil.example', 'X-Radar-Bridge': '1' },
      body: JSON.stringify({ jobs: [validRawJob(1)] })
    })
    assert.equal(ordinary.status, 403)

    const missingHeader = await fetch(`${baseUrl}/import`, {
      method: 'POST',
      body: JSON.stringify({ jobs: [validRawJob(1)] })
    })
    assert.equal(missingHeader.status, 403)
  } finally {
    await new Promise(resolve => server.close(resolve))
  }
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
      headers: {
        Origin: 'chrome-extension://test-id',
        'Content-Type': 'application/json',
        'X-Radar-Bridge': '1'
      },
      body: JSON.stringify({ jobs: [validRawJob(1)] })
    })
    assert.equal(response.status, 200)
    assert.equal(received[0].source, 'BOSS')
    assert.equal(received[0].salaryMin, 10000)
  } finally {
    await new Promise(resolve => server.close(resolve))
  }
})

test('rejects more than fifty jobs', async () => {
  const { server, baseUrl } = await startBridge()
  try {
    const response = await fetch(`${baseUrl}/import`, {
      method: 'POST',
      headers: {
        Origin: 'chrome-extension://test-id',
        'Content-Type': 'application/json',
        'X-Radar-Bridge': '1'
      },
      body: JSON.stringify({ jobs: Array.from({ length: 51 }, (_, index) => validRawJob(index)) })
    })
    assert.equal(response.status, 400)
  } finally {
    await new Promise(resolve => server.close(resolve))
  }
})

test('redacts the configured password from failures', async () => {
  const { server, baseUrl } = await startBridge(async () => {
    throw new Error('login failed: secret-value')
  })
  try {
    const response = await fetch(`${baseUrl}/import`, {
      method: 'POST',
      headers: {
        Origin: 'chrome-extension://test-id',
        'Content-Type': 'application/json',
        'X-Radar-Bridge': '1'
      },
      body: JSON.stringify({ jobs: [validRawJob(1)] })
    })
    const responseText = await response.text()
    assert.equal(response.status, 502)
    assert.ok(!responseText.includes('secret-value'))
  } finally {
    await new Promise(resolve => server.close(resolve))
  }
})
