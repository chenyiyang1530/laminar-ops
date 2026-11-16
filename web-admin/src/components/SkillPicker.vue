<template>
  <div class="skill-picker">
    <div class="skill-current">
      <template v-if="selectedOptions.length">
        <el-tag
          v-for="s in visibleSelected"
          :key="s.value"
          closable
          type="warning"
          effect="light"
          @close="remove(s.value)"
        >{{ s.label }}</el-tag>
        <el-tag v-if="extraCount > 0" type="info" effect="light">+{{ extraCount }}</el-tag>
        <span class="selected-count">已选 {{ selectedOptions.length }} 项</span>
      </template>
      <span v-else class="empty">未选择技能标签</span>
      <el-button link type="primary" :disabled="disabled" @click="open">选择技能</el-button>
    </div>

    <el-dialog v-model="visible" class="skill-dialog" title="选择技能标签" width="760px" append-to-body destroy-on-close>
      <div class="skill-toolbar">
        <el-input v-model="keyword" clearable placeholder="搜索技能名称" style="width: 260px" />
        <span class="count">已选 {{ draft.length }} 项</span>
        <el-button link type="danger" @click="draft = []">清空全部</el-button>
      </div>

      <div v-for="group in visibleGroups" :key="group.category" class="skill-group">
        <div class="group-head">
          <b>{{ group.category }}</b>
          <div>
            <el-button link type="primary" @click="selectGroup(group)">全选本类</el-button>
            <el-button link @click="clearGroup(group)">清空本类</el-button>
          </div>
        </div>
        <el-checkbox-group v-model="draft">
          <el-checkbox
            v-for="s in group.options"
            :key="s.value"
            :value="s.value"
            border
            class="skill-check"
          >{{ s.label }}</el-checkbox>
        </el-checkbox-group>
      </div>
      <el-empty v-if="!visibleGroups.length" description="没有匹配的技能标签" />

      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="confirm">确定（{{ draft.length }}）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '@/utils/request'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  disabled: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])
const visible = ref(false)
const keyword = ref('')
const options = ref([])
const draft = ref([])

const selectedOptions = computed(() => {
  const ids = new Set((props.modelValue || []).map(String))
  return options.value.filter((o) => ids.has(String(o.value)))
})
const visibleSelected = computed(() => selectedOptions.value.slice(0, 4))
const extraCount = computed(() => Math.max(0, selectedOptions.value.length - visibleSelected.value.length))

const groups = computed(() => {
  const map = new Map()
  options.value.forEach((o) => {
    const key = o.category || '其他'
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(o)
  })
  return [...map.entries()].map(([category, list]) => ({ category, options: list }))
})

const visibleGroups = computed(() => {
  const kw = keyword.value.trim()
  return groups.value
    .map((g) => ({ ...g, options: g.options.filter((o) => !kw || o.label.includes(kw)) }))
    .filter((g) => g.options.length)
})

async function load() {
  try {
    const res = await request.get('/crud/skill/options')
    options.value = res.data || []
  } catch (e) {
    options.value = []
  }
}

function open() {
  draft.value = [...(props.modelValue || [])]
  keyword.value = ''
  visible.value = true
}

function selectGroup(group) {
  const ids = new Set(draft.value.map(String))
  group.options.forEach((o) => ids.add(String(o.value)))
  draft.value = options.value.filter((o) => ids.has(String(o.value))).map((o) => o.value)
}

function clearGroup(group) {
  const ids = new Set(group.options.map((o) => String(o.value)))
  draft.value = draft.value.filter((id) => !ids.has(String(id)))
}

function remove(value) {
  emit('update:modelValue', (props.modelValue || []).filter((id) => String(id) !== String(value)))
}

function confirm() {
  emit('update:modelValue', [...draft.value])
  visible.value = false
}

onMounted(load)
</script>

<style scoped>
.skill-picker { width: 100%; }
.skill-current { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; min-height: 34px; max-height: 82px; overflow-y: auto; }
.skill-current .el-tag { margin: 0; }
.selected-count { color: #8a99ad; font-size: 12px; }
.empty { color: #a0a9b8; font-size: 13px; }
.skill-toolbar { position: sticky; top: 0; z-index: 2; display: flex; align-items: center; gap: 14px; margin-bottom: 10px; padding: 6px 0 12px; background: #fff; }
.skill-toolbar .count { color: #6b7a90; font-size: 13px; }
.skill-group { padding: 14px 0 6px; border-top: 1px solid #eef2f7; }
.skill-group:first-of-type { border-top: 0; padding-top: 0; }
.group-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; color: #1f2d3d; }
.skill-check { margin: 0 8px 8px 0; }
</style>
