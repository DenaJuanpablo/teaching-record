<template>
  <div style="padding: 20px;">
    <h1>教学记录列表</h1>
    <SearchBar @search="handleSearch" @reset="handleReset" />
    <RecordTable
        :data="tableData"
        :loading="loading"
        @view="goToDetail"
        @delete="handleDelete"
    />
    <PaginationBar
        :total="total"
        :current-page="currentPage"
        :page-size="pageSize"
        @page-change="handlePageChange"
        @size-change="handleSizeChange"
    />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useRecordList } from '@/composables/useRecordList'
import SearchBar from './components/SearchBar.vue'
import RecordTable from './components/RecordTable.vue'
import PaginationBar from './components/PaginationBar.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()


const {
  tableData,
  total,
  currentPage,
  pageSize,
  loading,
  handleSearch,
  handleReset,
  handlePageChange,
  handleSizeChange,
  deleteRecord
} = useRecordList()


const goToDetail = (id) => {
  router.push(`/records/${id}`)
}

const handleDelete = (id) => {
  ElMessageBox.confirm('此操作将永久删除该记录，是否继续？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteRecord(id)
  }).catch(() => {})
}
</script>