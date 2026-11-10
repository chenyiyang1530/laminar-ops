<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title"><div><h2>缓存监控</h2><div class="sub">Redis 运行状态</div></div></div>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item v-for="item in rows" :key="item.key" :label="item.label">{{ item.value }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '@/utils/request'

const info = ref({})
const labels = {
  redis_version: 'Redis 版本',
  uptime_in_days: '运行天数',
  connected_clients: '客户端连接',
  used_memory_human: '已用内存',
  used_memory_peak_human: '内存峰值',
  total_commands_processed: '命令总数',
  keyspace_hits: '命中次数',
  keyspace_misses: '未命中次数',
  db0: 'DB0 keys'
}
const rows = computed(() => Object.entries(labels).map(([k, label]) => ({ key: k, label, value: info.value[k] ?? '—' })))
onMounted(async () => { info.value = (await request.get('/system/monitor/cache')).data.info || {} })
</script>
