<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canHospital" label="医院" name="hospital" />
        <el-tab-pane v-if="canCampus" label="院区" name="campus" />
        <el-tab-pane v-if="canDepartment" label="科室" name="department" />
        <el-tab-pane v-if="canContact" label="对接人" name="contact" />
        <el-tab-pane v-if="canEquipment" label="设备台账" name="equipment" />
        <el-tab-pane v-if="canDeviceType" label="设备类型" name="deviceType" />
      </el-tabs>
    </div>
    <HospitalPage v-if="active === 'hospital'" />
    <CrudPage v-else-if="active === 'campus'" resource="campus" />
    <CrudPage v-else-if="active === 'department'" resource="department" />
    <CrudPage v-else-if="active === 'contact'" resource="contact" />
    <CrudPage v-else-if="active === 'equipment'" resource="equipment" :max-columns="8" />
    <CrudPage v-else-if="active === 'deviceType'" resource="deviceType" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CrudPage from '@/components/CrudPage.vue'
import HospitalPage from '@/views/customer/hospital/index.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const route = useRoute()
const router = useRouter()
const canHospital = computed(() => store.hasPerm('customer:hospital:list'))
const canCampus = computed(() => store.hasPerm('customer:campus:list'))
const canDepartment = computed(() => store.hasPerm('customer:department:list'))
const canContact = computed(() => store.hasPerm('customer:contact:list'))
const canEquipment = computed(() => store.hasPerm('device:equipment:list'))
const canDeviceType = computed(() => store.hasPerm('device:type:list'))

const tabPerms = {
  hospital: canHospital,
  campus: canCampus,
  department: canDepartment,
  contact: canContact,
  equipment: canEquipment,
  deviceType: canDeviceType
}

function allowedTab(name) {
  return tabPerms[name]?.value ? name : ''
}

const active = ref(allowedTab(route.query.tab)
  || (canHospital.value ? 'hospital' : canCampus.value ? 'campus' : canDepartment.value ? 'department' : canContact.value ? 'contact' : canEquipment.value ? 'equipment' : 'deviceType'))

watch(() => route.query.tab, (tab) => {
  const next = allowedTab(tab)
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
