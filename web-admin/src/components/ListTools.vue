<template>
  <div class="list-tools">
    <el-popover trigger="click" placement="bottom-end" :width="220">
      <template #reference>
        <el-button link :icon="Setting">列设置</el-button>
      </template>
      <el-checkbox-group :model-value="modelValue" class="column-options" @update:model-value="onChange">
        <el-checkbox v-for="c in columns" :key="c.key" :label="c.key">{{ c.label }}</el-checkbox>
      </el-checkbox-group>
      <div class="column-actions">
        <el-button link @click="reset">恢复默认</el-button>
      </div>
    </el-popover>
    <el-button link :icon="Download" :loading="exporting" @click="$emit('export')">导出</el-button>
  </div>
</template>

<script setup>
import { Setting, Download } from '@element-plus/icons-vue'

defineProps({
  modelValue: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  defaultKeys: { type: Array, default: () => [] },
  exporting: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'export', 'reset'])

function onChange(value) {
  emit('update:modelValue', value)
}

function reset() {
  emit('reset')
}
</script>

<style scoped>
.list-tools { display: inline-flex; align-items: center; gap: 8px; }
.column-options { display: flex; flex-direction: column; gap: 2px; max-height: 320px; overflow-y: auto; }
.column-actions { margin-top: 8px; padding-top: 8px; border-top: 1px solid #eef2f7; }
</style>
