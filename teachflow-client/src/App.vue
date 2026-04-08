<template>
  <div style="display: flex; height: 100vh;">
    <el-menu
        :default-active="activeIndex"
        router
        style="width: 240px; position: relative; display: flex; flex-direction: column;"
    >

      <el-menu-item disabled style="font-weight: bold; cursor: default; opacity: 1; color: #409EFF; height: 60px;">
        <span style="margin-left: 8px; font-size: 16px;">面对面教学记录</span>
      </el-menu-item>
      <el-divider style="margin: 8px 0;" />

      <template v-if="authStore.isLoggedIn">
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>数据看板</span>
        </el-menu-item>

        <el-menu-item index="/upload">
          <el-icon><Upload /></el-icon>
          <span>上传记录</span>
        </el-menu-item>

        <el-menu-item index="/records">
          <el-icon><List /></el-icon>
          <span>记录列表</span>
        </el-menu-item>

        <el-menu-item
            v-if="lastDetail"
            :index="`/records/${lastDetail.id}`"
            style="margin-top: 20px; border-top: 1px solid #eee;"
        >
          <el-icon><Document /></el-icon>
          <span>详情：{{ lastDetail.title }}</span>
        </el-menu-item>

        <div class="user-profile-block">
          <el-divider style="margin: 12px 0;" />
          <div class="user-info">
            <el-icon><User /></el-icon>
            <span class="username">{{ authStore.username }}</span>
          </div>
          <el-button
              type="danger"
              text
              @click="handleLogout"
              style="width: 100%;"
          >
            退出登录
          </el-button>
        </div>
      </template>
    </el-menu>

    <div style="flex: 1; padding: 20px; overflow-y: auto; background-color: #f5f7fa;">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRecord } from '@/api/record'
import { useAuthStore } from '@/store/auth' // [新增]
import { Document, Upload, List, DataLine, User } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore() // [新增]

// --- 你原来的业务逻辑开始 ---
const lastDetail = ref(null)

// 监听路由变化，记录最近查看的详情页
watch(
    () => route.path,
    async (newPath) => {
      const match = newPath.match(/^\/records\/(\d+)$/)
      if (match) {
        const id = match[1]
        try {
          const record = await getRecord(id)
          lastDetail.value = { id, title: record.title || `记录 #${id}` }
          localStorage.setItem('lastDetail', JSON.stringify(lastDetail.value))
        } catch (error) {
          console.error('获取最近记录详情失败', error)
        }
      }
    },
    { immediate: true }
)

const saved = localStorage.getItem('lastDetail')
if (saved) {
  try {
    lastDetail.value = JSON.parse(saved)
  } catch (e) {}
}

const activeIndex = computed(() => {
  if (route.path.startsWith('/records/')) {
    return '/records'
  }
  return route.path
})
// --- 你原来的业务逻辑结束 ---

// [新增]：退出登录逻辑
const handleLogout = () => {
  authStore.logout() // 清除 Pinia 和 localStorage 中的 token/username
  // 注意：这里可以按需选择是否清除 lastDetail，通常建议清除
  localStorage.removeItem('lastDetail')
  lastDetail.value = null
  router.push('/login')
}
</script>

<style scoped>
.el-menu-item.is-disabled {
  background-color: transparent !important;
}
.el-menu-item.is-disabled:hover {
  background-color: transparent !important;
}

/* [新增样式] 让用户信息固定在侧边栏底部 */
.user-profile-block {
  margin-top: auto; /* 在 flex 布局下，这一行会将该 div 推到底部 */
  padding: 0 20px 20px 20px;
  text-align: center;
}

.user-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #606266;
  font-size: 14px;
}

.username {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>