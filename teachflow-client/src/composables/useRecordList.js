import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getRecordList, deleteRecord as apiDeleteRecord } from '@/api/record'

export function useRecordList() {
    // 筛选条件（响应式对象）
    const filters = reactive({
        keyword: '',
        status: '',
        sceneType: ''
    })

    // 分页相关
    const currentPage = ref(1)
    const pageSize = ref(10)
    const total = ref(0)
    const tableData = ref([])    // 表格数据
    const loading = ref(false)   // 加载状态

    // 获取数据的方法
    const fetchData = async () => {
        loading.value = true
        try {
            // 构建请求参数，只传递有值的字段
            const params = {
                page: currentPage.value,
                size: pageSize.value,
                keyword: filters.keyword || undefined,
                status: filters.status || undefined,
                sceneType: filters.sceneType || undefined
            }
            const res = await getRecordList(params)   // 调用 API
            // 假设后端返回的数据格式为 { list: [], total: 0 }
            tableData.value = res.list
            total.value = res.total
        } catch (error) {
            ElMessage.error('获取列表失败')
        } finally {
            loading.value = false
        }
    }

    // 初始化时加载第一页
    fetchData()

    // 处理搜索（接收新的筛选条件）
    const handleSearch = (newFilters) => {
        Object.assign(filters, newFilters)   // 更新筛选条件
        currentPage.value = 1                 // 重置到第一页
        fetchData()
    }

    // 重置筛选
    const handleReset = () => {
        filters.keyword = ''
        filters.status = ''
        filters.sceneType = ''
        currentPage.value = 1
        fetchData()
    }

    // 页码变化
    const handlePageChange = (page) => {
        currentPage.value = page
        fetchData()
    }

    // 每页条数变化
    const handleSizeChange = (size) => {
        pageSize.value = size
        currentPage.value = 1
        fetchData()
    }

    // 在组合式函数内部添加方法
    const deleteRecord = async (id) => {
        try {
            await apiDeleteRecord(id)
            ElMessage.success('删除成功')
            // 刷新当前页数据
            await fetchData()
        } catch (error) {
            // 错误已在拦截器中处理，这里无需额外操作
            // 如果想自定义错误提示，可以在这里捕获，但拦截器已经弹窗了
        }
    }

    // 返回所有需要暴露给组件的属性和方法
    return {
        filters,
        currentPage,
        pageSize,
        total,
        tableData,
        loading,
        handleSearch,
        handleReset,
        handlePageChange,
        handleSizeChange,
        deleteRecord
    }
}