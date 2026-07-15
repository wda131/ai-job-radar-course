import { existsSync } from 'node:fs'
import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { chromium } from 'playwright'
import { extractBossCards, detectAccessRestriction } from './bossParser.js'
import { openBossSession } from './browserSession.js'
import { loginAndImport } from './importClient.js'
import { normalizeJob, normalizeLimit } from './normalizeJob.js'

const IMPORTER_DIR = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

export function parseArgs(argv) {
  const values = {}
  const supported = new Set(['keyword', 'city', 'limit', 'url', 'fixture'])
  for (let index = 0; index < argv.length; index += 2) {
    const flag = argv[index]
    if (!flag?.startsWith('--')) throw new Error(`无法识别的参数：${flag || ''}`)
    const name = flag.slice(2)
    if (!supported.has(name)) throw new Error(`不支持的参数：${flag}`)
    const value = argv[index + 1]
    if (!value || value.startsWith('--')) throw new Error(`${flag} 缺少参数值`)
    values[name] = value
  }
  if (!String(values.keyword || '').trim()) throw new Error('必须提供 --keyword')
  return {
    keyword: values.keyword.trim(),
    city: String(values.city || '').trim(),
    limit: normalizeLimit(values.limit),
    url: values.url,
    fixture: values.fixture
  }
}

function resolveFixture(input) {
  const roots = [process.env.INIT_CWD, process.cwd(), IMPORTER_DIR].filter(Boolean)
  for (const root of roots) {
    const candidate = path.resolve(root, input)
    if (existsSync(candidate)) return candidate
  }
  throw new Error(`找不到 fixture 文件：${input}`)
}

async function readFixture(options) {
  const browser = await chromium.launch({ headless: true })
  try {
    const page = await browser.newPage()
    await page.setContent(await readFile(resolveFixture(options.fixture), 'utf8'))
    return await extractBossCards(page, options.city, options.limit)
  } finally {
    await browser.close()
  }
}

async function readLivePage(options) {
  const context = await openBossSession(path.join(IMPORTER_DIR, '.browser-data'))
  try {
    const page = context.pages()[0] || await context.newPage()
    const url = options.url || `https://www.zhipin.com/web/geek/job?query=${encodeURIComponent(options.keyword)}`
    await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 60000 })
    console.log('浏览器已打开，请在可见窗口中手动完成登录；程序最多等待 3 分钟。')
    const deadline = Date.now() + 180000
    while (Date.now() < deadline) {
      if (await detectAccessRestriction(page)) {
        throw new Error('检测到安全验证或访问限制，已按安全边界停止，不会尝试绕过。')
      }
      const jobs = await extractBossCards(page, options.city, options.limit)
      if (jobs.length > 0) return jobs
      await page.waitForTimeout(2000)
    }
    throw new Error('等待岗位列表超时，请确认已登录并且页面上存在可见岗位卡片。')
  } finally {
    await context.close()
  }
}

export async function run(argv = process.argv.slice(2), env = process.env) {
  const options = parseArgs(argv)
  const rawJobs = options.fixture ? await readFixture(options) : await readLivePage(options)
  const errors = []
  const jobs = []
  for (let index = 0; index < rawJobs.length; index += 1) {
    try {
      jobs.push(normalizeJob(rawJobs[index], options.city))
    } catch (error) {
      errors.push(`第 ${index + 1} 条：${error.message}`)
    }
  }
  if (jobs.length === 0) throw new Error('没有可导入的有效岗位')
  if (!env.RADAR_USERNAME || !env.RADAR_PASSWORD) {
    throw new Error('请先设置 RADAR_USERNAME 和 RADAR_PASSWORD 环境变量')
  }
  const result = await loginAndImport({
    baseUrl: env.API_BASE_URL || 'http://127.0.0.1:9000',
    username: env.RADAR_USERNAME,
    password: env.RADAR_PASSWORD,
    jobs
  })
  console.log(`导入完成：新增 ${result.created}，更新 ${result.updated}，拒绝 ${result.rejected}`)
  for (const message of [...errors, ...(result.errors || [])]) console.log(`- ${message}`)
  return result
}

const entry = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : ''
if (import.meta.url === entry) {
  run().catch(error => {
    console.error(`导入失败：${error.message}`)
    process.exitCode = 1
  })
}
