import { ref } from 'vue'
import {getRecord, getTranscript, getAnalysis, processRecord} from '@/api/record'
import { ElMessage } from 'element-plus'

export function useRecordDetail(id) {

    const record = ref(null)
    const loading = ref(false)
    const error = ref(null)


    const transcript = ref(null)
    const transcriptLoading = ref(false)
    const transcriptError = ref(null)


    const analysis = ref(null)
    const analysisLoading = ref(false)
    const analysisError = ref(null)


    const processing = ref(false)
    let pollTimer = null


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


    const fetchTranscript = async () => {
        transcriptLoading.value = true
        transcriptError.value = null
        try {
            const data = await getTranscript(id)
            transcript.value = data
        } catch (err) {

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


    const loadAll = async () => {
        await fetchRecord()

        if (record.value && record.value.status === 'COMPLETED') {
            await Promise.all([fetchTranscript(), fetchAnalysis()])
        }
    }


    const triggerProcess = async () => {
        processing.value = true
        try {
            await processRecord(id)
            ElMessage.success('已开始处理')

            await fetchRecord()

            if (record.value && record.value.status === 'PROCESSING') {
                startPolling()
            } else if (record.value && record.value.status === 'COMPLETED') {

                await Promise.allSettled([fetchTranscript(), fetchAnalysis()])
            }
        } catch (err) {
            ElMessage.error(err.message || '处理失败')
        } finally {
            processing.value = false
        }
    }


    const startPolling = () => {
        if (pollTimer) clearInterval(pollTimer)
        pollTimer = setInterval(async () => {
            try {
                await fetchRecord()
                if (record.value && record.value.status !== 'PROCESSING') {
                    stopPolling()
                    if (record.value.status === 'COMPLETED') {
                        await Promise.allSettled([fetchTranscript(), fetchAnalysis()])
                    }
                }
            } catch (e) {

            }
        }, 3000)
    }


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