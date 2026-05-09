import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export type PortalMode = 'user' | 'admin'

function parsePortal(raw: string | null): PortalMode | null {
  if (raw === 'user' || raw === 'admin') return raw
  return null
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('lib_fake_token'))
  const displayName = ref(localStorage.getItem('lib_display_name') || '')
  const userId = ref(Number(localStorage.getItem('lib_user_id') || '1'))
  const portalMode = ref<PortalMode | null>(parsePortal(localStorage.getItem('lib_portal_mode')))

  const isAuthenticated = computed(() => !!token.value)

  function login(username: string, _password: string, mode: PortalMode) {
    const name = (username || '').trim() || '访客'
    const uid = /^\d+$/.test(name) ? Number(name) : 1
    const fake = 'fake-' + Date.now()
    token.value = fake
    displayName.value = name
    userId.value = uid
    portalMode.value = mode
    localStorage.setItem('lib_fake_token', fake)
    localStorage.setItem('lib_display_name', name)
    localStorage.setItem('lib_user_id', String(uid))
    localStorage.setItem('lib_portal_mode', mode)
  }

  function logout() {
    token.value = null
    displayName.value = ''
    userId.value = 1
    portalMode.value = null
    localStorage.removeItem('lib_fake_token')
    localStorage.removeItem('lib_display_name')
    localStorage.removeItem('lib_user_id')
    localStorage.removeItem('lib_portal_mode')
  }

  function hydrate() {
    const t = localStorage.getItem('lib_fake_token')
    const m = parsePortal(localStorage.getItem('lib_portal_mode'))
    if (t && !m) {
      logout()
      return
    }
    token.value = t
    portalMode.value = m
    if (token.value) {
      displayName.value = localStorage.getItem('lib_display_name') || ''
      userId.value = Number(localStorage.getItem('lib_user_id') || '1')
    } else {
      displayName.value = ''
      userId.value = 1
      portalMode.value = null
    }
  }

  return { token, displayName, userId, portalMode, isAuthenticated, login, logout, hydrate }
})
