<template>
  <div style="padding: 20px; max-width: 600px; margin: 0 auto;">
    <h1>上传教学记录</h1>
    <el-form label-width="100px">
      
      <el-form-item label="文件">
        <el-upload
            drag
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            accept=".mp4,.mov,.wav,.mp3"
        >
          <i class="el-icon-upload" />
          <div class="el-upload__text">拖拽文件或 <em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">
              支持 mp4/mov/wav/mp3，最大 500MB
            </div>
          </template>
        </el-upload>
      </el-form-item>

      
      <el-form-item label="标题">
        <el-input v-model="form.title" placeholder="请输入标题（可选）" />
      </el-form-item>

      
      <el-form-item label="时长(秒)">
        <el-input-number v-model="form.durationSeconds" :min="0" placeholder="可选" />
      </el-form-item>

      
      <el-form-item label="场景类型">
        <el-select v-model="form.sceneType" placeholder="请选择">
          <el-option label="通用" value="GENERAL" />
          <el-option label="作业检查" value="HOMEWORK_CHECK" />
          <el-option label="答辩" value="DEFENSE" />
        </el-select>
      </el-form-item>

      
      <el-form-item v-if="form.sceneType === 'HOMEWORK_CHECK'" label="作业信息">
        <el-input v-model="form.sceneMeta.assignmentName" placeholder="作业名称" style="width: 200px; margin-right: 10px;" />
        <el-input v-model="form.sceneMeta.studentName" placeholder="学生姓名" style="width: 150px; margin-right: 10px;" />
        <el-input v-model="form.sceneMeta.questionRange" placeholder="题号范围" style="width: 150px; margin-right: 10px;" />
        <el-input v-model="form.sceneMeta.checkDate" placeholder="检查日期 (YYYY-MM-DD)" style="width: 180px;" />
      </el-form-item>

      
      <el-form-item v-if="form.sceneType === 'DEFENSE'" label="答辩信息">
        <el-input v-model="form.sceneMeta.topicTitle" placeholder="课题名称" style="width: 250px; margin-right: 10px;" />
        <el-input v-model="form.sceneMeta.studentName" placeholder="学生姓名" style="width: 150px; margin-right: 10px;" />
        <el-select v-model="form.sceneMeta.defenseRound" placeholder="答辩轮次" style="width: 140px; margin-right: 10px;">
          <el-option label="开题" value="开题" />
          <el-option label="中期" value="中期" />
          <el-option label="预答辩" value="预答辩" />
          <el-option label="终答辩" value="终答辩" />
        </el-select>
        <el-input v-model="form.sceneMeta.defenseDate" placeholder="答辩日期" style="width: 150px;" />
      </el-form-item>

      
      <el-form-item v-if="form.sceneType === 'GENERAL'" label="备注">
        <el-input v-model="form.sceneMeta.remark" placeholder="可选备注" />
      </el-form-item>

      
      <el-form-item>
        <el-button type="primary" @click="submitUpload" :loading="uploading">上传</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { useUploadForm } from '@/composables/useUploadForm'


const { form, uploading, handleFileChange, submitUpload, resetForm } = useUploadForm()
</script>

<style scoped>

</style>