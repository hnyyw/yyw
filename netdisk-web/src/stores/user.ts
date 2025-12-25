import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    accessToken: localStorage.getItem('netdisk_access_token') || '',
  }),
  actions: {
    setToken(token: string) {
      this.accessToken = token
      localStorage.setItem('netdisk_access_token', token)
    },
    clearToken() {
      this.accessToken = ''
      localStorage.removeItem('netdisk_access_token')
    },
  },
})

