import request from './request'

export function chat(data) {
  return request.post('/api/rag/chat', data)
}

export function search(data) {
  return request.post('/api/search', data)
}