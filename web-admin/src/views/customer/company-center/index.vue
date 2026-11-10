<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canCompany" label="公司信息" name="company" />
        <el-tab-pane v-if="canRegion" label="服务区域" name="region" />
      </el-tabs>
    </div>
    <CrudPage
      v-if="active === 'company'"
      resource="company"
      :hide-add="!store.isSuperAdmin"
      :hide-remove="!store.isSuperAdmin"
      :form-defaults="companyDefaults"
    />
    <CrudPage v-else-if="active === 'region'" resource="companyRegion" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canCompany = computed(() => store.hasPerm('customer:company:list'))
const canRegion = computed(() => store.hasPerm('customer:companyregion:list'))
const companyDefaults = computed(() => ({
  contact: store.user?.nickName || '',
  phone: store.user?.phone || ''
}))
const active = ref(canCompany.value ? 'company' : 'region')
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
