<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canWorkorder" label="工单管理" name="workorder" />
        <el-tab-pane v-if="canDispatch" label="智能派单" name="dispatch" />
      </el-tabs>
    </div>
    <WorkorderPanel v-if="active === 'workorder'" />
    <DispatchPage v-else-if="active === 'dispatch'" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import WorkorderPanel from '@/components/WorkorderPanel.vue'
import DispatchPage from '@/views/staff/dispatch/index.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canWorkorder = computed(() => store.hasPerm('workorder:list'))
const canDispatch = computed(() => store.hasPerm('staff:dispatch:view'))
const active = ref(canWorkorder.value ? 'workorder' : 'dispatch')
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
