<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canEquipment" label="设备台账" name="equipment" />
        <el-tab-pane v-if="canType" label="设备类型" name="type" />
      </el-tabs>
    </div>
    <CrudPage v-if="active === 'equipment'" resource="equipment" />
    <CrudPage v-else-if="active === 'type'" resource="deviceType" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canEquipment = computed(() => store.hasPerm('device:equipment:list'))
const canType = computed(() => store.hasPerm('device:type:list'))
const active = ref(canEquipment.value ? 'equipment' : 'type')
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
