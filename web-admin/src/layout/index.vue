<template>
  <div class="layout">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="logo">
        <div class="logo-mark">LO</div>
        <div v-show="!collapsed" class="logo-text">
          <b>层流智能运维</b>
          <span>Laminar Ops Platform</span>
        </div>
      </div>

      <el-scrollbar class="menu-scroll">
        <div v-if="!collapsed && quickItems.length" class="quick-block">
          <div class="nav-caption">快捷</div>
          <button
            v-for="item in quickItems"
            :key="item.path"
            type="button"
            class="quick-row"
            :class="{ active: item.path === activeMenu }"
            @click="go(item.path)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span class="row-name">{{ item.name }}</span>
            <span v-if="isMessage(item.path) && unread" class="row-badge">{{ badgeText }}</span>
            <span class="row-pin" :class="{ 'is-fav': isFav(item.path) }" @click.stop="toggleFav(item.path)">
              <el-icon><StarFilled v-if="isFav(item.path)" /><Star v-else /></el-icon>
            </span>
          </button>
        </div>

        <div v-if="!collapsed && quickItems.length" class="nav-caption nav-caption-all">全部功能</div>

        <el-menu
            ref="menuRef"
            :default-active="activeMenu"
            :default-openeds="openeds"
            :collapse="collapsed"
            :collapse-transition="false"
            :unique-opened="true"
            router
            background-color="transparent"
            text-color="#9aa4b2"
            active-text-color="#ffffff"
          >
            <template v-for="m in navMenus" :key="m.menuId">
              <el-sub-menu v-if="m.menuType === 'M'" :index="'m-' + m.menuId">
                <template #title>
                  <el-icon><component :is="m.icon || 'Menu'" /></el-icon>
                  <span>{{ m.menuName }}</span>
                </template>
                <el-menu-item v-for="c in m.children || []" :key="c.menuId" :index="fullPath(m.path, c.path)">
                  <el-icon><component :is="c.icon || 'Document'" /></el-icon>
                  <template #title>
                    <div class="mi-title">
                      <span class="mi-text">{{ c.menuName }}</span>
                      <span v-if="isMessage(fullPath(m.path, c.path)) && unread" class="mi-badge">{{ badgeText }}</span>
                      <span v-if="menuBadge(c.menuId)" class="mi-badge">{{ menuBadge(c.menuId) }}</span>
                      <el-icon
                        class="mi-pin"
                        :class="{ 'is-fav': isFav(fullPath(m.path, c.path)) }"
                        @click.stop="toggleFav(fullPath(m.path, c.path))"
                      >
                        <StarFilled v-if="isFav(fullPath(m.path, c.path))" /><Star v-else />
                      </el-icon>
                    </div>
                  </template>
                </el-menu-item>
              </el-sub-menu>
              <el-menu-item v-else :index="normalize(m.path)">
                <el-icon><component :is="m.icon || 'Odometer'" /></el-icon>
                <template #title>
                  <div class="mi-title">
                    <span class="mi-text">{{ m.menuName }}</span>
                    <span v-if="isMessage(normalize(m.path)) && unread" class="mi-badge">{{ badgeText }}</span>
                    <span v-if="menuBadge(m.menuId)" class="mi-badge">{{ menuBadge(m.menuId) }}</span>
                    <el-icon
                      class="mi-pin"
                      :class="{ 'is-fav': isFav(normalize(m.path)) }"
                      @click.stop="toggleFav(normalize(m.path))"
                    >
                      <StarFilled v-if="isFav(normalize(m.path))" /><Star v-else />
                    </el-icon>
                  </div>
                </template>
              </el-menu-item>
            </template>
          </el-menu>
      </el-scrollbar>

      <div v-show="!collapsed" class="sidebar-footer">
        <span>点击 ☆ 固定常用</span>
        <span>v1.0</span>
      </div>
    </aside>

    <div class="main">
      <header class="header">
        <el-icon class="collapse-btn" :title="collapsed ? '展开侧边栏' : '收起侧边栏'" @click="toggleCollapse">
          <Expand v-if="collapsed" />
          <Fold v-else />
        </el-icon>
        <el-breadcrumb separator="/" class="crumb">
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-for="(b, i) in breadcrumbs" :key="i">{{ b }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="header-right">
          <el-tooltip content="消息中心" placement="bottom">
            <el-badge :value="unread" :hidden="!unread" :max="99" class="msg-badge">
              <el-icon class="hdr-icon" @click="$router.push('/message')"><Bell /></el-icon>
            </el-badge>
          </el-tooltip>
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user">
              <el-avatar :size="30" class="avatar">{{ (store.displayName || 'U').slice(0, 1) }}</el-avatar>
              <span class="uname">{{ store.displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>{{ store.roleNames.join(' / ') || '用户' }}</el-dropdown-item>
                <el-dropdown-item command="profile" divided>个人中心</el-dropdown-item>
                <el-dropdown-item command="release">
                  <span class="release-entry">版本更新<span v-if="store.releaseUnread" class="release-dot" /></span>
                </el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <el-dialog v-model="pwdVisible" title="修改密码" width="420px">
      <el-form :model="pwdForm" label-width="88px">
        <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="savePassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const store = useUserStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(localStorage.getItem('laminar_collapsed') === '1')
const unread = ref(0)
const navBadges = ref({})
const menuRef = ref(null)
const pwdVisible = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '' })

const FAV_KEY = 'laminar_fav_menus'
const RECENT_KEY = 'laminar_recent_menus'
const favPaths = ref(readList(FAV_KEY))
const recentPaths = ref(readList(RECENT_KEY))
let unreadTimer = null

function readList(key) {
  try { return JSON.parse(localStorage.getItem(key) || '[]') } catch (e) { return [] }
}

function normalize(p) {
  if (!p) return '/'
  return p.startsWith('/') ? p : '/' + p
}

function fullPath(parent, child) {
  if (!child) return normalize(parent)
  if (child.startsWith('/')) return child
  return normalize(parent).replace(/\/$/, '') + '/' + child
}

function buildItems(menus, parentName = '', parentIndex = null) {
  const list = []
  for (const m of menus || []) {
    if (m.menuType === 'M') {
      for (const c of m.children || []) {
        if (c.menuType === 'F') continue
        list.push({
          path: fullPath(m.path, c.path),
          name: c.menuName,
          icon: c.icon || 'Document',
          parentName: m.menuName,
          parentIndex: 'm-' + m.menuId
        })
      }
    } else if (m.menuType !== 'F') {
      list.push({
        path: normalize(m.path),
        name: m.menuName,
        icon: m.icon || 'Odometer',
        parentName,
        parentIndex
      })
    }
  }
  return list
}

const navMenus = computed(() => store.menus.filter((m) => m.menuId !== 8500))
const allItems = computed(() => buildItems(navMenus.value))
const allPaths = computed(() => allItems.value.map((i) => i.path).sort((a, b) => b.length - a.length))
const submenuIndexes = computed(() => (navMenus.value || []).filter((m) => m.menuType === 'M').map((m) => normalize(m.path)))

const activeMenu = computed(() => {
  return allPaths.value.find((p) => route.path === p || route.path.startsWith(p + '/')) || route.path
})

const openeds = computed(() => {
  const item = allItems.value.find((i) => i.path === activeMenu.value)
  return item?.parentIndex ? [item.parentIndex] : []
})

const quickItems = computed(() => {
  const merged = [...favPaths.value, ...recentPaths.value]
  const seen = new Set()
  return merged
    .filter((p) => {
      if (seen.has(p)) return false
      seen.add(p)
      return allItems.value.some((i) => i.path === p)
    })
    .map((p) => allItems.value.find((i) => i.path === p))
    .filter(Boolean)
    .slice(0, 6)
})

const badgeText = computed(() => (unread.value > 99 ? '99+' : String(unread.value)))

const breadcrumbs = computed(() => {
  const list = []
  route.matched.forEach((m) => {
    if (m.meta?.title && m.meta.title !== '首页') list.push(m.meta.title)
  })
  return list
})

function isFav(path) {
  return favPaths.value.includes(path)
}

function toggleFav(path) {
  if (!path) return
  if (favPaths.value.includes(path)) {
    favPaths.value = favPaths.value.filter((p) => p !== path)
  } else {
    favPaths.value = [path, ...favPaths.value].slice(0, 12)
  }
  localStorage.setItem(FAV_KEY, JSON.stringify(favPaths.value))
}

function trackRecent(path) {
  if (!path || !allItems.value.some((i) => i.path === path)) return
  recentPaths.value = [path, ...recentPaths.value.filter((p) => p !== path)].slice(0, 8)
  localStorage.setItem(RECENT_KEY, JSON.stringify(recentPaths.value))
}

function go(path) {
  router.push(path)
}

function isMessage(path) {
  return path === '/message'
}

function toggleCollapse() {
  collapsed.value = !collapsed.value
  localStorage.setItem('laminar_collapsed', collapsed.value ? '1' : '0')
  nextTick(openActiveParent)
}

function openActiveParent() {
  if (!menuRef.value) return
  const index = openeds.value[0]
  submenuIndexes.value.forEach((i) => {
    if (i !== index) menuRef.value.close(i)
  })
  if (index) menuRef.value.open(index)
}

function onCommand(cmd) {
  if (cmd === 'logout') store.logout()
  if (cmd === 'profile') router.push('/profile')
  if (cmd === 'release') router.push('/release')
  if (cmd === 'password') {
    pwdForm.value = { oldPassword: '', newPassword: '' }
    pwdVisible.value = true
  }
}

async function savePassword() {
  if (!pwdForm.value.newPassword || pwdForm.value.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  await request.put('/auth/password', pwdForm.value)
  ElMessage.success('密码已修改')
  pwdVisible.value = false
}

async function loadUnread() {
  try {
    const res = await request.get('/message/unread-count')
    unread.value = res.data || 0
  } catch (e) {
    unread.value = 0
  }
}

watch(activeMenu, (path) => {
  trackRecent(path)
  nextTick(openActiveParent)
}, { immediate: true })

async function loadBadges() {
  try {
    navBadges.value = (await request.get('/nav/badges')).data || {}
  } catch (e) {
    navBadges.value = {}
  }
}

function menuBadge(menuId) {
  const field = { 4100: 'alarmUnhandled', 5110: 'workorderPending', 5310: 'inspectionOverdue', 5510: 'maintenanceOverdue' }[menuId]
  if (!field) return ''
  const n = Number(navBadges.value[field] || 0)
  return n > 99 ? '99+' : n > 0 ? String(n) : ''
}

onMounted(() => {
  loadUnread()
  loadBadges()
  if (!store.mustChangePassword) store.loadReleaseNotice().catch(() => {})
  unreadTimer = setInterval(() => {
    loadUnread()
    loadBadges()
  }, 60000)
  nextTick(openActiveParent)
})

onBeforeUnmount(() => {
  if (unreadTimer) clearInterval(unreadTimer)
})
</script>

<style scoped>
.release-entry { display: inline-flex; align-items: center; gap: 6px; }
.release-dot { width: 7px; height: 7px; border-radius: 50%; background: #f56c6c; display: inline-block; }
.layout { display: flex; height: 100vh; overflow: hidden; }
.sidebar {
  width: 244px;
  background: #0b0f16;
  border-right: 1px solid rgba(255, 255, 255, .06);
  color: #e5e7eb;
  display: flex;
  flex-direction: column;
  transition: width .2s ease;
  flex-shrink: 0;
}
.sidebar.collapsed { width: 68px; }
.logo {
  height: 58px; display: flex; align-items: center; gap: 10px; padding: 0 14px;
  border-bottom: 1px solid rgba(255, 255, 255, .07); flex-shrink: 0;
}
.sidebar.collapsed .logo { justify-content: center; padding: 0; }
.logo-mark {
  width: 30px; height: 30px; border-radius: 8px; flex-shrink: 0;
  background: #1f6feb;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 12px; letter-spacing: .4px; color: #fff;
}
.logo-text { display: flex; flex-direction: column; line-height: 1.2; white-space: nowrap; min-width: 0; }
.logo-text b { font-size: 14px; font-weight: 600; color: #f3f4f6; letter-spacing: -.2px; }
.logo-text span { font-size: 9px; color: #6b7280; letter-spacing: .3px; margin-top: 2px; }

.menu-scroll { flex: 1; padding: 10px 8px 8px; }
.nav-caption {
  padding: 10px 8px 4px;
  font-size: 10px; line-height: 1.2; color: #6b7280;
  letter-spacing: .08em; text-transform: uppercase;
}
.nav-caption-all { padding-top: 12px; }

.quick-row {
  width: 100%; min-height: 34px;
  display: flex; align-items: center; gap: 9px;
  padding: 0 8px; margin: 1px 0;
  border: 0; border-radius: 6px;
  background: transparent; color: #9aa4b2;
  font-family: inherit; font-size: 13px; text-align: left;
  cursor: pointer;
}
.quick-row:hover { background: rgba(255, 255, 255, .05); color: #e5e7eb; }
.quick-row.active { background: rgba(31, 111, 235, .16); color: #fff; box-shadow: inset 2px 0 0 #3b82f6; }
.quick-row > .el-icon { font-size: 16px; flex-shrink: 0; }
.row-name { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.row-pin {
  display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px; border-radius: 4px; flex-shrink: 0;
  color: #5f6b7a; opacity: 0; transition: opacity .12s ease, color .12s ease;
}
.quick-row:hover .row-pin, .row-pin.is-fav { opacity: 1; }
.row-pin:hover { color: #f3f4f6; background: rgba(255, 255, 255, .08); }
.row-pin.is-fav { color: #93c5fd; }
.row-pin .el-icon { font-size: 13px; }
.row-badge, .mi-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 16px; height: 16px; padding: 0 4px;
  border-radius: 8px; background: #ef4444; color: #fff;
  font-size: 9px; font-weight: 600; line-height: 1;
}

.menu-scroll :deep(.el-menu) { background: transparent; border-right: 0; }
.menu-scroll :deep(.el-menu-item),
.menu-scroll :deep(.el-sub-menu__title) {
  height: 38px; line-height: 38px;
  margin: 2px 0; padding-left: 10px !important; padding-right: 8px !important;
  border-radius: 6px; color: #9aa4b2; font-size: 13px;
}
.menu-scroll :deep(.el-menu-item:hover),
.menu-scroll :deep(.el-sub-menu__title:hover) { background: rgba(255, 255, 255, .05); color: #e5e7eb; }
.menu-scroll :deep(.el-menu-item.is-active) {
  background: rgba(31, 111, 235, .16); color: #fff;
  box-shadow: inset 2px 0 0 #3b82f6;
}
.menu-scroll :deep(.el-sub-menu.is-active > .el-sub-menu__title) { color: #e5e7eb; }
.menu-scroll :deep(.el-menu-item .el-icon),
.menu-scroll :deep(.el-sub-menu__title .el-icon) { font-size: 16px; margin-right: 9px; }
.menu-scroll :deep(.el-sub-menu .el-menu-item) { padding-left: 34px !important; min-width: 0; }
.menu-scroll :deep(.el-sub-menu .el-menu-item .el-icon) { font-size: 14px; margin-right: 7px; }
.mi-title { display: flex; align-items: center; width: 100%; min-width: 0; gap: 6px; }
.sidebar.collapsed .mi-title { display: none; }
.sidebar.collapsed .menu-scroll :deep(.el-menu-item),
.sidebar.collapsed .menu-scroll :deep(.el-sub-menu__title) {
  padding: 0 !important;
  justify-content: center;
}
.sidebar.collapsed .menu-scroll :deep(.el-menu-item .el-icon),
.sidebar.collapsed .menu-scroll :deep(.el-sub-menu__title .el-icon) { margin-right: 0; }
.sidebar.collapsed .menu-scroll :deep(.el-sub-menu__title > span) { display: none; }
.mi-text { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mi-pin {
  margin-left: auto !important; margin-right: 0 !important;
  width: 20px; height: 20px; border-radius: 4px;
  font-size: 12px !important; color: #5f6b7a; opacity: 0;
  flex-shrink: 0; transition: opacity .12s ease, color .12s ease;
}
.el-menu-item:hover .mi-pin, .mi-pin.is-fav { opacity: 1; }
.mi-pin:hover { color: #f3f4f6; background: rgba(255, 255, 255, .08); }
.mi-pin.is-fav { color: #93c5fd; }
.mi-badge { margin-left: auto; flex-shrink: 0; }

.sidebar-footer {
  height: 34px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 14px; border-top: 1px solid rgba(255, 255, 255, .06);
  color: #5f6b7a; font-size: 10px;
}

.main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.header {
  height: 58px; background: #fff; border-bottom: 1px solid var(--border);
  display: flex; align-items: center; gap: 14px; padding: 0 20px; flex-shrink: 0;
}
.collapse-btn { font-size: 19px; cursor: pointer; color: #5a6b80; }
.crumb { flex: 1; }
.header-right { display: flex; align-items: center; gap: 18px; }
.hdr-icon { font-size: 19px; color: #5a6b80; cursor: pointer; }
.hdr-icon:hover { color: var(--brand); }
.msg-badge :deep(.el-badge__content) { top: 2px; }
.user { display: flex; align-items: center; gap: 8px; cursor: pointer; color: #33465c; }
.avatar { background: #1f6feb; font-size: 14px; }
.uname { font-size: 13px; max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.content { flex: 1; overflow: auto; }
</style>
