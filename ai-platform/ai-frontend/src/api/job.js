import request from './request'

export function getJobs() {
  return request.get('/api/jobs')
}

export function getJob(id) {
  return request.get(`/api/jobs/${id}`)
}

export function createJob(data) {
  return request.post('/api/jobs', data)
}

export function updateJob(data) {
  return request.put('/api/jobs', data)
}

export function deleteJob(id) {
  return request.delete(`/api/jobs/${id}`)
}

export function pauseJob(id) {
  return request.post(`/api/jobs/${id}/pause`)
}

export function resumeJob(id) {
  return request.post(`/api/jobs/${id}/resume`)
}

export function getJobLogs(id) {
  return request.get(`/api/jobs/${id}/logs`)
}

export function getExecutors() {
  return request.get('/api/jobs/executors')
}

export function getBlogs(limit = 20) {
  return request.get(`/api/jobs/blogs?limit=${limit}`)
}