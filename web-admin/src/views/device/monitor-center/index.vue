<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canRealtime" label="实时监测" name="realtime" />
        <el-tab-pane v-if="canPoint" label="监测点位" name="point" />
        <el-tab-pane v-if="canRule" label="告警规则" name="rule" />
        <el-tab-pane v-if="canAccess" label="接入配置" name="access" />
        <el-tab-pane v-if="canAlarm" label="告警中心" name="alarm" />
      </el-tabs>
    </div>
    <RealtimePage v-if="active === 'realtime'" />
    <CrudPage v-else-if="active === 'point'" resource="monitorPoint" />
    <CrudPage v-else-if="active === 'rule'" resource="alarmRule" />
    <MonitorAccessPage v-else-if="active === 'access'" />
    <AlarmPage v-else-if="active === 'alarm'" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CrudPage from '@/components/CrudPage.vue'
import RealtimePage from '@/views/device/realtime/index.vue'
import MonitorAccessPage from '@/views/device/access/index.vue'
import AlarmPage from '@/views/alarm/index.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const route = useRoute()
const router = useRouter()
const canRealtime = computed(() => store.hasPerm('device:realtime:view'))
const canPoint = computed(() => store.hasPerm('device:point:list'))
const canRule = computed(() => store.hasPerm('device:rule:list'))
const canAccess = computed(() => store.hasPerm('device:equipment:list') && !store.isHospitalUser)
const canAlarm = computed(() => store.hasPerm('alarm:list'))

const tabs = {
  realtime: canRealtime,
  point: canPoint,
  rule: canRule,
  access: canAccess,
  alarm: canAlarm
}
const defaultTab = canRealtime.value ? 'realtime' : canPoint.value ? 'point' : canRule.value ? 'rule' : canAccess.value ? 'access' : 'alarm'
const active = ref(tabs[route.query.tab]?.value ? route.query.tab : defaultTab)

watch(() => route.query.tab, (tab) => {
  if (tabs[tab]?.value) active.value = tab
})

watch(active, (tab) => {
  if (route.query.tab === tab) return
  router.replace({ path: route.path, query: { ...route.query, tab } })
})
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
