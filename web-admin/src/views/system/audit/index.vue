<template>
  <div class="app-container merged-page">
    <div class="merge-tabs">
      <el-tabs v-model="active">
        <el-tab-pane label="登录日志" name="login" />
        <el-tab-pane label="操作日志" name="oper" />
      </el-tabs>
    </div>
    <LoginLog v-if="active === 'login'" />
    <OperLog v-else />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import LoginLog from '@/views/system/loginlog/index.vue'
import OperLog from '@/views/system/operlog/index.vue'

const active = ref('login')
</script>

<style scoped>
.merged-page :deep(.app-container) { padding: 0; }
.merge-tabs { margin-bottom: 10px; padding: 0 4px; }
</style>
