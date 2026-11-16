<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canDept" label="部门" name="dept" />
        <el-tab-pane v-if="canPost" label="岗位" name="post" />
        <el-tab-pane v-if="canRegion" label="地理区域" name="region" />
        <el-tab-pane v-if="canQualification" label="公司资质" name="qualification" />
      </el-tabs>
    </div>
    <DeptPage v-if="active === 'dept'" />
    <CrudPage v-else-if="active === 'post'" resource="post" />
    <CrudPage v-else-if="active === 'region'" resource="region" />
    <CrudPage v-else-if="active === 'qualification'" resource="companyQualification" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import CrudPage from '@/components/CrudPage.vue'
import DeptPage from '@/views/system/dept/index.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const route = useRoute()
const canDept = computed(() => store.hasPerm('system:dept:list'))
const canPost = computed(() => store.hasPerm('system:post:list'))
const canRegion = computed(() => store.hasPerm('customer:region:list'))
const canQualification = computed(() => store.hasPerm('customer:qualification:list'))
function pickTab(tab) {
  if (tab === 'post' && canPost.value) return 'post'
  if (tab === 'region' && canRegion.value) return 'region'
  if (tab === 'qualification' && canQualification.value) return 'qualification'
  if (canDept.value) return 'dept'
  if (canPost.value) return 'post'
  if (canRegion.value) return 'region'
  return 'qualification'
}
const active = ref(pickTab(route.query.tab))
watch(() => route.query.tab, (tab) => { active.value = pickTab(tab) })
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
