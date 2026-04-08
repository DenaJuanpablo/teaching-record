<template>
  <div class="dashboard-container" style="padding: 20px;">
    <h2>教学数据大盘</h2>

    <el-row :gutter="20" class="top-cards">
      <el-col :span="8">
        <el-card shadow="hover" class="data-card bg-blue">
          <div class="card-title">累计处理记录</div>
          <div class="card-value">{{ summaryData.totalRecords }} <span class="unit">次</span></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="data-card bg-green">
          <div class="card-title">累计音视频时长</div>
          <div class="card-value">{{ summaryData.totalDuration }} <span class="unit">分钟</span></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="data-card bg-purple">
          <div class="card-title">AI 分析完成率</div>
          <div class="card-value">{{ summaryData.successRate }} <span class="unit">%</span></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header"><span>教学场景分布</span></div>
          </template>
          <div ref="pieChartRef" style="height: 300px; width: 100%;"></div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header"><span>近7天教学记录录入趋势</span></div>
          </template>
          <div ref="lineChartRef" style="height: 300px; width: 100%;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header"><span>教学过程高频关注词汇 (基于AI大模型提取)</span></div>
          </template>
          <div ref="barChartRef" style="height: 300px; width: 100%;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive } from 'vue'
import * as echarts from 'echarts'

import { getDashboardSummary } from '@/api/record'
import { ElMessage } from 'element-plus'


const pieChartRef = ref(null)
const lineChartRef = ref(null)
const barChartRef = ref(null)


let pieChart = null
let lineChart = null
let barChart = null


const summaryData = reactive({
  totalRecords: 0,
  totalDuration: 0,
  successRate: 0.0
})


const renderCharts = (data) => {

  if (!pieChart) pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { top: 'bottom' },
    series: [{
      name: '场景类型',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: { label: { show: true, fontSize: 20, fontWeight: 'bold' } },
      labelLine: { show: false },

      data: data.pieData && data.pieData.length > 0 ? data.pieData : [{ name: '暂无数据', value: 0 }]
    }]
  })


  if (!lineChart) lineChart = echarts.init(lineChartRef.value)
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.trendDates || []
    },
    yAxis: { type: 'value' },
    series: [{
      name: '录入次数',
      type: 'line',
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.5)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
        ])
      },
      itemStyle: { color: '#409EFF' },
      data: data.trendCounts || []
    }]
  })


  if (!barChart) barChart = echarts.init(barChartRef.value)
  barChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: {
      type: 'category',
      data: data.barWords || []
    },
    series: [{
      name: '词频次数',
      type: 'bar',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
          { offset: 0, color: '#67C23A' },
          { offset: 1, color: '#A0CFFF' }
        ]),
        borderRadius: [0, 10, 10, 0]
      },
      data: data.barCounts || []
    }]
  })
}


const fetchRealData = async () => {
  try {

    const res = await getDashboardSummary()


    summaryData.totalRecords = res.totalRecords || 0
    summaryData.totalDuration = res.totalDuration || 0
    summaryData.successRate = res.successRate || 0


    renderCharts(res)

  } catch (error) {
    ElMessage.error('获取看板数据失败')
    console.error('API Error:', error)
  }
}


const handleResize = () => {
  pieChart?.resize()
  lineChart?.resize()
  barChart?.resize()
}


onMounted(() => {
  fetchRealData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  lineChart?.dispose()
  barChart?.dispose()
})
</script>

<style scoped>
.top-cards .el-card {
  border: none;
  color: white;
}
.data-card {
  height: 120px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.card-title {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 10px;
}
.card-value {
  font-size: 36px;
  font-weight: bold;
}
.unit {
  font-size: 14px;
  font-weight: normal;
  opacity: 0.8;
}


.bg-blue { background: linear-gradient(135deg, #409EFF, #73b3f3); }
.bg-green { background: linear-gradient(135deg, #67C23A, #95d475); }
.bg-purple { background: linear-gradient(135deg, #9c27b0, #d05ce3); }
</style>