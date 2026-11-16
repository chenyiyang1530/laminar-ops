import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { viewModules } from './views'

const Placeholder = () => import('../views/placeholder.vue')
const Layout = () => import('../layout/index.vue')
const Login = () => import('../views/login/index.vue')
const NotFound = () => import('../views/error/404.vue')

const routes = [
  { path: '/login', component: Login, meta: { title: '登录' } },
  {
    path: '/',
    name: 'Layout',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/profile/index.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'release',
        name: 'Release',
        component: () => import('../views/release/index.vue'),
        meta: { title: '版本更新' }
      },
      {
        path: 'message',
        name: 'MessagePage',
        component: () => import('../views/message/index.vue'),
        meta: { title: '消息中心' }
      },
      { path: '/customer/hospital', redirect: '/customer/archive' },
      { path: '/customer/campus', redirect: '/customer/archive' },
      { path: '/customer/department', redirect: '/customer/archive' },
      { path: '/customer/clean-area', redirect: '/customer/archive' },
      { path: '/customer/contact', redirect: '/customer/archive' },
      { path: '/alarm/list', redirect: { path: '/workorder/monitor', query: { tab: 'alarm' } } },
      { path: '/alarm', redirect: { path: '/workorder/monitor', query: { tab: 'alarm' } } },
      { path: '/device/equipment', redirect: { path: '/customer/archive', query: { tab: 'equipment' } } },
      { path: '/device/monitor', redirect: '/workorder/monitor' },
      { path: '/device/type', redirect: { path: '/customer/archive', query: { tab: 'deviceType' } } },
      { path: '/workorder/maintenance', redirect: { path: '/workorder/inspection', query: { tab: 'maintenancePlan' } } },
      { path: '/workorder/maintenance-plan', redirect: { path: '/workorder/inspection', query: { tab: 'maintenancePlan' } } },
      { path: '/workorder/maintenance-task', redirect: { path: '/workorder/inspection', query: { tab: 'maintenanceTask' } } },
      { path: '/workorder/inspection-plan', redirect: { path: '/workorder/inspection', query: { tab: 'inspectionPlan' } } },
      { path: '/workorder/inspection-task', redirect: { path: '/workorder/inspection', query: { tab: 'inspectionTask' } } },
      { path: '/device/realtime', redirect: '/workorder/monitor' },
      { path: '/device/point', redirect: '/workorder/monitor' },
      { path: '/device/rule', redirect: '/workorder/monitor' },
      { path: '/analytics/workspace', redirect: '/dashboard' },
      { path: '/customer/company', redirect: { path: '/organization/overview', query: { tab: 'company' } } },
      { path: '/customer/contract', redirect: { path: '/organization/overview', query: { tab: 'contract' } } },
      { path: '/customer/company-region', redirect: { path: '/organization/overview', query: { tab: 'company' } } },
      { path: '/customer/region', redirect: { path: '/organization/org', query: { tab: 'region' } } },
      { path: '/organization/qualification', redirect: { path: '/organization/org', query: { tab: 'qualification' } } },
      { path: '/organization/company', redirect: { path: '/organization/overview', query: { tab: 'company' } } },
      { path: '/system/org', redirect: { path: '/organization/org' } },
      { path: '/workorder/list', redirect: '/workorder/dispatch' },
      { path: '/workorder/inspection-plan', redirect: '/workorder/inspection' },
      { path: '/workorder/inspection-task', redirect: '/workorder/inspection' },
      { path: '/workorder/maintenance-plan', redirect: '/workorder/maintenance' },
      { path: '/workorder/maintenance-task', redirect: '/workorder/maintenance' },
      { path: '/staff/dispatch', redirect: '/workorder/dispatch' },
      { path: '/system/loginlog', redirect: '/system/audit' },
      { path: '/system/operlog', redirect: '/system/audit' },
      { path: '/monitor/server', redirect: '/monitor/system' },
      { path: '/monitor/cache', redirect: '/monitor/system' },
      { path: '/monitor/data', redirect: '/monitor/system' },
      { path: '/resource/document', redirect: { path: '/resource/library', query: { tab: 'document' } } },
      { path: '/resource/knowledge', redirect: { path: '/resource/library', query: { tab: 'knowledge' } } },
      { path: '/staff/engineer', redirect: { path: '/staff/people', query: { tab: 'engineer' } } },
      { path: '/staff/skill', redirect: { path: '/staff/people', query: { tab: 'skill' } } },
      { path: '/staff/certificate', redirect: { path: '/staff/people', query: { tab: 'certificate' } } },
      { path: '/staff/cert-type-request', redirect: { path: '/staff/people', query: { tab: 'certificate' } } },
      { path: '/system/cert-type', redirect: { path: '/organization/org', query: { tab: 'qualification' } } },
      { path: '/resource/part', redirect: { path: '/resource/parts', query: { tab: 'part' } } },
      { path: '/resource/requisition', redirect: { path: '/resource/parts', query: { tab: 'requisition' } } },
      { path: '/system/dept', redirect: { path: '/organization/org' } },
      { path: '/system/post', redirect: { path: '/organization/org' } },
      { path: '/system/user', redirect: { path: '/staff/people', query: { tab: 'user' } } },
      { path: '/system/role', redirect: { path: '/staff/people', query: { tab: 'role' } } },
      { path: '/system/access', redirect: { path: '/staff/people', query: { tab: 'user' } } },
      { path: '/staff/approval-config', redirect: { path: '/staff/approval-settings', query: { tab: 'config' } } },
      { path: '/staff/approval-delegate', redirect: { path: '/staff/approval-settings', query: { tab: 'delegate' } } }
    ]
  },
  { path: '/:pathMatch(.*)*', component: NotFound, meta: { title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHistory(window.__APP_BASE__ || '/'),
  routes
})

function addMenuRoute(menu, parentPath) {
  if (!menu.path || menu.path === '#') return
  let full = menu.path
  if (parentPath && !full.startsWith('/')) {
    full = parentPath.replace(/\/$/, '') + '/' + full
  }
  if (!full.startsWith('/')) full = '/' + full
  const name = 'menu-' + menu.menuId
  if (router.hasRoute(name)) return
  const loader = menu.component ? viewModules[menu.component] : null
  router.addRoute('Layout', {
    path: full,
    name,
    component: loader || Placeholder,
    meta: { title: menu.menuName, icon: menu.icon, menuId: menu.menuId }
  })
}

export function addDynamicRoutes(menus) {
  menus.forEach((m) => {
    if (m.menuType === 'M') {
      ;(m.children || []).forEach((c) => addMenuRoute(c, m.path))
    } else {
      addMenuRoute(m, '')
    }
  })
}

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} · 层流智能运维` : '层流净化设备智能运维平台'
  const store = useUserStore()
  if (to.path === '/login') {
    return store.token ? '/' : true
  }
  if (!store.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!store.loaded) {
    try {
      await store.getInfo()
      addDynamicRoutes(store.menus)
      if (store.mustChangePassword && to.path !== '/profile') {
        return { path: '/profile', query: { force: '1' } }
      }
      return { ...to, replace: true }
    } catch (e) {
      store.token = ''
      localStorage.removeItem('laminar_token')
      return '/login'
    }
  }
  if (store.mustChangePassword && to.path !== '/profile') {
    return { path: '/profile', query: { force: '1' } }
  }
  return true
})

export default router
