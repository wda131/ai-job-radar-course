chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  if (message?.type !== 'RADAR_EXTRACT_BOSS_JOBS') return false

  try {
    const jobs = globalThis.RadarBossExtractor.extractBossJobs(document, message.limit || 50)
    sendResponse({ ok: true, jobs })
  } catch (error) {
    sendResponse({ ok: false, message: error.message || '岗位解析失败' })
  }
  return false
})
