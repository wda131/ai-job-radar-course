(function attachBridgeClient(root) {
  const BASE_URL = 'http://127.0.0.1:9011'

  async function request(path, options = {}) {
    const response = await fetch(`${BASE_URL}${path}`, options)
    const payload = await response.json().catch(() => ({ message: '本地桥接返回无法解析的响应' }))
    if (!response.ok || payload.code !== 200) {
      throw new Error(payload.message || `本地桥接请求失败（HTTP ${response.status}）`)
    }
    return payload.data
  }

  const health = () => request('/health')
  const importJobs = jobs => request('/import', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Radar-Bridge': '1' },
    body: JSON.stringify({ jobs })
  })

  root.RadarBridgeClient = { health, importJobs }
})(globalThis)
