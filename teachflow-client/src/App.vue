<template>
  <div style="display: flex; height: 100vh;">
    <!-- 左侧导航菜单 -->
    <el-menu
        :default-active="activeIndex"
        router
        style="width: 240px;"
    >
      <!-- 应用标题（不可点击） -->
      <el-menu-item disabled style="font-weight: bold; cursor: default; opacity: 1; color: #409EFF; height: 60px;">
        <span style="margin-left: 8px; font-size: 16px;">面对面教学记录</span>
      </el-menu-item>
      <el-divider style="margin: 8px 0;" />

      <!-- 导航菜单项 -->
      <el-menu-item index="/upload">
        <el-icon><Upload /></el-icon>
        <span>上传记录</span>
      </el-menu-item>
      <el-menu-item index="/records">
        <el-icon><List /></el-icon>
        <span>记录列表</span>
      </el-menu-item>

      <!-- 最近查看的详情页（如果有） -->
      <el-menu-item
          v-if="lastDetail"
          :index="`/records/${lastDetail.id}`"
          style="margin-top: 20px; border-top: 1px solid #eee;"
      >
        <el-icon><Document /></el-icon>
        <span>详情页面</span>
      </el-menu-item>
    </el-menu>

    <!-- 右侧内容区域 -->
    <div style="flex: 1; padding: 20px; overflow-y: auto;">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRecord } from '@/api/record'
import { Document, Upload, List } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 存储最近查看的详情记录 { id, title }
const lastDetail = ref(null)

// 监听路由变化，当进入详情页时更新 lastDetail
watch(
    () => route.path,
    async (newPath) => {
      const match = newPath.match(/^\/records\/(\d+)$/)
      if (match) {
        const id = match[1]
        try {
          const record = await getRecord(id)
          lastDetail.value = {
            id,
            title: record.title || `记录 #${id}`
          }
          localStorage.setItem('lastDetail', JSON.stringify(lastDetail.value))
        } catch (error) {
          console.error('获取最近记录详情失败', error)
        }
      }
    },
    { immediate: true }
)

// 从 localStorage 恢复最近查看记录
const saved = localStorage.getItem('lastDetail')
if (saved) {
  try {
    lastDetail.value = JSON.parse(saved)
  } catch (e) {}
}

// 菜单高亮逻辑
const activeIndex = computed(() => {
  if (route.path.startsWith('/records/')) {
    return '/records'
  }
  return route.path
})
</script>

<style scoped>
/* 可选：微调菜单项样式 */
.el-menu-item.is-disabled {
  background-color: transparent !important;
}
.el-menu-item.is-disabled:hover {
  background-color: transparent !important;
}
</style>