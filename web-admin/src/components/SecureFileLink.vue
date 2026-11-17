<template>
  <el-link v-if="href" :href="href" target="_blank" type="primary">{{ text }}</el-link>
  <el-tag v-else-if="missing" type="danger" size="small" effect="plain">文件丢失</el-tag>
  <span v-else-if="unavailable" class="secure-file-error">文件不可用</span>
  <span v-else-if="path" class="secure-file-pending">生成链接...</span>
  <span v-else>{{ emptyText }}</span>
</template>

<script setup>
import { ref, watch } from 'vue'
import request from '@/utils/request'

const props = defineProps({
  path: { type: String, default: '' },
  text: { type: String, default: '查看文件' },
  emptyText: { type: String, default: '—' }
})
const href = ref('')
const missing = ref(false)
const unavailable = ref(false)

function normalizePath(value) {
  let raw = String(value || '').trim()
  if (!raw) return ''
  if (raw.startsWith('/upload/')) return raw.slice('/upload/'.length)
  if (raw.startsWith('/api/common/file')) {
    try {
      const url = new URL(raw, window.location.origin)
      return url.searchParams.get('path') || ''
    } catch {
      return ''
    }
  }
  return raw.replace(/^\/+/, '')
}

watch(() => props.path, async (value) => {
  href.value = ''
  missing.value = false
  unavailable.value = false
  const path = normalizePath(value)
  if (!path) return
  try {
    const res = await request.get('/common/file/sign', { params: { path } })
    if (res.data?.missing) {
      missing.value = true
      return
    }
    href.value = res.data?.url || ''
    if (!href.value) missing.value = true
  } catch (e) {
    if (e.response?.status === 404) missing.value = true
    else unavailable.value = true
  }
}, { immediate: true })
</script>

<style scoped>
.secure-file-pending { color: #909399; font-size: 12px; }
.secure-file-error { color: #e6a23c; font-size: 12px; }
</style>
