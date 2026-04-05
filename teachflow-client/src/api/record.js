import request from '@/utils/request'

export function createRecord(data) {
    return request.post('/records', data)
}

// 获取记录列表（支持分页和筛选）
export function getRecordList(params) {
    return request.get('/records', { params })
}

// 获取记录详情
export function getRecord(id) {
    return request.get(`/records/${id}`)
}

// 获取转写结果
export function getTranscript(id) {
    return request.get(`/records/${id}/transcript`)
}

// 获取分析结果
export function getAnalysis(id) {
    return request.get(`/records/${id}/analysis`)
}
export function processRecord(id) {
    return request.post(`/records/${id}/process`)
}

export function deleteRecord(id) {
    return request.delete(`/records/${id}`)
}

// ... 上面是你原来的代码（获取列表、删除等）

// 获取数据看板的真实统计数据
export function getDashboardSummary() {
    return request.get('/dashboard/summary')
}
