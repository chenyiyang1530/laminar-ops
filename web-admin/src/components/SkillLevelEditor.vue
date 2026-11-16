<template>
  <div class="skill-level-editor">
    <div v-if="rows.length" class="skill-list">
      <div v-for="row in rows" :key="row.skillId" class="skill-item">
        <div class="skill-main">
          <div class="skill-name">
            {{ skillName(row) }}
            <span v-if="skillCategory(row)" class="skill-category">{{ skillCategory(row) }}</span>
          </div>
          <div class="skill-hint">{{ levelHint(row.level) }}</div>
        </div>
        <el-radio-group
          v-model="row.level"
          size="small"
          class="level-group"
          :disabled="disabled"
          @change="sync"
        >
          <el-radio-button v-for="level in levels" :key="level.value" :value="level.value">
            {{ level.short }}
          </el-radio-button>
        </el-radio-group>
        <el-button
          v-if="!disabled"
          class="remove-btn"
          link
          type="danger"
          :icon="Delete"
          @click="remove(row.skillId)"
        />
      </div>
    </div>
    <el-empty v-else :image-size="48" description="尚未配置技能等级" />

    <div class="skill-add">
      <el-select
        v-model="adding"
        filterable
        clearable
        :disabled="disabled"
        placeholder="搜索技能并添加"
        class="skill-add-select"
        @change="addSelected"
      >
        <el-option-group v-for="group in groupedOptions" :key="group.category" :label="group.category">
          <el-option
            v-for="option in group.options"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-option-group>
      </el-select>
      <span class="skill-tip">已有 {{ rows.length }} 项 · 等级由左到右递增</span>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const levels = [
  { value: '1', short: 'L1', label: '了解', hint: 'L1 了解：在指导下参与' },
  { value: '2', short: 'L2', label: '辅助', hint: 'L2 辅助：可配合完成' },
  { value: '3', short: 'L3', label: '独立', hint: 'L3 独立：可独立完成' },
  { value: '4', short: 'L4', label: '熟练', hint: 'L4 熟练：复杂问题可处理' },
  { value: '5', short: 'L5', label: '带教', hint: 'L5 带教：可带教与审核' }
]
const options = ref([])
const rows = ref([])
const adding = ref()

function normalize(value) {
  return (Array.isArray(value) ? value : []).map((item) => ({
    skillId: item.skillId ?? item.skill_id,
    skillName: item.skillName ?? item.skill_name,
    level: String(item.level ?? '3')
  }))
}

rows.value = normalize(props.modelValue)

const groupedOptions = computed(() => {
  const selected = new Set(rows.value.map((row) => String(row.skillId)))
  const groups = new Map()
  options.value
    .filter((option) => !selected.has(String(option.value)))
    .forEach((option) => {
      const category = option.category || '其他'
      if (!groups.has(category)) groups.set(category, [])
      groups.get(category).push(option)
    })
  return [...groups.entries()].map(([category, options]) => ({ category, options }))
})

function optionOf(row) {
  return options.value.find((option) => String(option.value) === String(row.skillId))
}

function skillName(row) {
  return optionOf(row)?.label || row.skillName || '未命名技能'
}

function skillCategory(row) {
  return optionOf(row)?.category || ''
}

function levelHint(value) {
  const level = levels.find((item) => item.value === String(value))
  return level?.hint || ''
}

function sync() {
  emit('update:modelValue', rows.value.map((row) => ({ ...row })))
}

function addSelected(skillId) {
  if (!skillId) return
  if (rows.value.some((row) => String(row.skillId) === String(skillId))) {
    adding.value = undefined
    return
  }
  const option = options.value.find((item) => String(item.value) === String(skillId))
  rows.value.push({
    skillId,
    skillName: option?.label || '',
    level: '3'
  })
  adding.value = undefined
  sync()
}

function remove(skillId) {
  rows.value = rows.value.filter((row) => String(row.skillId) !== String(skillId))
  sync()
}

onMounted(async () => {
  try {
    const res = await request.get('/crud/skill/options')
    options.value = res.data || []
    rows.value = normalize(props.modelValue)
  } catch (e) {
    options.value = []
  }
})
</script>

<style scoped>
.skill-level-editor {
  width: 100%;
  border: 1px solid #e8edf4;
  border-radius: 10px;
  padding: 8px 12px;
  background: #fbfdff;
}
.skill-list { display: flex; flex-direction: column; }
.skill-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 9px 2px;
  border-bottom: 1px solid #eef2f7;
}
.skill-item:last-child { border-bottom: none; }
.skill-main { flex: 1; min-width: 0; }
.skill-name {
  font-size: 13px;
  font-weight: 500;
  color: #26364a;
  display: flex;
  align-items: center;
  gap: 8px;
}
.skill-category {
  font-size: 10px;
  color: #5b6b7d;
  background: #eef3f9;
  border-radius: 3px;
  padding: 1px 6px;
  font-weight: 400;
}
.skill-hint { font-size: 11px; color: #93a1b5; margin-top: 2px; }
.level-group { flex: 0 0 auto; }
.level-group :deep(.el-radio-button__inner) {
  min-width: 34px;
  padding: 5px 6px;
  font-size: 11px;
}
.remove-btn { flex: 0 0 auto; }
.skill-add {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 10px;
  margin-top: 4px;
  border-top: 1px dashed #e6ebf2;
}
.skill-add-select { width: 260px; }
.skill-tip { font-size: 11px; color: #93a1b5; }
:deep(.el-empty) { padding: 12px 0; }
</style>
