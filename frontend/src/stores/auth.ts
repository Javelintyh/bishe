import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { clearAuth, getRole, getToken, getUserId, getUsername, setAuth, type RoleCode } from '@/utils/auth'
import { login as apiLogin } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const username = ref<string | null>(getUsername())
  const roleCode = ref<RoleCode | null>(getRole())
  const userId = ref<number | null>(getUserId())

  const isLoggedIn = computed(() => Boolean(token.value))

  async function login(usernameInput: string, password: string) {
    const resp = await apiLogin({ username: usernameInput, password })
    if (resp.code !== 0 || !resp.data?.token) {
      // 抛出完整响应，方便上层根据 code 区分错误类型
      throw resp
    }
    token.value = resp.data.token
    username.value = resp.data.username
    roleCode.value = resp.data.roleCode
    userId.value = resp.data.userId ?? null
    setAuth(resp.data.token, resp.data.username, resp.data.roleCode, resp.data.userId)
  }

  function logout() {
    token.value = null
    username.value = null
    roleCode.value = null
    userId.value = null
    clearAuth()
  }

  return { token, username, roleCode, userId, isLoggedIn, login, logout }
})

