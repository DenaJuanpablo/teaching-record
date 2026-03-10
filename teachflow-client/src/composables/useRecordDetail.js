import { ref } from 'vue'
import {getRecord, getTranscript, getAnalysis, processRecord} from '@/api/record'
import { ElMessage } from 'element-plus'

export function useRecordDetail(id) {
    // 基础信息
    const record = ref(null)
    const loading = ref(false)
    const error = ref(null)

    // 转写结果
    const transcript = ref(null)
    const transcriptLoading = ref(false)
    const transcriptError = ref(null)

    // 分析结果
    const analysis = ref(null)
    const analysisLoading = ref(false)
    const analysisError = ref(null)

    // 新增：处理中状态和轮询相关
    const processing = ref(false)
    let pollTimer = null

    // 获取记录详情
    const fetchRecord = async () => {
        loading.value = true
        error.value = null
        try {
            const data = await getRecord(id)
            record.value = data
        } catch (err) {
            error.value = err.message || '获取记录失败'
            ElMessage.error(error.value)
        } finally {
            loading.value = false
        }
    }

    // 获取转写结果
    const fetchTranscript = async () => {
        transcriptLoading.value = true
        transcriptError.value = null
        try {
            const data = await getTranscript(id)
            transcript.value = data
        } catch (err) {
            // 如果是未就绪（code=3001），我们将其视为正常状态，不显示错误弹窗，只在 UI 上提示
            if (err.code === 3001) {
                transcriptError.value = '转写未就绪'
            } else {
                transcriptError.value = err.message || '获取转写失败'
                ElMessage.error(transcriptError.value)
            }
        } finally {
            transcriptLoading.value = false
        }
    }

    // 获取分析结果
    const fetchAnalysis = async () => {
        analysisLoading.value = true
        analysisError.value = null
        try {
            const data = await getAnalysis(id)
            analysis.value = data
        } catch (err) {
            if (err.code === 3002) {
                analysisError.value = '分析未就绪'
            } else {
                analysisError.value = err.message || '获取分析失败'
                ElMessage.error(analysisError.value)
            }
        } finally {
            analysisLoading.value = false
        }
    }

    // 一次性加载所有数据（通常在组件挂载时调用）
    const loadAll = async () => {
        await fetchRecord()
        // 如果记录状态为 COMPLETED，才尝试获取转写和分析
        if (record.value && record.value.status === 'COMPLETED') {
            await Promise.all([fetchTranscript(), fetchAnalysis()])
        }
    }

    // 新增：触发处理
    const triggerProcess = async () => {
        processing.value = true
        try {
            await processRecord(id)
            ElMessage.success('已开始处理')
            // 重新获取记录详情，更新状态
            await fetchRecord()
            // 如果状态变为 PROCESSING，开始轮询
            if (record.value && record.value.status === 'PROCESSING') {
                startPolling()
            } else if (record.value && record.value.status === 'COMPLETED') {
                // 如果直接完成（极少数情况），加载转写和分析
                await Promise.allSettled([fetchTranscript(), fetchAnalysis()])
            }
        } catch (err) {
            ElMessage.error(err.message || '处理失败')
        } finally {
            processing.value = false
        }
    }

    // 新增：开始轮询
    const startPolling = () => {
        if (pollTimer) clearInterval(pollTimer)
        pollTimer = setInterval(async () => {
            try {
                await fetchRecord() // 重新获取记录详情
                if (record.value && record.value.status !== 'PROCESSING') {
                    stopPolling()
                    if (record.value.status === 'COMPLETED') {
                        await Promise.allSettled([fetchTranscript(), fetchAnalysis()])
                    }
                }
            } catch (e) {
                // 忽略轮询中的错误
            }
        }, 3000)
    }

    // 新增：停止轮询
    const stopPolling = () => {
        if (pollTimer) {
            clearInterval(pollTimer)
            pollTimer = null
        }
    }

    return {
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
        fetchRecord,
        fetchTranscript,
        fetchAnalysis,
        loadAll,
        triggerProcess,
        stopPolling
    }
}