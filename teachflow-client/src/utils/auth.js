// 修改源: .\teachflow-client\src\utils\auth.js

const TOKEN_KEY = 'teachflow_token' // 统一使用这个 Key，不要用 'token'
const USERNAME_KEY = 'teachflow_username'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (token) => localStorage.setItem(TOKEN_KEY, token)
export const removeToken = () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USERNAME_KEY)
}

// 新增用户名的存取
export const getUsername = () => localStorage.getItem(USERNAME_KEY)
export const setUsername = (name) => localStorage.setItem(USERNAME_KEY, name)