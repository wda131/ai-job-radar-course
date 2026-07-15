async function postJson(url, body, token) {
  const headers = { 'content-type': 'application/json' }
  if (token) headers.Authorization = token
  const response = await fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify(body)
  })
  let payload
  try {
    payload = await response.json()
  } catch {
    throw new Error(`课程服务返回了无法解析的响应（HTTP ${response.status}）`)
  }
  if (!response.ok || payload.code !== 200) {
    throw new Error(payload.message || `课程服务请求失败（HTTP ${response.status}）`)
  }
  return payload.data
}

export async function loginAndImport({ baseUrl, username, password, jobs }) {
  const root = String(baseUrl || 'http://127.0.0.1:9000').replace(/\/$/, '')
  const login = await postJson(`${root}/api/user/login`, { username, password })
  if (!login?.token) throw new Error('登录成功响应中缺少 token')
  return postJson(`${root}/api/jobs/import`, { jobs }, login.token)
}
