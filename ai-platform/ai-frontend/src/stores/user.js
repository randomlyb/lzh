import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi } from '../api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  async function login(loginForm) {
    const res = await loginApi(loginForm)
    token.value = res.token || ''
    // 保存完整的用户信息，包括 username
    userInfo.value = {
      token: res.token,
      userId: res.userId,
      username: res.username,
      ...res
    }
    if (token.value) {
      localStorage.setItem('token', token.value)
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
      // 保存 username 到 userId，用于 AI 对话
      localStorage.setItem('userId', res.username || res.userId || '')
    }
    return res
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, login, logout }
})