import { createServer } from 'node:http'
import { pathToFileURL } from 'node:url'
import { loginAndImport } from './importClient.js'
import { normalizeJob } from './normalizeJob.js'

const json = (response, status, payload, origin) => {
  response.statusCode = status
  response.setHeader('Content-Type', 'application/json;charset=utf-8')
  response.setHeader('Vary', 'Origin')
  if (origin?.startsWith('chrome-extension://')) {
    response.setHeader('Access-Control-Allow-Origin', origin)
  }
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
  for (const secret of [env.RADAR_PASSWORD].filter(Boolean)) {
    message = message.split(secret).join('[REDACTED]')
  }
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
      if (!origin.startsWith('chrome-extension://')) {
        return json(response, 403, { code: 403, message: '来源不允许' })
      }
      response.setHeader('Access-Control-Allow-Origin', origin)
      response.setHeader('Access-Control-Allow-Methods', 'GET,POST,OPTIONS')
      response.setHeader('Access-Control-Allow-Headers', 'Content-Type,X-Radar-Bridge')
      response.statusCode = 204
      return response.end()
    }

    if (request.method === 'GET' && request.url === '/health') {
      return json(response, 200, {
        code: 200,
        data: { service: 'boss-import-bridge', limit: 50 }
      }, origin)
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
        try {
          jobs.push(normalizeJob(raw))
        } catch (error) {
          errors.push(`第 ${index + 1} 条：${error.message}`)
        }
      })

      if (!jobs.length) {
        return json(response, 400, { code: 400, message: errors[0] || '没有有效岗位' }, origin)
      }

      const result = await delegate(jobs)
      return json(response, 200, {
        code: 200,
        data: { ...result, errors: [...errors, ...(result.errors || [])] }
      }, origin)
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
