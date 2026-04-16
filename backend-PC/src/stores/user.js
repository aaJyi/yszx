import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const userId = ref(localStorage.getItem('admin_userId') || '')
  const nickname = ref(localStorage.getItem('admin_nickname') || '')

  function setLogin(info) {
    token.value = info.token || ''
    userId.value = String(info.userId ?? '')
    nickname.value = info.nickname || ''
    localStorage.setItem('admin_token', token.value)
    localStorage.setItem('admin_userId', userId.value)
    localStorage.setItem('admin_nickname', nickname.value)
  }

  function logout() {
    token.value = ''
    userId.value = ''
    nickname.value = ''
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_userId')
    localStorage.removeItem('admin_nickname')
  }

  const isLoggedIn = () => !!token.value

  return { token, userId, nickname, setLogin, logout, isLoggedIn }
})
