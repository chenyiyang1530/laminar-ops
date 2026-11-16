<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canCompany" label="公司信息" name="company" />
        <el-tab-pane v-if="canContract" label="合同管理" name="contract" />
      </el-tabs>
    </div>
    <CrudPage
      v-if="active === 'company'"
      resource="company"
      :hide-add="!store.isSuperAdmin"
      :hide-remove="!store.isSuperAdmin"
      :form-defaults="companyDefaults"
    />
    <CrudPage v-else-if="active === 'contract'" resource="contract" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import CrudPage from '@/components/CrudPage.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const route = useRoute()
const canCompany = computed(() => store.hasPerm('customer:company:list'))
const canContract = computed(() => store.hasPerm('customer:contract:list'))
const companyDefaults = computed(() => ({
  contact: store.user?.nickName || '',
  phone: store.user?.phone || ''
}))
function pickTab(tab) {
  if (tab === 'contract' && canContract.value) return 'contract'
  return canCompany.value ? 'company' : 'contract'
}
const active = ref(pickTab(route.query.tab))
watch(() => route.query.tab, (tab) => { active.value = pickTab(tab) })
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
