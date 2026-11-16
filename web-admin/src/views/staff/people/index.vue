<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canEngineer" label="工程师" name="engineer" />
        <el-tab-pane v-if="canSkill" label="技能标签" name="skill" />
        <el-tab-pane v-if="canCertificate" label="员工证书" name="certificate" />
        <el-tab-pane v-if="canUser" label="用户" name="user" />
        <el-tab-pane v-if="canRole" label="角色" name="role" />
      </el-tabs>
    </div>
    <CrudPage v-if="active === 'engineer'" resource="engineer" />
    <CrudPage v-else-if="active === 'skill'" resource="skill" />
    <CrudPage v-else-if="active === 'certificate'" resource="certificate" />
    <UserPage v-else-if="active === 'user'" />
    <RolePage v-else-if="active === 'role'" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import CrudPage from '@/components/CrudPage.vue'
import UserPage from '@/views/system/user/index.vue'
import RolePage from '@/views/system/role/index.vue'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const route = useRoute()
const canEngineer = computed(() => store.hasPerm('staff:engineer:list'))
const canSkill = computed(() => store.hasPerm('staff:skill:list'))
const canCertificate = computed(() => store.hasPerm('staff:certificate:list'))
const canUser = computed(() => store.hasPerm('system:user:list'))
const canRole = computed(() => store.hasPerm('system:role:list'))
function pickTab(tab) {
  if (tab === 'skill' && canSkill.value) return 'skill'
  if (tab === 'certificate' && canCertificate.value) return 'certificate'
  if (tab === 'user' && canUser.value) return 'user'
  if (tab === 'role' && canRole.value) return 'role'
  if (canEngineer.value) return 'engineer'
  if (canSkill.value) return 'skill'
  if (canCertificate.value) return 'certificate'
  if (canUser.value) return 'user'
  return 'role'
}
const active = ref(pickTab(route.query.tab))
watch(() => route.query.tab, (tab) => { active.value = pickTab(tab) })
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
