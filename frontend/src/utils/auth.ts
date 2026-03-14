export type RoleCode = 'ADMIN' | 'WORKSHOP' | 'WAREHOUSE'

const TOKEN_KEY = 'mes_token'
const ROLE_KEY = 'mes_role'
const USERNAME_KEY = 'mes_username'
const USER_ID_KEY = 'mes_user_id'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function getRole(): RoleCode | null {
  return (localStorage.getItem(ROLE_KEY) as RoleCode | null) ?? null
}

export function getUsername(): string | null {
  return localStorage.getItem(USERNAME_KEY)
}

export function getUserId(): number | null {
  const v = localStorage.getItem(USER_ID_KEY)
  return v != null ? Number(v) : null
}

export function setAuth(token: string, username: string, role: RoleCode, userId?: number | null) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USERNAME_KEY, username)
  localStorage.setItem(ROLE_KEY, role)
  if (userId != null) localStorage.setItem(USER_ID_KEY, String(userId))
  else localStorage.removeItem(USER_ID_KEY)
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USERNAME_KEY)
  localStorage.removeItem(ROLE_KEY)
  localStorage.removeItem(USER_ID_KEY)
}

export function defaultHomeByRole(role: RoleCode | null): string {
  if (role === 'ADMIN') return '/admin'
  if (role === 'WAREHOUSE') return '/warehouse'
  return '/workshop'
}

