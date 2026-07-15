const baseUrl = process.env.API_BASE_URL || 'http://127.0.0.1:9000'

const request = async (path, options = {}) => {
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    signal: AbortSignal.timeout(90000)
  })
  const result = await response.json()
  if (!response.ok || result.code !== 200) {
    throw new Error(`${path}: ${result.message || response.statusText}`)
  }
  return result.data
}

const login = await request('/api/user/login', {
  method: 'POST',
  body: JSON.stringify({ username: 'student', password: '123456' })
})
const headers = { Authorization: login.token }
const jobs = await request('/api/jobs?page=1&size=8', { headers })
const keywordJobs = await request('/api/jobs?keyword=Java&page=1&size=8', { headers })
const job = jobs.records[0]
const match = await request(`/api/matches/jobs/${job.id}`, { method: 'POST', headers })
const interview = await request(`/api/interviews?jobId=${job.id}`, { method: 'POST', headers })
const firstQuestion = interview.questions[0]
const answer = await request(`/api/interviews/${interview.id}/answers`, {
  method: 'POST',
  headers,
  body: JSON.stringify({
    questionId: firstQuestion.id,
    answer: '我会先明确接口契约，完成参数校验和异常处理，再加入缓存、监控、自动化测试与压力测试。'
  })
})

console.log(JSON.stringify({
  gateway: 'ok',
  jobs: jobs.records.length,
  keywordJobs: keywordJobs.records.length,
  match: {
    finalScore: match.score,
    ruleScore: match.ruleScore,
    semanticScore: match.semanticScore,
    aiUsed: match.aiUsed
  },
  interview: {
    questionCount: interview.questions.length,
    answerScore: answer.score,
    aiUsed: answer.aiUsed,
    strengths: answer.strengths?.length || 0,
    weaknesses: answer.weaknesses?.length || 0
  }
}, null, 2))
