import assert from 'node:assert/strict'
import { createServer } from 'node:http'
import test from 'node:test'
import { loginAndImport } from '../src/importClient.js'

test('logs in and submits jobs with the returned JWT', async () => {
  const server = createServer(async (request, response) => {
    const chunks = []
    for await (const chunk of request) chunks.push(chunk)
    const body = JSON.parse(Buffer.concat(chunks).toString('utf8'))
    response.setHeader('content-type', 'application/json')

    if (request.url === '/api/user/login') {
      assert.deepEqual(body, { username: 'student', password: 'student123' })
      response.end(JSON.stringify({ code: 200, data: { token: 'test-token' } }))
      return
    }

    assert.equal(request.url, '/api/jobs/import')
    assert.equal(request.headers.authorization, 'test-token')
    assert.equal(body.jobs[0].externalId, 'abc123')
    response.end(JSON.stringify({
      code: 200,
      data: { created: 1, updated: 0, rejected: 0, errors: [] }
    }))
  })

  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  try {
    const { port } = server.address()
    const result = await loginAndImport({
      baseUrl: `http://127.0.0.1:${port}`,
      username: 'student',
      password: 'student123',
      jobs: [{ externalId: 'abc123' }]
    })
    assert.equal(result.created, 1)
  } finally {
    await new Promise(resolve => server.close(resolve))
  }
})
