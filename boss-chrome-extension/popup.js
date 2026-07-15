const statusElement = document.querySelector('#status')
const countElement = document.querySelector('#count')
const detectButton = document.querySelector('#detect')
const importButton = document.querySelector('#import')

let detectedJobs = []
let bridgeReady = false

function showStatus(message, type = '') {
  statusElement.textContent = message
  statusElement.className = `status ${type}`.trim()
}

function setBusy(busy) {
  detectButton.disabled = busy
  importButton.disabled = busy || !bridgeReady || detectedJobs.length === 0
}

async function activeBossTab() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true })
  if (!tab?.id || !tab.url?.startsWith('https://www.zhipin.com/web/geek/job')) {
    throw new Error('请打开 BOSS 岗位搜索结果页')
  }
  return tab
}

async function detectJobs() {
  setBusy(true)
  try {
    const tab = await activeBossTab()
    const result = await chrome.tabs.sendMessage(tab.id, {
      type: 'RADAR_EXTRACT_BOSS_JOBS',
      limit: 50
    })
    if (!result?.ok) throw new Error(result?.message || '岗位解析失败，请刷新 BOSS 页面后重试')

    detectedJobs = result.jobs || []
    countElement.textContent = detectedJobs.length
      ? `已检测到 ${detectedJobs.length} 个可导入岗位`
      : '当前页面没有检测到岗位，请先滚动加载'
    showStatus(detectedJobs.length ? '页面解析完成，可以开始导入。' : '没有找到可导入岗位。', detectedJobs.length ? 'ok' : 'error')
  } catch (error) {
    detectedJobs = []
    countElement.textContent = '尚未检测岗位'
    showStatus(error.message || '检测失败', 'error')
  } finally {
    setBusy(false)
  }
}

async function importJobs() {
  if (!detectedJobs.length) return
  setBusy(true)
  showStatus(`正在导入 ${detectedJobs.length} 个岗位……`)
  try {
    const result = await globalThis.RadarBridgeClient.importJobs(detectedJobs)
    showStatus(`导入完成：新增 ${result.created || 0}，更新 ${result.updated || 0}，拒绝 ${result.rejected || 0}`, 'ok')
  } catch (error) {
    showStatus(error.message || '导入失败', 'error')
  } finally {
    setBusy(false)
  }
}

detectButton.addEventListener('click', detectJobs)
importButton.addEventListener('click', importJobs)

globalThis.RadarBridgeClient.health()
  .then(() => {
    bridgeReady = true
    showStatus('本地桥接已连接，请检测当前 BOSS 页面。', 'ok')
    setBusy(false)
  })
  .catch(() => {
    bridgeReady = false
    showStatus('本地桥接未启动，请先运行启动脚本。', 'error')
    setBusy(false)
  })
