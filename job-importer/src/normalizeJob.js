const text = value => String(value || '').trim()
const trimTo = (value, max) => text(value).slice(0, max)

export function parseSalary(value) {
  const match = text(value).toUpperCase().match(/(\d+(?:\.\d+)?)\s*-\s*(\d+(?:\.\d+)?)\s*K/)
  if (!match) return { salaryMin: 0, salaryMax: 0 }
  return {
    salaryMin: Math.round(Number(match[1]) * 1000),
    salaryMax: Math.round(Number(match[2]) * 1000)
  }
}

export function parseExperience(value) {
  const normalized = text(value)
  if (!normalized || /不限|应届|在校/.test(normalized)) return 0
  const match = normalized.match(/(\d+)/)
  return match ? Number(match[1]) : 0
}

export function normalizeLimit(value) {
  const parsed = Number.parseInt(value || '10', 10)
  return Math.min(20, Math.max(1, Number.isFinite(parsed) ? parsed : 10))
}

export function normalizeJob(raw, fallbackCity = '') {
  const title = trimTo(raw.title, 100)
  const company = trimTo(raw.company, 100)
  const externalId = trimTo(raw.externalId, 100)
  if (!title) throw new Error('职位标题不能为空')
  if (!company) throw new Error('公司名称不能为空')
  if (!externalId) throw new Error('外部职位编号不能为空')
  const salary = parseSalary(raw.salary)
  return {
    source: 'BOSS',
    externalId,
    title,
    company,
    city: trimTo(raw.city || fallbackCity || '不限', 50),
    ...salary,
    experienceYears: parseExperience(raw.experience),
    education: trimTo(raw.education || '不限', 50),
    description: trimTo(raw.description || '来源职位页面未提供完整描述', 1000),
    requirements: trimTo(raw.requirements || raw.description || '以来源职位页面为准', 1000),
    welfareTags: trimTo(Array.isArray(raw.welfareTags) ? raw.welfareTags.join(',') : raw.welfareTags, 300),
    sourceUrl: trimTo(raw.sourceUrl, 1000),
    status: 'OPEN'
  }
}
