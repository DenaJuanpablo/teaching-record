import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createRecord } from '@/api/record'
import { useRouter } from 'vue-router'  // 注意：组合式函数也可以使用路由！

export function useUploadForm() {
    // 1. 定义响应式数据
    const form = reactive({
        file: null,
        title: '',
        durationSeconds: null,
        sceneType: 'GENERAL',
        sceneMeta: {}
    })
    const uploading = ref(false)
    const router = useRouter()  // 组合式函数内部也可以使用其他组合式函数

    // 2. 监听场景变化
    watch(() => form.sceneType, (newType) => {
        if (newType === 'HOMEWORK_CHECK') {
            form.sceneMeta = { assignmentName: '', studentName: '', questionRange: '', checkDate: '' }
        } else if (newType === 'DEFENSE') {
            form.sceneMeta = { topicTitle: '', studentName: '', defenseRound: '', defenseDate: '' }
        } else {
            form.sceneMeta = {}
        }
    }, { immediate: true })

    // 3. 定义方法
    const handleFileChange = (file) => {
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
            console.error('上传失败', error)
        } finally {
            uploading.value = false
        }
    }

    const resetForm = () => {
        form.file = null
        form.title = ''
        form.durationSeconds = null
        form.sceneType = 'GENERAL'
        // sceneMeta 会被 watch 自动重置
    }

    // 4. 返回所有需要暴露给组件的属性和方法
    return {
        form,
        uploading,
        handleFileChange,
        submitUpload,
        resetForm
    }
}