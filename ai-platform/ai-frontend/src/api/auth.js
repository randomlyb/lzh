import request from './request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function refreshToken(token) {
  return request.post('/auth/refreshToken', token)
}

export function getCurrentUser() {
  return request.get('/userinfo')
}