<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canDept" label="部门" name="dept" />
        <el-tab-pane v-if="canPost" label="岗位" name="post" />
      </el-tabs>
    </div>
    <DeptPage v-if="active === 'dept'" />
    <CrudPage v-else-if="active === 'post'" resource="post" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import DeptPage from '@/views/system/dept/index.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canDept = computed(() => store.hasPerm('system:dept:list'))
const canPost = computed(() => store.hasPerm('system:post:list'))
const active = ref(canDept.value ? 'dept' : 'post')
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
