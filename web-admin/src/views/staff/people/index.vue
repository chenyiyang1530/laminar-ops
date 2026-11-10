<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canEngineer" label="工程师" name="engineer" />
        <el-tab-pane v-if="canSkill" label="技能标签" name="skill" />
        <el-tab-pane v-if="canCertificate" label="资质证书" name="certificate" />
      </el-tabs>
    </div>
    <CrudPage v-if="active === 'engineer'" resource="engineer" />
    <CrudPage v-else-if="active === 'skill'" resource="skill" />
    <CrudPage v-else-if="active === 'certificate'" resource="certificate" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import CrudPage from '@/components/CrudPage.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canEngineer = computed(() => store.hasPerm('staff:engineer:list'))
const canSkill = computed(() => store.hasPerm('staff:skill:list'))
const canCertificate = computed(() => store.hasPerm('staff:certificate:list'))
const active = ref(canEngineer.value ? 'engineer' : canSkill.value ? 'skill' : 'certificate')
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
