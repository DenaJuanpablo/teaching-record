<template>
  <div style="padding: 20px;">
    <div v-if="loading">加载中...</div>
    <div v-else-if="error">错误：{{ error }}</div>
    <div v-else-if="record">
      <h1>{{ record.title }}</h1>
      <p>状态：<el-tag :type="statusTagType">{{ record.status }}</el-tag></p>
      <p>时长：{{ record.durationSeconds }} 秒</p>
      <p>创建时间：{{ record.createdAt }}</p>
      <p v-if="record.failedReason">失败原因：{{ record.failedReason }}</p>

      <!-- 视频播放器（简单版） -->
      <div v-if="record.videoUrl">
        <video :src="record.videoUrl" controls style="width: 100%; max-height: 400px;"></video>
      </div>

      <div style="margin: 20px 0;">
        <!-- 当状态为 UPLOADED 或 FAILED 时显示处理/重试按钮 -->
        <el-button
            v-if="record.status === 'UPLOADED' || record.status === 'FAILED'"
            type="primary"
            :loading="processing"
            @click="handleProcess"
        >
          {{ record.status === 'FAILED' ? '重试' : '处理' }}
        </el-button>
        <!-- 当状态为 PROCESSING 时显示一个禁用按钮，提示处理中 -->
        <el-button v-if="record.status === 'PROCESSING'" type="info" disabled>处理中...</el-button>
        <!-- 原来的返回列表按钮保持不变 -->
        <el-button @click="goBack">返回列表</el-button>
      </div>

      <!-- 转写结果 -->
      <el-divider />
      <h2>转写结果</h2>
      <div v-if="transcriptLoading">加载中...</div>
      <div v-else-if="transcriptError" style="color: #999;">{{ transcriptError }}</div>
      <div v-else-if="transcript && transcript.segments">
        <el-timeline>
          <el-timeline-item
              v-for="(seg, index) in transcript.segments"
              :key="index"
              :timestamp="formatTime(seg.startMs) + ' - ' + formatTime(seg.endMs)"
          >
            {{ seg.text }}
          </el-timeline-item>
        </el-timeline>
      </div>
      <div v-else>暂无转写数据</div>

      <!-- 分析结果 -->
      <el-divider />
      <h2>分析结果</h2>
      <div v-if="analysisLoading">加载中...</div>
      <div v-else-if="analysisError" style="color: #999;">{{ analysisError }}</div>
      <div v-else-if="analysis">
        <p><strong>摘要：</strong>{{ analysis.summary }}</p>
        <p><strong>关键词：</strong>
          <el-tag v-for="kw in analysis.keywords" :key="kw" size="small" style="margin-right: 5px;">{{ kw }}</el-tag>
        </p>
        <!-- 大纲展示（根据类型定制） -->
        <div v-if="analysis.outline && analysis.outline.length">
          <p><strong>大纲：</strong></p>
          <div v-for="(item, idx) in analysis.outline" :key="idx">
            <!-- 通用报告 -->
            <div v-if="item.type === 'GENERAL_REPORT'">
              <h3>主题：{{ item.topic }}</h3>
              <el-timeline>
                <el-timeline-item v-for="(section, i) in item.sections" :key="i">
                  <h4>{{ section.title }}</h4>
                  <ul>
                    <li v-for="(point, j) in section.keyPoints" :key="j">{{ point }}</li>
                  </ul>
                </el-timeline-item>
              </el-timeline>
            </div>

            <!-- 作业检查报告 -->
            <div v-else-if="item.type === 'HOMEWORK_CHECK_REPORT'">
              <p><strong>结论：</strong>{{ item.header.conclusion }}</p>
              <p><strong>标签：</strong>
                <el-tag v-for="tag in item.header.tags" :key="tag.name" size="small" style="margin-right:5px;">
                  {{ tag.name }} ({{ tag.count }})
                </el-tag>
              </p>
              <el-collapse>
                <el-collapse-item v-for="panel in item.panels" :key="panel.panelId" :title="panel.title">
                  <div v-for="it in panel.items" :key="it.issue">
                    <p><strong>问题：</strong>{{ it.issue }}</p>
                    <p><strong>改法：</strong>{{ it.suggestion }}</p>
                    <p><strong>证据时间：</strong>{{ formatTime(it.evidence.startMs) }} - {{ formatTime(it.evidence.endMs) }}</p>
                  </div>
                </el-collapse-item>
              </el-collapse>
              <div v-if="item.todo && item.todo.length">
                <p><strong>待办：</strong></p>
                <ul>
                  <li v-for="todo in item.todo" :key="todo.todoId">
                    {{ todo.title }}（{{ todo.status }}）
                  </li>
                </ul>
              </div>
            </div>

            <!-- 答辩报告 -->
            <div v-else-if="item.type === 'DEFENSE_REPORT'">
              <p><strong>结论：</strong>{{ item.header.verdict }}</p>
              <p><strong>总体评价：</strong>{{ item.header.overallConclusion }}</p>
              <p><strong>标签：</strong>
                <el-tag v-for="tag in item.header.tags" :key="tag.name" size="small">{{ tag.name }}</el-tag>
              </p>
              <el-collapse>
                <el-collapse-item v-for="panel in item.panels" :key="panel.panelId" :title="panel.category">
                  <div v-for="it in panel.items" :key="it.issue">
                    <p><strong>问题：</strong>{{ it.issue }}</p>
                    <p><strong>建议：</strong>{{ it.suggestion }}</p>
                    <p><strong>优先级：</strong>{{ it.priority }}</p>
                    <p><strong>证据时间：</strong>{{ formatTime(it.evidence.startMs) }} - {{ formatTime(it.evidence.endMs) }}</p>
                  </div>
                </el-collapse-item>
              </el-collapse>
              <div v-if="item.todo && item.todo.length">
                <p><strong>待办：</strong></p>
                <ul>
                  <li v-for="todo in item.todo" :key="todo.todoId">
                    {{ todo.title }}（{{ todo.priority }}）
                  </li>
                </ul>
              </div>
            </div>

            <!-- 未知类型，降级显示 JSON -->
            <div v-else>
              <pre>{{ JSON.stringify(item, null, 2) }}</pre>
            </div>
          </div>
        </div>
      </div>
      <div v-else>暂无分析数据</div>
    </div>
  </div>
</template>

<script setup>
import {computed, onUnmounted} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRecordDetail } from '@/composables/useRecordDetail'

const route = useRoute()
const router = useRouter()
const id = route.params.id

const {
  record,
  loading,
  error,
  transcript,
  transcriptLoading,
  transcriptError,
  analysis,
  analysisLoading,
  analysisError,
  processing,
  triggerProcess,
  stopPolling,
  loadAll
} = useRecordDetail(id)

// 页面加载时调用 loadAll
loadAll()

// 状态对应的标签类型
const statusTagType = computed(() => {
  const map = {
    UPLOADED: 'info',
    PROCESSING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger'
  }
  return map[record.value?.status] || 'info'
})

// 格式化时间（毫秒转 mm:ss）
const formatTime = (ms) => {
  if (!ms) return '0:00'
  const totalSeconds = Math.floor(ms / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}:${seconds.toString().padStart(2, '0')}`
}

// 返回列表
const goBack = () => {
  router.push('/records')
}

// 定义处理/重试按钮的点击方法
const handleProcess = async () => {
  await triggerProcess()   // 调用组合式函数中的触发方法
}

onUnmounted(() => {
  stopPolling()   // 清理轮询定时器
})
</script>