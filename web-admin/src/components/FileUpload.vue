<template>
  <div class="file-upload">
    <div v-if="modelValue" class="file-current">
      <SecureFileLink :path="modelValue" :text="fileName" />
      <el-button link type="danger" :disabled="disabled" @click="clear">删除</el-button>
    </div>
    <el-upload
      v-else
      :show-file-list="false"
      :http-request="upload"
      :accept="accept"
      :disabled="disabled"
    >
      <el-button :loading="uploading" :disabled="disabled">选择文件</el-button>
      <span class="tip">图片 / PDF / Word / Excel，≤20MB</span>
    </el-upload>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import SecureFileLink from '@/components/SecureFileLink.vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  category: { type: String, default: 'common' },
  accept: { type: String, default: '.jpg,.jpeg,.png,.webp,.pdf,.doc,.docx,.xls,.xlsx' },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])
const uploading = ref(false)
const fileName = computed(() => {
  const raw = String(props.modelValue || '')
  const name = raw.split('?')[0].split('/').pop()
  return name || '查看文件'
})

async function upload(options) {
  uploading.value = true
  try {
    const data = new FormData()
    data.append('file', options.file)
    const res = await request.post('/common/upload', data, { params: { category: props.category } })
    emit('update:modelValue', res.data.url)
    ElMessage.success('上传成功')
    options.onSuccess?.(res)
  } catch (e) {
    options.onError?.(e)
  } finally {
    uploading.value = false
  }
}

function clear() {
  emit('update:modelValue', '')
}
</script>

<style scoped>
.file-upload { width: 100%; }
.file-current { display: flex; align-items: center; gap: 10px; min-height: 32px; }
:deep(.el-upload) { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.tip { color: #909399; font-size: 12px; white-space: nowrap; }
</style>
