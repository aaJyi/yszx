import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

export const request = axios.create({
  baseURL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('admin_token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    const userId = localStorage.getItem('admin_userId')
    if (userId) config.headers.userId = userId
    return config
  },
  (err) => Promise.reject(err)
)

request.interceptors.response.use(
  (res) => {
    const { data } = res
    if (data && typeof data.code !== 'undefined' && data.code !== 200) {
      const err = new Error(data.message || '服务器返回错误')
      err.response = res
      return Promise.reject(err)
    }
    return data
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_userId')
      window.location.href = '/#/login'
    }
    return Promise.reject(err)
  }
)

export default request
