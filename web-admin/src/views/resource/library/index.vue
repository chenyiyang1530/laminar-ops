<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canDocument" label="技术文档" name="document" />
        <el-tab-pane v-if="canKnowledge" label="运维知识" name="knowledge" />
      </el-tabs>
    </div>
    <CrudPage v-if="active === 'document'" resource="document" />
    <CrudPage v-else-if="active === 'knowledge'" resource="knowledge" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canDocument = computed(() => store.hasPerm('resource:document:list'))
const canKnowledge = computed(() => store.hasPerm('resource:knowledge:list'))
const active = ref(canDocument.value ? 'document' : 'knowledge')
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
