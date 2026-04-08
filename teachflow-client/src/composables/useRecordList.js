import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getRecordList, deleteRecord as apiDeleteRecord } from '@/api/record'

export function useRecordList() {

    const filters = reactive({
        keyword: '',
        status: '',
        sceneType: ''
    })


    const currentPage = ref(1)
    const pageSize = ref(10)
    const total = ref(0)
    const tableData = ref([])
    const loading = ref(false)


    const fetchData = async () => {
        loading.value = true
        try {

            const params = {
                page: currentPage.value,
                size: pageSize.value,
                keyword: filters.keyword || undefined,
                status: filters.status || undefined,
                sceneType: filters.sceneType || undefined
            }
            const res = await getRecordList(params)

            tableData.value = res.list
            total.value = res.total
        } catch (error) {
            ElMessage.error('获取列表失败')
        } finally {
            loading.value = false
        }
    }


    fetchData()


    const handleSearch = (newFilters) => {
        Object.assign(filters, newFilters)
        currentPage.value = 1
        fetchData()
    }


    const handleReset = () => {
        filters.keyword = ''
        filters.status = ''
        filters.sceneType = ''
        currentPage.value = 1
        fetchData()
    }


    const handlePageChange = (page) => {
        currentPage.value = page
        fetchData()
    }


    const handleSizeChange = (size) => {
        pageSize.value = size
        currentPage.value = 1
        fetchData()
    }


    const deleteRecord = async (id) => {
        try {
            await apiDeleteRecord(id)
            ElMessage.success('删除成功')

            await fetchData()
        } catch (error) {


        }
    }


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