<template>
  <div class="app-container">
    <div class="page-card">
      <slot name="intro" />
      <el-form v-if="searchFields.length" :model="query" inline class="search-form">
        <el-form-item v-for="f in searchFields" :key="f.name" :label="f.label">
          <el-select
            v-if="f.type === 'select' || f.type === 'dict' || f.type === 'autocomplete'"
            v-model="query[f.name]"
            clearable
            filterable
            :allow-create="!!f.allowCreate"
            default-first-option
            :disabled="isDisabled(f, query)"
            :placeholder="placeholderFor(f, query, '全部')"
            style="width: 158px"
            @change="onSelectChange(f, 'query')"
          >
            <el-option
              v-for="o in selectOptions(f, query)"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
          <el-input
            v-else
            v-model="query[f.name]"
            clearable
            :placeholder="'请输入' + f.label"
            style="width: 180px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <div class="toolbar-left">
          <el-button v-if="can('add') && !hideAdd" type="primary" :icon="Plus" @click="openAdd">新增{{ meta.title }}</el-button>
          <el-button v-if="can('remove') && !hideRemove" :disabled="!selection.length" :icon="Delete" @click="removeSelected">批量删除</el-button>
        </div>
        <div class="toolbar-right">
          <el-popover trigger="click" placement="bottom-end" :width="220">
            <template #reference>
              <el-button link :icon="Setting">列设置</el-button>
            </template>
            <el-checkbox-group v-model="visibleColumnNames" class="column-options">
              <el-checkbox v-for="f in listFields" :key="f.name" :label="f.name">{{ f.label }}</el-checkbox>
            </el-checkbox-group>
            <div class="column-actions">
              <el-button link @click="resetColumns">恢复默认</el-button>
            </div>
          </el-popover>
          <el-button link :icon="Download" @click="exportCsv">导出</el-button>
          <span class="total-text">共 {{ total }} 条</span>
        </div>
      </div>

      <el-table v-loading="loading" :data="records" stripe @selection-change="(v) => (selection = v)">
        <el-table-column type="selection" width="46" />
        <el-table-column type="index" label="序号" width="62" align="center" :index="rowIndex" />
        <el-table-column
          v-for="f in visibleListFields"
          :key="f.name"
          :label="f.label"
          :min-width="f.width || 120"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-tag v-if="f.dictType" size="small" :type="tagType(f, row[f.name])" effect="plain">
              {{ dictLabel(f.dictType, row[f.name]) }}
            </el-tag>
            <span v-else-if="f.options">{{ optionLabel(f.options, row[f.name]) }}</span>
            <span v-else-if="f.type === 'date'">{{ formatDate(row[f.name]) }}</span>
            <span v-else-if="f.type === 'datetime'">{{ row[f.name] || '—' }}</span>
            <span v-else>{{ display(row[f.name]) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" :width="detail || $slots['row-actions'] ? 215 : 130" fixed="right">
          <template #default="{ row }">
            <slot name="row-actions" :row="row" />
            <el-button v-if="detail" link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="can('edit')" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="can('remove') && !hideRemove" link type="danger" @click="removeOne(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pager"
        @change="load"
      />
    </div>

    <el-dialog v-model="dialog" class="crud-dialog" :title="(form[meta.pk] ? '编辑' : '新增') + meta.title" width="920px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="20">
          <el-col v-for="f in formFields" :key="f.name" :span="f.span || 12">
            <el-form-item :label="f.label" :prop="f.name">
              <el-autocomplete
                v-if="f.type === 'autocomplete'"
                v-model="form[f.name]"
                :fetch-suggestions="(q, cb) => autocompleteSuggestions(f, form, q, cb)"
                :placeholder="placeholderFor(f, form, '可选择或输入' + f.label)"
                clearable
                style="width: 100%"
              />
              <el-select
                v-else-if="f.type === 'select' || f.type === 'dict'"
                v-model="form[f.name]"
                clearable
                filterable
                :allow-create="!!f.allowCreate"
                default-first-option
                :disabled="isDisabled(f, form)"
                :placeholder="placeholderFor(f, form, '请选择' + f.label)"
                style="width: 100%"
                @change="onSelectChange(f, 'form')"
              >
                <el-option v-for="o in selectOptions(f, form)" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
              <el-date-picker
                v-else-if="f.type === 'date'"
                v-model="form[f.name]"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
              <el-date-picker
                v-else-if="f.type === 'datetime'"
                v-model="form[f.name]"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
              <el-input-number
                v-else-if="f.type === 'number'"
                v-model="form[f.name]"
                :controls="false"
                style="width: 100%"
              />
              <el-input
                v-else-if="f.type === 'address'"
                v-model="form[f.name]"
                clearable
                :placeholder="f.placeholder || '请输入地址，或点击右侧地图选址'"
              >
                <template #append>
                  <el-button :icon="Location" @click="openAddress(f)">地图选址</el-button>
                </template>
              </el-input>
              <el-input
                v-else-if="f.type === 'textarea'"
                v-model="form[f.name]"
                type="textarea"
                :rows="3"
              />
              <el-input v-else v-model="form[f.name]" clearable :placeholder="f.placeholder || ''" @input="onCodeInput(f, $event)" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" size="820px" :title="detailTitle" destroy-on-close>
      <div v-loading="detailLoading" class="detail-body">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item
            v-for="f in detailFields"
            :key="f.name"
            :label="f.label"
            :span="['address', 'remark', 'description'].includes(f.name) ? 2 : 1"
          >
            <span v-if="f.dictType">{{ dictLabel(f.dictType, detailRow[f.name]) }}</span>
            <span v-else-if="f.options">{{ optionLabel(f.options, detailRow[f.name]) }}</span>
            <span v-else-if="f.type === 'date'">{{ formatDate(detailRow[f.name]) }}</span>
            <span v-else>{{ display(detailRow[f.name]) }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-tabs v-if="detailTabs.length" v-model="detailTab" class="detail-tabs">
          <el-tab-pane v-for="t in detailTabs" :key="t.key" :label="tabLabel(t)" :name="t.key">
            <el-table :data="detailRelated[t.key] || []" size="small" stripe max-height="320">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column
                v-for="c in t.columns"
                :key="c.prop"
                :prop="c.prop"
                :label="c.label"
                :width="c.width"
                :min-width="c.width ? undefined : 120"
                show-overflow-tooltip
              />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <AmapPicker ref="amapPicker" @select="onAddressSelect" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete, Location, Setting, Download } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import AmapPicker from '@/components/AmapPicker.vue'
import { crudMeta, crudPage, crudGet, crudOptions, crudAdd, crudUpdate, crudDelete } from '@/api/crud'

const props = defineProps({
  resource: { type: String, required: true },
  detail: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
  hideAdd: { type: Boolean, default: false },
  hideRemove: { type: Boolean, default: false },
  formDefaults: { type: Object, default: () => ({}) },
  hiddenFields: { type: Array, default: () => [] },
  detailTabs: { type: Array, default: () => [] }
})
const store = useUserStore()
const effectiveHiddenFields = computed(() => {
  const hidden = new Set(props.hiddenFields || [])
  if (store.companyId) hidden.add('companyId')
  return [...hidden]
})
const effectiveFormDefaults = computed(() => ({
  ...(props.formDefaults || {}),
  ...(store.companyId ? { companyId: store.companyId } : {})
}))
const route = useRoute()
const amapPicker = ref()
const addressField = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRow = ref({})
const detailTab = ref('')
const detailRelated = reactive({})
const meta = ref({ fields: [], title: '', pk: '', permPrefix: '' })
const records = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const selection = ref([])
const dialog = ref(false)
const formRef = ref()
const form = reactive({})
const query = reactive({ pageNum: 1, pageSize: store.pageSize || 10 })
const dicts = reactive({})
const options = reactive({})
const optionCache = reactive({})
const hiddenColumns = ref(readHiddenColumns())
const columnStorageKey = computed(() => `laminar_columns_${props.resource}`)
const visibleColumnNames = computed({
  get: () => listFields.value.filter((f) => !hiddenColumns.value.includes(f.name)).map((f) => f.name),
  set: (names) => {
    hiddenColumns.value = listFields.value.filter((f) => !names.includes(f.name)).map((f) => f.name)
    try { localStorage.setItem(columnStorageKey.value, JSON.stringify(hiddenColumns.value)) } catch (e) { /* ignore */ }
  }
})
const visibleListFields = computed(() => listFields.value.filter((f) => !hiddenColumns.value.includes(f.name)))

function readHiddenColumns() {
  try { return JSON.parse(localStorage.getItem(`laminar_columns_${props.resource}`) || '[]') } catch (e) { return [] }
}

function resetColumns() {
  hiddenColumns.value = []
  try { localStorage.removeItem(columnStorageKey.value) } catch (e) { /* ignore */ }
}

async function exportCsv() {
  if (!visibleListFields.value.length) {
    ElMessage.warning('没有可导出的列')
    return
  }
  loading.value = true
  try {
    const res = await request.get(`/crud/${props.resource}/page`, {
      params: { ...query, pageNum: 1, pageSize: 5000 }
    })
    const rows = res.data?.records || []
    if (!rows.length) {
      ElMessage.warning('当前筛选条件下没有数据')
      return
    }
    const cols = visibleListFields.value
    const cell = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`
    const valueOf = (f, row) => {
      const v = row[f.name]
      if (v === null || v === undefined || v === '') return ''
      if (f.dictType) return dictLabel(f.dictType, v)
      if (f.options) return optionLabel(f.options, v)
      if (f.type === 'date') return formatDate(v)
      return v
    }
    const lines = [cols.map((f) => cell(f.label)).join(',')]
    rows.forEach((row) => lines.push(cols.map((f) => cell(valueOf(f, row))).join(',')))
    const blob = new Blob(['\uFEFF' + lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    const date = new Date().toISOString().slice(0, 10).replace(/-/g, '')
    a.href = url
    a.download = `${meta.value.title || props.resource}-${date}.csv`
    document.body.appendChild(a)
    a.click()
    a.remove()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
    ElMessage.success(`已导出 ${rows.length} 条`)
  } finally {
    loading.value = false
  }
}

const DEPENDS = {
  department: { campusId: 'hospitalId' },
  cleanArea: { campusId: 'hospitalId', departmentId: 'campusId' },
  contact: { campusId: 'hospitalId', departmentId: 'campusId' },
  equipment: { campusId: 'hospitalId', departmentId: 'campusId', areaId: 'campusId' },
  inspectionPlan: { campusId: 'hospitalId', areaId: 'campusId' },
  maintenancePlan: { equipmentId: 'hospitalId' },
  monitorPoint: {}
}

const inlineOptions = {
  yesNo: [{ value: '1', label: '是' }, { value: '0', label: '否' }],
  enableStatus: [{ value: '0', label: '启用' }, { value: '1', label: '停用' }],
  contractStatus: [
    { value: '0', label: '执行中' },
    { value: '1', label: '已终止' },
    { value: '2', label: '已过期' },
    { value: '3', label: '未开始' }
  ],
  cycleType: [
    { value: 'day', label: '按天' },
    { value: 'week', label: '按周' },
    { value: 'month', label: '按月' },
    { value: 'quarter', label: '按季' },
    { value: 'year', label: '按年' }
  ]
}

const fields = computed(() => meta.value.fields || [])
const listFields = computed(() => fields.value.filter((f) => f.list))
const formFields = computed(() => fields.value.filter((f) => f.form && !effectiveHiddenFields.value.includes(f.name)))
const searchFields = computed(() => fields.value.filter((f) => f.query && f.query !== 'none' && !effectiveHiddenFields.value.includes(f.name)).slice(0, 5))
const detailFields = computed(() => fields.value.filter((f) => f.list || f.form))
const detailTitle = computed(() => {
  const name = detailRow.value?.[meta.value.nameColumn]
  return name || (meta.value.title + '详情')
})

const rules = computed(() => {
  const r = {}
  formFields.value.forEach((f) => {
    if (f.required) r[f.name] = [{ required: true, message: '请填写' + f.label, trigger: 'blur' }]
  })
  return r
})

function can(action) {
  if (props.readonly && ['add', 'edit', 'remove'].includes(action)) return false
  return store.hasPerm(`${meta.value.permPrefix}:${action}`)
}

function rowIndex(i) {
  return (query.pageNum - 1) * query.pageSize + i + 1
}

function loadOptions(ref, params) {
  if (ref === 'user') return request.get('/system/user/options')
  return crudOptions(ref, params)
}

function selectOptions(f, model) {
  if (f.dictType) return (dicts[f.dictType] || []).map((d) => ({ value: d.dictValue, label: d.dictLabel }))
  if (inlineOptions[f.options]) return inlineOptions[f.options]
  if (depParent(f) && !parentReady(f, model)) return []
  const params = depParams(f, model)
  const key = f.options + '|' + JSON.stringify(params)
  if (!(key in optionCache)) {
    optionCache[key] = []
    loadOptions(f.options, params)
      .then((res) => { optionCache[key] = res.data })
      .catch(() => { optionCache[key] = [] })
  }
  return optionCache[key]
}

function autocompleteSuggestions(f, model, query, callback) {
  const keyword = String(query || '').trim().toLowerCase()
  const options = selectOptions(f, model) || []
  callback(options
    .filter((o) => !keyword || String(o.label).toLowerCase().includes(keyword) || String(o.value).toLowerCase().includes(keyword))
    .map((o) => ({ value: o.label })))
}

function depParent(f) {
  return (DEPENDS[props.resource] || {})[f.name]
}

function parentReady(f, model) {
  const parent = depParent(f)
  if (!parent) return true
  return !!model && model[parent] !== undefined && model[parent] !== null && model[parent] !== ''
}

function labelOf(name) {
  return fields.value.find((x) => x.name === name)?.label || ''
}

function isDisabled(f, model) {
  return isLocked(f) || (!!depParent(f) && !parentReady(f, model))
}

function placeholderFor(f, model, fallback) {
  const parent = depParent(f)
  if (parent && !parentReady(f, model)) {
    return '请先选择' + labelOf(parent)
  }
  return fallback
}

function depParams(f, model) {
  const params = {}
  const parent = depParent(f)
  if (parent && model && model[parent] !== undefined && model[parent] !== null && model[parent] !== '') {
    params[parent] = model[parent]
  }
  return params
}

function onSelectChange(f, ctx) {
  const model = ctx === 'query' ? query : form
  clearDescendants(f.name, model)
}

function clearDescendants(parentName, model) {
  const deps = DEPENDS[props.resource] || {}
  Object.keys(deps).forEach((child) => {
    if (deps[child] === parentName) {
      model[child] = undefined
      clearDescendants(child, model)
    }
  })
}

function isLocked(f) {
  return f.name === 'hospitalId' && !!store.hospitalId
}

function dictLabel(type, v) {
  return (dicts[type] || []).find((d) => String(d.dictValue) === String(v))?.dictLabel || v || '—'
}

function optionLabel(key, v) {
  if (v === null || v === undefined || v === '') return '—'
  const list = inlineOptions[key] || options[key] || []
  return list.find((o) => String(o.value) === String(v))?.label ?? v
}

function tagType(f, v) {
  if (['status', 'monitorStatus', 'runStatus', 'certStatus'].includes(f.name)) {
    if (['0', '1'].includes(String(v)) && f.name === 'status') return v === '0' ? 'success' : 'info'
    return { 0: 'success', 1: 'warning', 2: 'danger', 3: 'info' }[String(v)] || 'info'
  }
  return 'info'
}

function display(v) {
  return v === null || v === undefined || v === '' ? '—' : v
}
function formatDate(v) {
  return v ? String(v).slice(0, 10) : '—'
}

async function loadMeta() {
  const res = await crudMeta(props.resource)
  meta.value = res.data
}

async function loadBase() {
  const dictTypes = [...new Set(fields.value.filter((f) => f.dictType).map((f) => f.dictType))]
  await Promise.all(
    dictTypes.map(async (t) => {
      const res = await request.get('/system/dict/type/' + t)
      dicts[t] = res.data
    })
  )
  const refs = [...new Set(fields.value.filter((f) => f.options && !inlineOptions[f.options]).map((f) => f.options))]
  await Promise.all(
    refs.map(async (r) => {
      try {
        const res = await loadOptions(r)
        options[r] = res.data
      } catch (e) {
        options[r] = []
      }
    })
  )
}

async function load() {
  loading.value = true
  try {
    const res = await crudPage(props.resource, query)
    records.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function reset() {
  Object.keys(query).forEach((k) => {
    if (!['pageNum', 'pageSize'].includes(k)) query[k] = undefined
  })
  query.pageNum = 1
  if (store.hospitalId) query.hospitalId = store.hospitalId
  load()
}

function resetForm() {
  Object.keys(form).forEach((k) => delete form[k])
  formFields.value.forEach((f) => {
    if (f.defaultValue !== undefined && f.defaultValue !== null) form[f.name] = f.defaultValue
    else if (f.type === 'number') form[f.name] = undefined
  })
  if (store.hospitalId) form.hospitalId = store.hospitalId
}

function tabLabel(t) {
  const list = detailRelated[t.key]
  return Array.isArray(list) ? `${t.label}（${list.length}）` : t.label
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detailRow.value = row
  detailTab.value = props.detailTabs[0]?.key || ''
  Object.keys(detailRelated).forEach((k) => delete detailRelated[k])
  try {
    const res = await crudGet(props.resource, row[meta.value.pk])
    detailRow.value = res.data
  } catch (e) {
    // 详情接口失败时仍展示列表行数据
  }
  if (props.detailTabs.length) {
    const id = row[meta.value.pk]
    await Promise.all(
      props.detailTabs.map(async (t) => {
        const params = { pageNum: 1, pageSize: 100 }
        params[t.param || 'id'] = id
        try {
          const res = await crudPage(t.resource, params)
          detailRelated[t.key] = res.data.records || []
        } catch (e) {
          detailRelated[t.key] = []
        }
      })
    )
  }
  detailLoading.value = false
}

function onCodeInput(f, v) {
  if (!f.name.endsWith('Code')) return
  form[f.name] = String(v || '').toUpperCase().replace(/\s+/g, '')
}

function openAddress(f) {
  addressField.value = f.name
  amapPicker.value?.open({ address: form[f.name] })
}

function onAddressSelect(location) {
  if (!addressField.value) return
  form[addressField.value] = location.address
  if (formFields.value.some((f) => f.name === 'longitude')) form.longitude = Number(location.longitude)
  if (formFields.value.some((f) => f.name === 'latitude')) form.latitude = Number(location.latitude)
}

function openAdd() {
  resetForm()
  applyFormDefaults()
  dialog.value = true
}

function applyFormDefaults() {
  Object.entries(effectiveFormDefaults.value || {}).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '') return
    if (!formFields.value.some((f) => f.name === key)) return
    if (form[key] === null || form[key] === undefined || form[key] === '') form[key] = value
  })
}

async function openEdit(row) {
  resetForm()
  const res = await crudGet(props.resource, row[meta.value.pk])
  Object.assign(form, res.data)
  applyFormDefaults()
  if (store.hospitalId) form.hospitalId = store.hospitalId
  dialog.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form[meta.value.pk]) await crudUpdate(props.resource, form)
    else await crudAdd(props.resource, form)
    ElMessage.success('保存成功')
    dialog.value = false
    await Promise.all([load(), refreshOptions()])
  } finally {
    saving.value = false
  }
}

async function removeOne(row) {
  await ElMessageBox.confirm(`确认删除该${meta.value.title}？`, '提示', { type: 'warning' })
  await crudDelete(props.resource, row[meta.value.pk])
  ElMessage.success('已删除')
  await Promise.all([load(), refreshOptions()])
}

async function refreshOptions() {
  Object.keys(optionCache).forEach((k) => delete optionCache[k])
  await loadBase()
}

async function removeSelected() {
  await ElMessageBox.confirm(`确认删除选中的 ${selection.value.length} 条数据？`, '提示', { type: 'warning' })
  await crudDelete(props.resource, selection.value.map((r) => r[meta.value.pk]).join(','))
  ElMessage.success('已删除')
  await Promise.all([load(), refreshOptions()])
}

function applyRouteQuery(params) {
  if (!params) return false
  const known = new Set(fields.value.filter((f) => f.query && f.query !== 'none').map((f) => f.name))
  const idFields = /^(hospitalId|campusId|departmentId|areaId|equipmentId|engineerId|deviceTypeId)$/
  if (params.hospitalId) {
    fields.value
      .filter((f) => f.query && f.query !== 'none' && f.name !== 'hospitalId')
      .forEach((f) => { query[f.name] = undefined })
  }
  let changed = false
  Object.entries(params).forEach(([k, v]) => {
    if (!known.has(k) || v === undefined || v === null || v === '') return
    query[k] = idFields.test(k) && /^\d+$/.test(String(v)) ? Number(v) : v
    changed = true
  })
  if (store.hospitalId) query.hospitalId = store.hospitalId
  return changed
}

onMounted(async () => {
  await loadMeta()
  await loadBase()
  applyRouteQuery(route.query)
  load()
})

watch(
  () => route.query,
  (q) => {
    if (applyRouteQuery(q)) {
      query.pageNum = 1
      load()
    }
  },
  { deep: true }
)
</script>

<style scoped>
.search-form { margin-bottom: 4px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin: 6px 0 12px; }
.toolbar-right { display: flex; align-items: center; gap: 10px; color: #8494a8; font-size: 12px; }
.total-text { color: #8494a8; font-size: 12px; }
.column-options { display: flex; flex-direction: column; gap: 2px; max-height: 300px; overflow-y: auto; }
.column-actions { margin-top: 8px; padding-top: 8px; border-top: 1px solid #eef2f7; }
.pager { margin-top: 14px; justify-content: flex-end; }
.detail-body { padding: 2px 2px 10px; }
:global(.crud-dialog) { max-width: 94vw; }
.detail-tabs { margin-top: 16px; }
</style>
