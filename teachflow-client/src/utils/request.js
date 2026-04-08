// 文件源: .\teachflow-client\src\utils\request.js
import axios from 'axios'
import { useAuthStore } from '@/store/auth' // 引入 Store

const request = axios.create({
    baseURL: '/api',
    timeout: 60000
})

request.interceptors.request.use(
    config => {
        const authStore = useAuthStore() // 获取 Store 实例
        if (authStore.token) {
            config.headers['Authorization'] = `Bearer ${authStore.token}`
        }
        return config
    },
    error => Promise.reject(error)
)

// 响应拦截器处理 401 逻辑
request.interceptors.response.use(
    response => {
        // Axios 的 response.data 是后端返回的 JSON 体
        const res = response.data

        // 如果后端返回的 code 是 0，代表业务成功
        if (res.code === 0) {
            // 【核心修复】直接返回 res.data，这样 Store 拿到的才是包含 token 的对象
            return res.data
        } else {
            // 业务失败（如密码错误），抛出异常
            return Promise.reject(new Error(res.message || '操作失败'))
        }
    },
    error => {
        if (error.response?.status === 401) {
            const authStore = useAuthStore()
            authStore.logout()
            // 使用 window.location 强制刷新并清理所有状态
            window.location.href = '/login'
        }
        return Promise.reject(error)
    }
)

export default request