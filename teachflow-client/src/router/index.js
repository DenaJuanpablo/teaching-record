import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/',
        redirect: '/dashboard'   // 修改：根路径重定向到看板页
    },
    {
        path: '/dashboard',      // 新增：数据看板路由
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

export default router