<template>
  <el-card style="margin-bottom: 20px;">
    <el-form :inline="true" :model="localFilters">
      <el-form-item label="标题">
        <el-input v-model="localFilters.keyword" placeholder="输入标题关键字" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="localFilters.status" placeholder="全部" clearable style="width: 100px">
          <el-option label="已上传" value="UPLOADED" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="失败" value="FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item label="场景类型">
        <el-select v-model="localFilters.sceneType" placeholder="全部" clearable style="width: 100px">
          <el-option label="通用" value="GENERAL" />
          <el-option label="作业检查" value="HOMEWORK_CHECK" />
          <el-option label="答辩" value="DEFENSE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive } from 'vue'

const emit = defineEmits(['search', 'reset'])


const localFilters = reactive({
  keyword: '',
  status: '',
  sceneType: ''
})


const handleSearch = () => {
  emit('search', { ...localFilters })
}


const handleReset = () => {
  localFilters.keyword = ''
  localFilters.status = ''
  localFilters.sceneType = ''
  emit('reset')
}
</script>