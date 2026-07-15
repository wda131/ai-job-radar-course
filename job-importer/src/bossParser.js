const CARD_SELECTOR = '.job-card-wrapper, .job-card-box, .job-list-box li'

async function readText(root, selector) {
  const locator = root.locator(selector).first()
  if (await locator.count() === 0) return ''
  return (await locator.innerText()).trim()
}

async function readTexts(root, selector) {
  const values = await root.locator(selector).allInnerTexts()
  return values.map(value => value.trim()).filter(Boolean)
}

function externalIdFromUrl(url) {
  return String(url || '').match(/\/job_detail\/([^/?#]+?)(?:\.html)?(?:[?#]|$)/)?.[1] || ''
}

export async function extractBossCards(page, fallbackCity = '', limit = 20) {
  const jobLink = page.locator('a.job-card-left, a[href*="/job_detail/"]')
  const cards = page.locator(CARD_SELECTOR).filter({ has: jobLink })
  const results = []
  const seen = new Set()
  const boundedLimit = Math.min(20, Math.max(1, Number(limit) || 20))

  for (let index = 0; index < await cards.count() && results.length < boundedLimit; index += 1) {
    const card = cards.nth(index)
    if (!await card.isVisible()) continue

    const link = card.locator('a.job-card-left, a[href*="/job_detail/"]').first()
    const sourceUrl = await link.getAttribute('href') || ''
    const externalId = externalIdFromUrl(sourceUrl)
    if (!externalId || seen.has(externalId)) continue

    const jobInfo = await readTexts(card, '.job-info li')
    const welfareTags = await readTexts(card, '.tag-list li')
    results.push({
      externalId,
      title: await readText(card, '.job-name'),
      company: await readText(card, '.company-name'),
      city: await readText(card, '.job-area') || fallbackCity,
      salary: await readText(card, '.salary'),
      experience: jobInfo[0] || '',
      education: jobInfo[1] || '',
      description: welfareTags.join(' '),
      requirements: welfareTags.join(' '),
      welfareTags,
      sourceUrl
    })
    seen.add(externalId)
  }

  return results
}

export async function detectAccessRestriction(page) {
  const title = await page.title()
  const body = (await page.locator('body').innerText()).slice(0, 1000)
  return /验证码|安全验证|访问异常|请稍后再试/.test(`${title}\n${body}`)
}
