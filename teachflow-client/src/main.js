// 文件源: .\teachflow-client\src\main.js
import { createApp } from 'vue'
import { createPinia } from 'pinia' // 引入 Pinia
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'

const app = createApp(App)
const pinia = createPinia() // 创建 Pinia 实例

app.use(pinia) // 注册 Pinia
app.use(ElementPlus)
app.use(router)
app.mount('#app')