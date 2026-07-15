(function attachBossExtractor(root) {
  const CARD_SELECTOR = '.job-card-wrapper, .job-card-box, .job-list-box > li, [class*="job-card-wrapper"]'

  const clean = value => String(value || '').replace(/\s+/g, ' ').trim()
  const text = (node, selector) => clean(node.querySelector(selector)?.textContent)
  const texts = (node, selector) => Array.from(node.querySelectorAll(selector))
    .map(item => clean(item.textContent))
    .filter(Boolean)

  function externalIdFromUrl(url) {
    return String(url || '').match(/\/job_detail\/([^/?#]+?)(?:\.html)?(?:[?#]|$)/)?.[1] || ''
  }

  function extractBossJobs(documentRoot, requestedLimit = 50) {
    const limit = Math.min(50, Math.max(1, Number(requestedLimit) || 50))
    const seen = new Set()
    const jobs = []

    for (const card of documentRoot.querySelectorAll(CARD_SELECTOR)) {
      if (jobs.length >= limit) break
      const link = card.querySelector('a.job-card-left, a[href*="/job_detail/"]')
      if (!link) continue

      const sourceUrl = link.href || link.getAttribute('href') || ''
      const externalId = externalIdFromUrl(sourceUrl)
      if (!externalId || seen.has(externalId)) continue

      const jobInfo = texts(card, '.job-info li, .job-info span')
      const tags = texts(card, '.tag-list li, .job-card-footer li, [class*="tag-list"] li')
      jobs.push({
        externalId,
        title: text(card, '.job-name'),
        company: text(card, '.company-name'),
        city: text(card, '.job-area'),
        salary: text(card, '.salary'),
        experience: jobInfo[0] || '',
        education: jobInfo[1] || '',
        description: '来自 BOSS 当前可见岗位列表，完整信息请查看来源链接',
        requirements: tags.join(' ') || '完整要求请查看 BOSS 来源链接',
        welfareTags: tags,
        sourceUrl
      })
      seen.add(externalId)
    }

    return jobs.filter(job => job.title && job.company)
  }

  root.RadarBossExtractor = { extractBossJobs, externalIdFromUrl }
})(globalThis)
