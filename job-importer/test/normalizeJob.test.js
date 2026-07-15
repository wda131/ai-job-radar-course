import assert from 'node:assert/strict'
import test from 'node:test'
import { normalizeJob, normalizeLimit, parseExperience, parseSalary } from '../src/normalizeJob.js'

test('parses monthly K salary ranges', () => {
  assert.deepEqual(parseSalary('10-16K'), { salaryMin: 10000, salaryMax: 16000 })
  assert.deepEqual(parseSalary('\uE032\uE036-\uE033\uE031K'), { salaryMin: 15000, salaryMax: 20000 })
  assert.deepEqual(parseSalary('面议'), { salaryMin: 0, salaryMax: 0 })
})

test('normalizes experience and clamps import limit', () => {
  assert.equal(parseExperience('3-5年'), 3)
  assert.equal(parseExperience('应届生'), 0)
  assert.equal(normalizeLimit('80'), 50)
  assert.equal(normalizeLimit('0'), 1)
})

test('builds a validated BOSS import record', () => {
  const result = normalizeJob({
    externalId: 'abc123', title: ' Java开发 ', company: ' 海纳科技 ', city: '威海',
    salary: '10-16K·13薪', experience: '1-3年', education: '本科',
    description: '负责后端服务', requirements: 'Java Spring Boot',
    welfareTags: ['双休', '五险一金'], sourceUrl: 'https://www.zhipin.com/job_detail/abc123.html'
  })
  assert.equal(result.source, 'BOSS')
  assert.equal(result.salaryMin, 10000)
  assert.equal(result.salaryText, '10-16K·13薪')
  assert.equal(result.welfareTags, '双休,五险一金')
})

test('preserves decoded daily salary text without inventing a monthly range', () => {
  const result = normalizeJob({
    externalId: 'daily123', title: 'AI 实习生', company: '海纳科技', city: '威海',
    salary: '\uE033\uE031\uE031-\uE037\uE031\uE031元/天', experience: '应届生', education: '本科'
  })

  assert.equal(result.salaryText, '200-600元/天')
  assert.equal(result.salaryMin, 0)
  assert.equal(result.salaryMax, 0)
})

test('rejects records without identity fields', () => {
  assert.throws(() => normalizeJob({ title: '', company: '公司' }), /职位标题不能为空/)
})
