<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canPart" label="备件库存" name="part" />
        <el-tab-pane v-if="canRequisition" label="领用记录" name="requisition" />
      </el-tabs>
    </div>
    <CrudPage v-if="active === 'part'" resource="part" />
    <RequisitionPage v-else-if="active === 'requisition'" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import CrudPage from '@/components/CrudPage.vue'
import RequisitionPage from '@/views/resource/requisition/index.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const route = useRoute()
const canPart = computed(() => store.hasPerm('resource:part:list'))
const canRequisition = computed(() => store.hasPerm('resource:requisition:list'))
function pickTab(tab) {
  if (tab === 'requisition' && canRequisition.value) return 'requisition'
  return canPart.value ? 'part' : 'requisition'
}
const active = ref(pickTab(route.query.tab))
watch(() => route.query.tab, (tab) => { active.value = pickTab(tab) })
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
