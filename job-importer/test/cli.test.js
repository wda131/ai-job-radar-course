import assert from 'node:assert/strict'
import { createServer } from 'node:http'
import { fileURLToPath } from 'node:url'
import test from 'node:test'
import { parseArgs, run } from '../src/cli.js'

test('parses bounded fixture import arguments', () => {
  const options = parseArgs([
    '--keyword', 'Java', '--city', '威海', '--limit', '8',
    '--fixture', 'test/fixtures/boss-results.html'
  ])

  assert.equal(options.keyword, 'Java')
  assert.equal(options.city, '威海')
  assert.equal(options.limit, 8)
  assert.equal(options.fixture, 'test/fixtures/boss-results.html')
})

test('requires a search keyword', () => {
  assert.throws(() => parseArgs(['--city', '威海']), /必须提供 --keyword/)
})

test('keeps fixture browser open until the batch is imported', async () => {
  const server = createServer((request, response) => {
    response.setHeader('content-type', 'application/json')
    if (request.url === '/api/user/login') {
      response.end(JSON.stringify({ code: 200, data: { token: 'fixture-token' } }))
    } else {
      response.end(JSON.stringify({
        code: 200,
        data: { created: 2, updated: 0, rejected: 0, errors: [] }
      }))
    }
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  try {
    const fixture = fileURLToPath(new URL('./fixtures/boss-results.html', import.meta.url))
    const result = await run(
      ['--keyword', 'Java', '--city', '威海', '--limit', '2', '--fixture', fixture],
      {
        API_BASE_URL: `http://127.0.0.1:${server.address().port}`,
        RADAR_USERNAME: 'student',
        RADAR_PASSWORD: '123456'
      }
    )
    assert.equal(result.created, 2)
  } finally {
    await new Promise(resolve => server.close(resolve))
  }
})
