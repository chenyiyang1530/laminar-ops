<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canInspectionPlan" label="巡检计划" name="inspectionPlan" />
        <el-tab-pane v-if="canInspectionTask" label="巡检任务" name="inspectionTask" />
        <el-tab-pane v-if="canMaintenancePlan" label="保养计划" name="maintenancePlan" />
        <el-tab-pane v-if="canMaintenanceTask" label="保养任务" name="maintenanceTask" />
      </el-tabs>
    </div>
    <CrudPage v-if="active === 'inspectionPlan'" resource="inspectionPlan" />
    <TaskPanel v-else-if="active === 'inspectionTask'" type="inspection" />
    <CrudPage v-else-if="active === 'maintenancePlan'" resource="maintenancePlan" />
    <TaskPanel v-else-if="active === 'maintenanceTask'" type="maintenance" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CrudPage from '@/components/CrudPage.vue'
import TaskPanel from '@/components/TaskPanel.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const route = useRoute()
const router = useRouter()
const canInspectionPlan = computed(() => store.hasPerm('inspection:plan:list'))
const canInspectionTask = computed(() => store.hasPerm('inspection:task:list'))
const canMaintenancePlan = computed(() => store.hasPerm('maintenance:plan:list'))
const canMaintenanceTask = computed(() => store.hasPerm('maintenance:task:list'))

const allowed = {
  inspectionPlan: canInspectionPlan,
  inspectionTask: canInspectionTask,
  maintenancePlan: canMaintenancePlan,
  maintenanceTask: canMaintenanceTask
}
function pickTab(name) {
  return allowed[name]?.value ? name : ''
}
const fallback = canInspectionPlan.value ? 'inspectionPlan'
  : canInspectionTask.value ? 'inspectionTask'
    : canMaintenancePlan.value ? 'maintenancePlan' : 'maintenanceTask'
const active = ref(pickTab(route.query.tab) || fallback)

watch(() => route.query.tab, (tab) => {
  const next = pickTab(tab)
  if (next && next !== active.value) active.value = next
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
