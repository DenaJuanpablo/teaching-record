<template>
  <el-pagination
      background
      layout="prev, pager, next, total, sizes"
      :total="total"
      :page-size="pageSize"
      :page-sizes="[5, 10, 20]"
      :current-page="currentPage"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
      style="margin-top: 20px; justify-content: flex-end;"
  />
</template>

<script setup>
defineProps({
  total: {
    type: Number,
    required: true
  },
  currentPage: {
    type: Number,
    required: true
  },
  pageSize: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['page-change', 'size-change'])

// 页码变化时触发
const handlePageChange = (page) => {
  emit('page-change', page)
}

// 每页条数变化时触发
const handleSizeChange = (size) => {
  emit('size-change', size)
}
</script>