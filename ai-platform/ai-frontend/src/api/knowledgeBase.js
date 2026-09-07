import request from './request'

export function getKnowledgeBases() {
  return request.get('/api/knowledge-bases')
}

export function getKnowledgeBase(id) {
  return request.get(`/api/knowledge-bases/${id}`)
}

export function createKnowledgeBase(data) {
  return request.post('/api/knowledge-bases', data)
}

export function updateKnowledgeBase(id, data) {
  return request.put(`/api/knowledge-bases/${id}`, data)
}

export function deleteKnowledgeBase(id) {
  return request.delete(`/api/knowledge-bases/${id}`)
}

export function addDocumentsToKnowledgeBase(data) {
  return request.post('/api/knowledge-bases/add-documents', data)
}

export function removeDocumentFromKnowledgeBase(kbId, docId) {
  return request.delete(`/api/knowledge-bases/${kbId}/documents/${docId}`)
}