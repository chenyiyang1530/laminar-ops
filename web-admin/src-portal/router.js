import { createRouter, createWebHistory } from 'vue-router'
import { store } from './store'
import request from './request'
import Layout from './layout/index.vue'
import Login from './views/Login.vue'
import Dashboard from './views/Dashboard.vue'
import Monitor from './views/Monitor.vue'
import Alarms from './views/Alarms.vue'
import Workorders from './views/Workorders.vue'
import Messages from './views/Messages.vue'

const routes = [
  { path: '/login', component: Login, meta: { title: '登录' } },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: Dashboard, meta: { title: '本院概览' } },
      { path: 'monitor', component: Monitor, meta: { title: '设备监测' } },
      { path: 'alarms', component: Alarms, meta: { title: '设备告警' } },
      { path: 'workorders', component: Workorders, meta: { title: '报修工单' } },
      { path: 'messages', component: Messages, meta: { title: '消息通知' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({ history: createWebHistory(window.__APP_BASE__ || '/'), routes })

router.beforeEach(async (to) => {
  document.title = (to.meta.title || '医院协同') + ' · 洁净设备运维'
  if (to.path === '/login') return store.token ? '/' : true
  if (!store.token) return { path: '/login', query: { redirect: to.fullPath } }
  if (!store.profile) {
    try {
      const res = await request.get('/portal/profile')
      store.profile = res.data
    } catch (e) {
      store.token = ''
      localStorage.removeItem('portal_token')
      return '/login'
    }
  }
  return true
})

export default router
