<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane label="服务监控" name="server" />
        <el-tab-pane label="缓存监控" name="cache" />
        <el-tab-pane label="数据监控" name="data" />
        <el-tab-pane label="远程控制" name="control" />
      </el-tabs>
    </div>
    <ServerMonitor v-if="active === 'server'" />
    <CacheMonitor v-else-if="active === 'cache'" />
    <ControlMonitor v-else-if="active === 'control'" />
    <DataMonitor v-else />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ServerMonitor from '@/views/monitor/server/index.vue'
import CacheMonitor from '@/views/monitor/cache/index.vue'
import DataMonitor from '@/views/monitor/data/index.vue'
import ControlMonitor from '@/views/monitor/control/index.vue'

const active = ref('server')
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
