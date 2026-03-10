<template>
  <el-table
      v-loading="loading"
      :data="data"
      border
      style="width: 100%"
  >
    <el-table-column prop="id" label="ID" width="60" />
    <el-table-column prop="title" label="标题" min-width="200" />
    <el-table-column prop="durationSeconds" label="时长(秒)" width="85" />
    <el-table-column prop="status" label="状态" width="120">
      <template #default="{ row }">
        <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="createdAt" label="创建时间" width="180" />
    <el-table-column prop="sceneType" label="场景" width="120" />
    <el-table-column label="操作" width="150">
      <template #default="{ row }">
        <el-button type="primary" size="small" @click="handleView(row.id)">查看</el-button>
        <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
defineProps({
  data: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['view', 'delete'])

// 根据状态返回对应的标签类型
const getStatusType = (status) => {
  const map = {
    UPLOADED: 'info',
    PROCESSING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger'
  }
  return map[status] || 'info'
}

// 点击查看按钮，触发 view 事件并传递 id
const handleView = (id) => {
  emit('view', id)
}

const handleDelete = (id) => {
  emit('delete', id)
}
</script>