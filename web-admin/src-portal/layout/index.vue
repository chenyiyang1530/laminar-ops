<template>
  <div class="layout">
    <aside class="side">
      <div class="brand">
        <div class="mark">医</div>
        <div class="txt"><b>洁净设备运维</b><span>院方协同平台</span></div>
      </div>
      <el-menu :default-active="$route.path" router background-color="transparent" text-color="#a7c3c0" active-text-color="#fff" class="menu">
        <el-menu-item index="/dashboard"><el-icon><Odometer /></el-icon><span>本院概览</span></el-menu-item>
        <el-menu-item index="/monitor"><el-icon><Monitor /></el-icon><span>设备监测</span></el-menu-item>
        <el-menu-item index="/alarms"><el-icon><Warning /></el-icon><span>设备告警</span></el-menu-item>
        <el-menu-item index="/workorders"><el-icon><Tools /></el-icon><span>报修工单</span></el-menu-item>
        <el-menu-item index="/messages"><el-icon><Bell /></el-icon><span>消息通知</span><el-badge v-if="unread" :value="unread" class="badge" /></el-menu-item>
      </el-menu>
      <div class="foot">数据仅限本院范围</div>
    </aside>
    <div class="main">
      <header class="header">
        <div class="hosp">{{ store.profile?.hospitalName || '医院' }}<span>{{ store.profile?.hospitalCode }}</span></div>
        <el-dropdown @command="logout">
          <span class="user">{{ store.profile?.nickName }} <el-icon><ArrowDown /></el-icon></span>
          <template #dropdown><el-dropdown-menu><el-dropdown-item command="logout">退出登录</el-dropdown-item></el-dropdown-menu></template>
        </el-dropdown>
      </header>
      <main class="content"><router-view /></main>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/request'
import { store } from '@/store'

const router = useRouter()
const unread = ref(0)

async function loadUnread() {
  try { unread.value = (await request.get('/portal/messages/unread-count')).data } catch (e) { unread.value = 0 }
}
function logout() {
  localStorage.removeItem('portal_token')
  store.token = ''
  store.profile = null
  router.replace('/login')
}
onMounted(() => { loadUnread(); setInterval(loadUnread, 60000) })
</script>

<style scoped>
.layout { display: flex; height: 100vh; overflow: hidden; }
.side { width: 212px; background: linear-gradient(180deg, #0d3b38, #0b2a2a); color: #fff; display: flex; flex-direction: column; flex-shrink: 0; }
.brand { display: flex; gap: 10px; align-items: center; padding: 18px 16px; border-bottom: 1px solid rgba(255,255,255,.07); }
.mark { width: 34px; height: 34px; border-radius: 9px; background: linear-gradient(135deg, #14b8a6, #06b6d4); display: flex; align-items: center; justify-content: center; font-weight: 700; }
.txt { display: flex; flex-direction: column; line-height: 1.2; }
.txt b { font-size: 14px; } .txt span { font-size: 10px; color: #7fa9a4; margin-top: 3px; }
.menu { flex: 1; border-right: none; padding-top: 8px; }
.menu :deep(.el-menu-item) { height: 44px; line-height: 44px; margin: 3px 8px; border-radius: 8px; }
.menu :deep(.el-menu-item.is-active) { background: linear-gradient(90deg, #0f766e, #14b8a6); }
.badge { margin-left: auto; }
.foot { padding: 12px; text-align: center; font-size: 10px; color: #5f8783; }
.main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.header { height: 56px; background: #fff; border-bottom: 1px solid #e8eef4; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; }
.hosp { font-weight: 600; } .hosp span { color: #94a3b8; font-weight: 400; font-size: 12px; margin-left: 8px; }
.user { cursor: pointer; color: #475569; display: flex; align-items: center; gap: 6px; }
.content { flex: 1; overflow: auto; }
</style>
