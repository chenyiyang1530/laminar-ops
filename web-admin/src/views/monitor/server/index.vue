<template>
  <div class="app-container">
    <el-row :gutter="14">
      <el-col :xs="12" :sm="6"><div class="card kpi"><div class="n">{{ info.processors || '-' }}</div><div class="l">CPU 核数</div></div></el-col>
      <el-col :xs="12" :sm="6"><div class="card kpi"><div class="n">{{ uptime }}</div><div class="l">运行时长</div></div></el-col>
      <el-col :xs="12" :sm="6"><div class="card kpi"><div class="n">{{ memUsed }}%</div><div class="l">堆内存使用</div></div></el-col>
      <el-col :xs="12" :sm="6"><div class="card kpi"><div class="n">{{ online.length }}</div><div class="l">在线用户</div></div></el-col>
    </el-row>

    <div class="page-card" style="margin-top:14px">
      <div class="page-title"><div><h2>服务监控</h2><div class="sub">{{ info.os }} · Java {{ info.javaVersion }}</div></div></div>
      <el-progress :percentage="Number(memUsed)" :stroke-width="14" :status="memUsed > 85 ? 'exception' : ''" />
      <div class="mem">{{ fmt(info.totalMemory - info.freeMemory) }} / {{ fmt(info.totalMemory) }}（最大 {{ fmt(info.maxMemory) }}）</div>
    </div>

    <div class="page-card" style="margin-top:14px">
      <div class="page-title"><div><h2>后端启动任务</h2><div class="sub">LaminarOpsBackend · 开机自动启动与崩溃恢复</div></div></div>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="任务状态">{{ task.state || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="上次运行">{{ task.lastRunTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="返回码">{{ task.lastTaskResult ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="失败原因" :span="2">{{ task.failureReason || '—' }}</el-descriptions-item>
        <el-descriptions-item label="下次运行">{{ task.nextRunTime || '—' }}</el-descriptions-item>
      </el-descriptions>
      <div style="margin-top:10px; display:flex; gap:8px">
        <el-button size="small" @click="openLog('log')">查看启动日志</el-button>
        <el-button size="small" @click="openLog('err')">查看错误日志</el-button>
      </div>
    </div>

    <el-dialog v-model="logDialog" title="后端启动日志" width="860px">
      <pre class="task-log">{{ logContent }}</pre>
    </el-dialog>

    <div class="page-card" style="margin-top:14px">
      <div class="page-title"><div><h2>在线用户</h2><div class="sub">Redis 会话，30 分钟无操作自动过期</div></div></div>
      <el-table :data="online" size="small">
        <el-table-column type="index" label="序号" width="62" align="center" />
        <el-table-column prop="username" label="账号" width="140" />
        <el-table-column prop="nickName" label="姓名" width="120" />
        <el-table-column prop="ip" label="IP" width="150" />
        <el-table-column prop="loginTime" label="登录时间" width="170" />
        <el-table-column label="剩余有效期">
        <template #default="{ row }">{{ row.ttl > 0 ? Math.round(row.ttl / 60) + ' 分钟' : '—' }}</template></el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '@/utils/request'

const info = ref({})
const online = ref([])
const task = ref({})
const logDialog = ref(false)
const logContent = ref('')
const uptime = computed(() => {
  const s = info.value.uptime || 0
  return Math.floor(s / 3600) + ' 小时 ' + Math.floor((s % 3600) / 60) + ' 分'
})
const memUsed = computed(() => {
  if (!info.value.totalMemory) return 0
  return Math.round(((info.value.totalMemory - info.value.freeMemory) / info.value.totalMemory) * 100)
})
const fmt = (b) => b == null ? '-' : (b / 1024 / 1024).toFixed(1) + ' MB'
async function openLog(type) {
  const res = await request.get('/system/monitor/app-task/log', { params: { type, lines: 300 } })
  logContent.value = res.data.content || '暂无日志'
  logDialog.value = true
}
onMounted(async () => {
  info.value = (await request.get('/system/monitor/server')).data
  online.value = (await request.get('/system/monitor/online')).data
  task.value = (await request.get('/system/monitor/app-task')).data
})
</script>

<style scoped>
.card { background: #fff; border: 1px solid #eef2f7; border-radius: 10px; padding: 14px 16px; }
.kpi .n { font-size: 22px; font-weight: 700; color: #1f6feb; }
.kpi .l { font-size: 11px; color: #8494a8; margin-top: 4px; }
.mem { font-size: 12px; color: #8494a8; margin-top: 8px; }
.task-log { max-height: 420px; overflow: auto; padding: 12px; background: #0f172a; color: #dbeafe; border-radius: 8px; font-size: 12px; line-height: 1.55; white-space: pre-wrap; }
</style>
