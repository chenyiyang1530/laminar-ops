import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 20000
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('laminar_token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res.code === 200) {
      return res
    }
    if (res.code === 401) {
      handleUnauthorized()
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      handleUnauthorized()
    } else {
      ElMessage.error(error.response?.data?.msg || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  localStorage.removeItem('laminar_token')
  const base = window.__APP_BASE__ || '/'
  if (!location.pathname.startsWith(base + 'login')) {
    setTimeout(() => { window.location.href = base + 'login' }, 600)
  }
}

export default service
