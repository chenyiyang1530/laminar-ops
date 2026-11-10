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
      { path: '/device/type', redirect: '/device/equipment' },
      { path: '/device/realtime', redirect: '/device/monitor' },
      { path: '/device/point', redirect: '/device/monitor' },
      { path: '/device/rule', redirect: '/device/monitor' },
      { path: '/budget', redirect: '/business' },
      { path: '/report', redirect: '/business' },
      { path: '/customer/company', redirect: '/organization/company' },
      { path: '/customer/company-region', redirect: '/organization/company' },
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
      { path: '/resource/document', redirect: '/resource/library' },
      { path: '/resource/knowledge', redirect: '/resource/library' },
      { path: '/staff/engineer', redirect: '/staff/people' },
      { path: '/staff/skill', redirect: '/staff/people' },
      { path: '/staff/certificate', redirect: '/staff/people' },
      { path: '/resource/part', redirect: '/resource/parts' },
      { path: '/resource/requisition', redirect: '/resource/parts' },
      { path: '/system/dept', redirect: '/system/org' },
      { path: '/system/post', redirect: '/system/org' },
      { path: '/system/user', redirect: '/system/access' },
      { path: '/system/role', redirect: '/system/access?tab=role' }
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
