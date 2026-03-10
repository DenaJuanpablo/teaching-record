import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const request = axios.create({
    baseURL: '/api',           // 后端接口的基础路径（所有请求都会自动加上 /api 前缀）
    timeout: 60000              // 请求超时时间 60 秒（大文件上传需要长一点）
})

// 请求拦截器（在发送请求之前做些什么）
request.interceptors.request.use(
    config => {
        // 如果发送的是 FormData（文件上传），自动设置正确的 Content-Type
        if (config.data instanceof FormData) {
            config.headers['Content-Type'] = 'multipart/form-data'
        }
        // 可以在这里添加 token 等认证信息（后续需要时再加）
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 响应拦截器（对响应数据做统一处理）
request.interceptors.response.use(
    response => {
        const res = response.data
        // 假设后端返回格式为 { code, message, data }
        if (res.code === 0) {
            // 成功：只返回 data 部分，方便使用
            return res.data
        } else {
            // 业务错误：构造一个包含完整响应数据的错误对象
            const error = new Error(res.message || '操作失败')
            error.code = res.code
            error.responseData = res
            return Promise.reject(error)
        }
    },
    error => {
        // 网络错误或超时
        const netError = new Error('网络错误，请稍后重试')
        netError.isNetwork = true
        return Promise.reject(netError)
    }
)

export default request