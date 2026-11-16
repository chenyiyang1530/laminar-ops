<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canConfig" label="审批配置" name="config" />
        <el-tab-pane v-if="canDelegate" label="审批代理" name="delegate" />
      </el-tabs>
    </div>
    <ApprovalConfigPage v-if="active === 'config'" />
    <ApprovalDelegatePage v-else-if="active === 'delegate'" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ApprovalConfigPage from '@/views/staff/approval-config/index.vue'
import ApprovalDelegatePage from '@/views/staff/approval-delegate/index.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const route = useRoute()
const canConfig = computed(() => store.hasPerm('staff:approval:config'))
const canDelegate = computed(() => store.hasPerm('staff:leave:list'))
function pickTab(tab) {
  if (tab === 'delegate' && canDelegate.value) return 'delegate'
  if (canConfig.value) return 'config'
  return 'delegate'
}
const active = ref(pickTab(route.query.tab))
watch(() => route.query.tab, (tab) => { active.value = pickTab(tab) })
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
