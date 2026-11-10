<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane v-if="canUser" label="用户" name="user" />
        <el-tab-pane v-if="canRole" label="角色" name="role" />
      </el-tabs>
    </div>
    <UserPage v-if="active === 'user'" />
    <RolePage v-else-if="active === 'role'" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import UserPage from '@/views/system/user/index.vue'
import RolePage from '@/views/system/role/index.vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const store = useUserStore()
const canUser = computed(() => store.hasPerm('system:user:list'))
const canRole = computed(() => store.hasPerm('system:role:list'))
const active = ref(route.query.tab === 'role' && canRole.value ? 'role' : canUser.value ? 'user' : 'role')
watch(() => route.query.tab, (tab) => {
  if (tab === 'role' && canRole.value) active.value = 'role'
})
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
