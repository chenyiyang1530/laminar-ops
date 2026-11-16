import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('laminar_token') || '',
    user: null,
    roles: [],
    roleNames: [],
    permissions: [],
    menus: [],
    dataScope: 'self',
    hospitalId: null,
    departmentId: null,
    engineerId: null,
    companyId: null,
    companyName: '',
    pageSize: 10,
    mustChangePassword: false,
    releaseUnread: false,
    latestRelease: null,
    loaded: false
  }),
  getters: {
    isSuperAdmin: (s) => s.roles.includes('super_admin'),
    isHospitalUser: (s) => s.user?.userType === 'hospital'
      || s.roles.includes('hospital_admin') || s.roles.includes('hospital_dept'),
    displayName: (s) => s.user?.nickName || s.user?.username || ''
  },
  actions: {
    async login(form) {
      const res = await request.post('/auth/login', form)
      this.token = res.data.token
      localStorage.setItem('laminar_token', res.data.token)
      this.mustChangePassword = !!res.data.mustChangePassword
      return res.data
    },
    async getInfo() {
      const res = await request.get('/auth/info')
      const d = res.data
      this.user = d.user
      this.roles = d.roles || []
      this.roleNames = d.roleNames || []
      this.permissions = d.permissions || []
      this.menus = d.menus || []
      this.dataScope = d.dataScope
      this.hospitalId = d.hospitalId
      this.departmentId = d.departmentId
      this.engineerId = d.engineerId
      this.companyId = d.companyId
      this.companyName = d.companyName || ''
      this.pageSize = d.pageSize || d.user?.prefPageSize || 10
      this.mustChangePassword = !!(d.mustChangePassword || d.user?.mustChangePwd)
      this.loaded = true
      return d
    },
    async loadReleaseNotice() {
      const res = await request.get('/release/latest')
      this.latestRelease = res.data?.latest || null
      this.releaseUnread = !!res.data?.unread
      return this.releaseUnread
    },
    async markReleaseSeen() {
      await request.post('/release/seen', { id: this.latestRelease?.id })
      this.releaseUnread = false
    },
    hasPerm(perm) {
      if (!perm) return true
      if (this.isSuperAdmin) return true
      return this.permissions.includes(perm)
    },
    async logout() {
      try { await request.post('/auth/logout') } catch (e) { /* ignore */ }
      this.token = ''
      this.loaded = false
      this.user = null
      this.menus = []
      localStorage.removeItem('laminar_token')
      window.location.href = (window.__APP_BASE__ || '/') + 'login'
    }
  }
})
