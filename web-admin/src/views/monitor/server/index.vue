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
const uptime = computed(() => {
  const s = info.value.uptime || 0
  return Math.floor(s / 3600) + ' 小时 ' + Math.floor((s % 3600) / 60) + ' 分'
})
const memUsed = computed(() => {
  if (!info.value.totalMemory) return 0
  return Math.round(((info.value.totalMemory - info.value.freeMemory) / info.value.totalMemory) * 100)
})
const fmt = (b) => b == null ? '-' : (b / 1024 / 1024).toFixed(1) + ' MB'
onMounted(async () => {
  info.value = (await request.get('/system/monitor/server')).data
  online.value = (await request.get('/system/monitor/online')).data
})
</script>

<style scoped>
.card { background: #fff; border: 1px solid #eef2f7; border-radius: 10px; padding: 14px 16px; }
.kpi .n { font-size: 22px; font-weight: 700; color: #1f6feb; }
.kpi .l { font-size: 11px; color: #8494a8; margin-top: 4px; }
.mem { font-size: 12px; color: #8494a8; margin-top: 8px; }
</style>
