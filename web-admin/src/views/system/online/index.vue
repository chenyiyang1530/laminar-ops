<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div>
          <h2>在线用户</h2>
          <div class="sub">Redis 会话，按平台“会话空闲超时”参数自动过期；仅系统管理员可见</div>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column type="index" label="序号" width="62" align="center" />
        <el-table-column prop="username" label="账号" width="160" />
        <el-table-column prop="nickName" label="姓名" width="140" />
        <el-table-column prop="ip" label="登录 IP" width="150" />
        <el-table-column prop="location" label="登录地点" width="130" />
        <el-table-column prop="browser" label="浏览器" min-width="130" show-overflow-tooltip />
        <el-table-column prop="os" label="操作系统" width="130" />
        <el-table-column prop="loginTime" label="登录时间" width="180" />
        <el-table-column label="剩余有效期" width="120">
          <template #default="{ row }">{{ row.ttl > 0 ? Math.round(row.ttl / 60) + ' 分钟' : '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default><el-tag type="success" size="small" effect="plain">在线</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="kick(row)">强退</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="当前没有在线用户" :image-size="46" /></template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()

const loading = ref(false)
const records = ref([])

async function load() {
  loading.value = true
  try {
    records.value = (await request.get('/system/monitor/online')).data || []
  } finally {
    loading.value = false
  }
}

async function kick(row) {
  await ElMessageBox.confirm(
    `确认强制退出「${row.nickName || row.username}」？退出后该账号需要重新登录。`,
    '强退确认',
    { type: 'warning' }
  )
  await request.delete('/system/monitor/online/' + row.userId)
  if (String(row.userId) === String(store.user?.userId)) {
    localStorage.removeItem('laminar_token')
    window.location.href = (window.__APP_BASE__ || '/') + 'login'
    return
  }
  ElMessage.success('已强退，对方将在 3 秒内退出登录')
  load()
}

onMounted(load)
</script>
