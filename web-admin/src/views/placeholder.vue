<template>
  <div class="app-container">
    <div class="page-card placeholder">
      <el-icon class="icon"><Tools /></el-icon>
      <h2>{{ title }}</h2>
      <p>该模块正在建设中，后续版本开放。</p>
      <el-button type="primary" plain @click="$router.push('/dashboard')">返回工作台</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => route.meta?.title || '功能模块')
</script>

<style scoped>
.placeholder { min-height: 62vh; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; }
.placeholder .icon { font-size: 44px; color: #b9c8da; }
.placeholder h2 { margin: 4px 0 0; }
.placeholder p { color: #8494a8; margin: 0 0 12px; }
</style>
