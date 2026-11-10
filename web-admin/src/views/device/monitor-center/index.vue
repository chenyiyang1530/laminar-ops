<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canRealtime" label="实时监测" name="realtime" />
        <el-tab-pane v-if="canPoint" label="监测点位" name="point" />
        <el-tab-pane v-if="canRule" label="告警规则" name="rule" />
      </el-tabs>
    </div>
    <RealtimePage v-if="active === 'realtime'" />
    <CrudPage v-else-if="active === 'point'" resource="monitorPoint" />
    <CrudPage v-else-if="active === 'rule'" resource="alarmRule" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import RealtimePage from '@/views/device/realtime/index.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canRealtime = computed(() => store.hasPerm('device:realtime:view'))
const canPoint = computed(() => store.hasPerm('device:point:list'))
const canRule = computed(() => store.hasPerm('device:rule:list'))
const active = ref(canRealtime.value ? 'realtime' : canPoint.value ? 'point' : 'rule')
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
