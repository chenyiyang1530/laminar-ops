<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canPlan" label="巡检计划" name="plan" />
        <el-tab-pane v-if="canTask" label="巡检任务" name="task" />
      </el-tabs>
    </div>
    <CrudPage v-if="active === 'plan'" resource="inspectionPlan" />
    <TaskPanel v-else-if="active === 'task'" type="inspection" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import TaskPanel from '@/components/TaskPanel.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canPlan = computed(() => store.hasPerm('inspection:plan:list'))
const canTask = computed(() => store.hasPerm('inspection:task:list'))
const active = ref(canPlan.value ? 'plan' : 'task')
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
