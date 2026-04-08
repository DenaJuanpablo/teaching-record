// 修改源: .\teachflow-client\src\store\auth.js
import { defineStore } from 'pinia'
import { getToken, setToken, removeToken, getUsername, setUsername } from '@/utils/auth'
import { login as apiLogin } from '@/api/auth'

export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: getToken() || '',
        username: getUsername() || ''
    }),
    // 【必须新增】App.vue 里的 v-if 依赖这个计算属性
    getters: {
        isLoggedIn: (state) => !!state.token
    },
    actions: {
        async login(loginData) {
            // 直接调用 api 层封装好的 login
            const data = await apiLogin(loginData)

            // 更新内存状态
            this.token = data.token
            this.username = data.username

            // 【关键】同步到硬盘，使用工具类确保 Key 一致
            setToken(data.token)
            setUsername(data.username)
            return data
        },
        logout() {
            this.token = ''
            this.username = ''
            removeToken() // 清理所有 Key
        }
    }
})