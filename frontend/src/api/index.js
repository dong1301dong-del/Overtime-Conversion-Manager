import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({ baseURL: '' })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers['X-Auth-Token'] = token
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      if (body.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        ElMessage.error('会话已失效，请重新登录')
        window.location.hash = '#/login'
        return Promise.reject(new Error(body.message || '未登录'))
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body ? body.data : null
  },
  (error) => {
    const msg = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default http
