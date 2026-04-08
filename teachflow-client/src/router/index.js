import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes = [
    {
        path: '/',
        redirect: '/dashboard'
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/Login.vue')
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('@/views/Register.vue')
    },
    {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/record/Dashboard.vue')
    },
    {
        path: '/upload',
        name: 'Upload',
        component: () => import('@/views/record/Upload.vue')
    },
    {
        path: '/records',
        name: 'RecordList',
        component: () => import('@/views/record/List.vue')
    },
    {
        path: '/records/:id',
        name: 'RecordDetail',
        component: () => import('@/views/record/Detail.vue')
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// --- 路由守卫优化部分 ---

// 1. 定义白名单（不需要登录就能访问的路径）
const whiteList = ['/login', '/register']

router.beforeEach((to, from, next) => {
    // 【核心修改】不要直接读 localStorage，用工具类函数
    const token = getToken()

    if (token) {
        if (whiteList.includes(to.path)) {
            next('/dashboard')
        } else {
            next()
        }
    } else {
        if (whiteList.includes(to.path)) {
            next()
        } else {
            next('/login')
        }
    }
})

export default router