<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canPart" label="备件库存" name="part" />
        <el-tab-pane v-if="canRequisition" label="领用记录" name="requisition" />
        <el-tab-pane v-if="canPart" label="兼容设备" name="equipment" />
        <el-tab-pane v-if="canPart" label="库存调整" name="stock" />
      </el-tabs>
    </div>
    <CrudPage v-if="active === 'part'" resource="part" />
    <RequisitionPage v-else-if="active === 'requisition'" />
    <CrudPage v-else-if="active === 'equipment'" resource="partEquipment" />
    <PartStockPage v-else-if="active === 'stock'" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import CrudPage from '@/components/CrudPage.vue'
import RequisitionPage from '@/views/resource/requisition/index.vue'
import PartStockPage from '@/views/resource/parts/stock.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const route = useRoute()
const canPart = computed(() => store.hasPerm('resource:part:list'))
const canRequisition = computed(() => store.hasPerm('resource:requisition:list'))
function pickTab(tab) {
  if (tab === 'requisition' && canRequisition.value) return 'requisition'
  if (tab === 'equipment' && canPart.value) return 'equipment'
  if (tab === 'stock' && canPart.value) return 'stock'
  return canPart.value ? 'part' : 'requisition'
}
const active = ref(pickTab(route.query.tab))
watch(() => route.query.tab, (tab) => { active.value = pickTab(tab) })
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
