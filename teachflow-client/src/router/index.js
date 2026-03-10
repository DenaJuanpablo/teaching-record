import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/',
        redirect: '/records'   // 新增：根路径重定向到列表页
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