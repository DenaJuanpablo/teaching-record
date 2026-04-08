// 文件：src/api/auth.js
import request from '@/utils/request'

// 登录接口：统一管理 URL 和请求方式
export function login(data) {
    return request.post('/auth/login', data)
}

// 注册接口 (预留)
export function register(data) {
    return request.post('/auth/register', data)
}