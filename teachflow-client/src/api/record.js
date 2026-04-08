import request from '@/utils/request'

export function createRecord(data) {
    return request.post('/records', data)
}


export function getRecordList(params) {
    return request.get('/records', { params })
}


export function getRecord(id) {
    return request.get(`/records/${id}`)
}


export function getTranscript(id) {
    return request.get(`/records/${id}/transcript`)
}


export function getAnalysis(id) {
    return request.get(`/records/${id}/analysis`)
}
export function processRecord(id) {
    return request.post(`/records/${id}/process`)
}

export function deleteRecord(id) {
    return request.delete(`/records/${id}`)
}




export function getDashboardSummary() {
    return request.get('/dashboard/summary')
}
