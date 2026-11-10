<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canHospital" label="医院" name="hospital" />
        <el-tab-pane v-if="canCampus" label="院区" name="campus" />
        <el-tab-pane v-if="canDepartment" label="科室" name="department" />
        <el-tab-pane v-if="canCleanArea" label="洁净区域" name="cleanArea" />
        <el-tab-pane v-if="canContact" label="对接人" name="contact" />
      </el-tabs>
    </div>
    <HospitalPage v-if="active === 'hospital'" />
    <CrudPage v-else-if="active === 'campus'" resource="campus" />
    <CrudPage v-else-if="active === 'department'" resource="department" />
    <CrudPage v-else-if="active === 'cleanArea'" resource="cleanArea" />
    <CrudPage v-else-if="active === 'contact'" resource="contact" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import HospitalPage from '@/views/customer/hospital/index.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canHospital = computed(() => store.hasPerm('customer:hospital:list'))
const canCampus = computed(() => store.hasPerm('customer:campus:list'))
const canDepartment = computed(() => store.hasPerm('customer:department:list'))
const canCleanArea = computed(() => store.hasPerm('customer:cleanarea:list'))
const canContact = computed(() => store.hasPerm('customer:contact:list'))
const active = ref(canHospital.value ? 'hospital' : canCampus.value ? 'campus' : canDepartment.value ? 'department' : canCleanArea.value ? 'cleanArea' : 'contact')
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
