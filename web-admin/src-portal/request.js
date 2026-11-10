import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({ baseURL: '/api', timeout: 20000 })

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('portal_token')
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) return res
    if (res.code === 401) redirect()
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg))
  },
  (error) => {
    if (error.response?.status === 401) redirect()
    else ElMessage.error(error.response?.data?.msg || '网络异常')
    return Promise.reject(error)
  }
)

function redirect() {
  localStorage.removeItem('portal_token')
  const base = window.__APP_BASE__ || '/'
  if (!location.pathname.startsWith(base + 'login')) {
    setTimeout(() => { location.href = base + 'login' }, 500)
  }
}

export default service
