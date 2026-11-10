<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canBudget" label="预算管理" name="budget" />
        <el-tab-pane v-if="canReport" label="统计报表" name="report" />
      </el-tabs>
    </div>
    <CrudPage v-if="active === 'budget'" resource="budget" />
    <ReportPage v-else-if="active === 'report'" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import ReportPage from '@/views/report/index.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canBudget = computed(() => store.hasPerm('budget:list'))
const canReport = computed(() => store.hasPerm('report:view'))
const active = ref(canBudget.value ? 'budget' : 'report')
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
