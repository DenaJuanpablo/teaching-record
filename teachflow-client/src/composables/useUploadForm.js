import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createRecord } from '@/api/record'
import { useRouter } from 'vue-router'

export function useUploadForm() {
    const form = reactive({
        file: null,
        title: '',
        durationSeconds: null,
        sceneType: 'GENERAL',
        sceneMeta: {}
    })
    const uploading = ref(false)
    const router = useRouter()

    watch(() => form.sceneType, (newType) => {
        if (newType === 'HOMEWORK_CHECK') {
            form.sceneMeta = { assignmentName: '', studentName: '', questionRange: '', checkDate: '' }
        } else if (newType === 'DEFENSE') {
            form.sceneMeta = { topicTitle: '', studentName: '', defenseRound: '', defenseDate: '' }
        } else {
            form.sceneMeta = {}
        }
    }, { immediate: true })


    const handleFileChange = (file, fileList) => {
        if (!file) return

        const fileName = file.name.toLowerCase()
        const isValidExt = fileName.endsWith('.mp4') || fileName.endsWith('.mov') ||
            fileName.endsWith('.wav') || fileName.endsWith('.mp3')


        if (!isValidExt) {
            ElMessage.error('文件格式不支持，仅支持 mp4 / mov / wav / mp3')
            if (fileList) fileList.splice(0, fileList.length)
            form.file = null
            return
        }


        if (file.raw.size > 500 * 1024 * 1024) {
            ElMessage.error('文件大小不能超过 500MB')
            if (fileList) fileList.splice(0, fileList.length)
            form.file = null
            return
        }

        form.file = file.raw
    }

    const submitUpload = async () => {
        if (!form.file) {
            ElMessage.warning('请先选择文件')
            return
        }

        const formData = new FormData()
        formData.append('file', form.file)
        if (form.title) formData.append('title', form.title)
        if (form.durationSeconds) formData.append('durationSeconds', form.durationSeconds)
        formData.append('sceneType', form.sceneType)
        if (Object.keys(form.sceneMeta).length > 0) {
            formData.append('sceneMeta', JSON.stringify(form.sceneMeta))
        }

        uploading.value = true
        try {
            const result = await createRecord(formData)
            ElMessage.success('上传成功')
            if (result && result.id) {
                router.push(`/records/${result.id}`)
            } else {
                router.push('/records')
            }
        } catch (error) {
            ElMessage.error(error.message || '上传失败，请检查网络或后端服务')
        } finally {
            uploading.value = false
        }
    }

    const resetForm = () => {
        form.file = null
        form.title = ''
        form.durationSeconds = null
        form.sceneType = 'GENERAL'
    }

    return {
        form,
        uploading,
        handleFileChange,
        submitUpload,
        resetForm
    }
}