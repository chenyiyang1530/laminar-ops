<template>
  <el-link v-if="href" :href="href" target="_blank" type="primary">{{ text }}</el-link>
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
  const path = normalizePath(value)
  if (!path) return
  try {
    const res = await request.get('/common/file/sign', { params: { path } })
    href.value = res.data.url
  } catch {
    href.value = ''
  }
}, { immediate: true })
</script>

<style scoped>
.secure-file-pending { color: #909399; font-size: 12px; }
</style>
