import request from './request'

export function uploadDocument(formData) {
  return request.post('/api/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getDocument(id) {
  return request.get(`/api/documents/${id}`)
}

export function deleteDocument(id) {
  return request.delete(`/api/documents/${id}`)
}