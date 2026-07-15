import axios from 'axios'

const request = axios.create({ baseURL: 'http://localhost:9000', timeout: 60000 })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('radar_token')
  if (token) config.headers.Authorization = token
  return config
})

request.interceptors.response.use(
  response => {
    const result = response.data
    if (result.code !== 200) return Promise.reject(new Error(result.message || '请求失败'))
    return result.data
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('radar_token')
      localStorage.removeItem('radar_profile')
      if (location.pathname !== '/login') location.href = '/login'
    }
    return Promise.reject(new Error(error.response?.data?.message || error.message || '网络异常'))
  }
)

export default request
