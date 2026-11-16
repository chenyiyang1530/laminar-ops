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
          :min-width="f.type === 'switch' ? Math.max(f.width || 120, 130) : (f.width || 120)"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-tag v-if="f.dictType" size="small" :type="tagType(f, row[f.name])" effect="plain">
              {{ dictLabel(f.dictType, row[f.name]) }}
            </el-tag>
            <span v-else-if="f.options">{{ optionLabel(f.options, row[f.name]) }}</span>
            <span v-else-if="f.type === 'date'">{{ formatDate(row[f.name]) }}</span>
            <span v-else-if="f.type === 'datetime'">{{ row[f.name] || '—' }}</span>
            <el-tag v-else-if="f.name === 'onDuty' && row.leaveStatus" type="warning" effect="light" size="small">请假中</el-tag>
            <span v-else-if="f.type === 'switch'" class="switch-cell">
              <span class="switch-label" :class="{ active: !isSwitchOn(f, row) }">{{ f.switchOffText || '关闭' }}</span>
              <el-switch
                :model-value="isSwitchOn(f, row)"
                :disabled="!can('edit')"
                size="small"
                @click.stop="toggleTableSwitch(row, f, !isSwitchOn(f, row))"
              />
              <span class="switch-label" :class="{ active: isSwitchOn(f, row) }">{{ f.switchOnText || '开启' }}</span>
            </span>
            <div v-else-if="f.name === 'skillNames' && row[f.name]" class="skill-tags">
              <el-tooltip :content="row[f.name]" placement="top">
                <span class="skill-tag-box">
                  <el-tag v-for="tag in skillTags(row[f.name]).slice(0, 1)" :key="tag" size="small" effect="plain" class="skill-tag">{{ tag }}</el-tag>
                  <el-tag v-if="skillTags(row[f.name]).length > 1" size="small" type="info" effect="plain">+{{ skillTags(row[f.name]).length - 1 }}</el-tag>
                </span>
              </el-tooltip>
            </div>
            <el-tag v-else-if="f.tag && row[f.name]" type="warning" effect="light" size="small">{{ row[f.name] }}</el-tag>
            <SecureFileLink v-else-if="f.type === 'file' && row[f.name]" :path="row[f.name]" text="查看文件" />
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

    <el-dialog v-model="dialog" class="crud-dialog" width="780px" destroy-on-close>
      <template #header>
        <div class="crud-dialog-head">
          <div class="crud-dialog-title">{{ form[meta.pk] ? '编辑' : '新增' }}{{ meta.title }}</div>
          <div class="crud-dialog-sub">带 * 为必填项，保存前会自动校验格式</div>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="crud-form">
        <el-row :gutter="20">
          <el-col v-for="f in formFields" :key="f.name" :span="f.formSpan || 12">
            <el-form-item :label="f.label" :prop="f.name" :class="{ 'field-label-top': f.formLabelTop, 'field-compact': f.formWidth, 'field-wide': f.formSpan === 24 || f.type === 'skills' }" :style="f.formWidth ? { '--field-width': f.formWidth + 'px' } : null">
              <SkillPicker v-if="f.name === 'skillIds' || f.type === 'skillPicker'" v-model="form[f.name]" />
              <el-autocomplete
                v-else-if="f.type === 'autocomplete'"
                v-model="form[f.name]"
                :fetch-suggestions="(q, cb) => autocompleteSuggestions(f, form, q, cb)"
                :placeholder="placeholderFor(f, form, '可选择或输入' + f.label)"
                :disabled="!!f.readOnly"
                :maxlength="f.maxLength || undefined"
                clearable
                style="width: 100%"
              />
              <el-select
                v-else-if="f.type === 'select' || f.type === 'dict'"
                v-model="form[f.name]"
                clearable
                filterable
                :allow-create="!!f.allowCreate"
                :multiple="!!f.multiple"
                collapse-tags
                default-first-option
                :disabled="!!f.readOnly || isDisabled(f, form) || (f.name === 'contactId' && !form[meta.value.pk]) || (props.resource === 'contract' && f.name === 'managerId' && !form.hospitalId)"
                :placeholder="placeholderFor(f, form, '请选择' + f.label)"
                style="width: 100%"
                @change="onSelectChange(f, 'form')"
              >
                <template v-if="groupedOptions(f, form).length">
                  <el-option-group v-for="g in groupedOptions(f, form)" :key="g.category" :label="g.category">
                    <el-option v-for="o in g.options" :key="o.value" :label="o.label" :value="o.value" />
                  </el-option-group>
                </template>
                <el-option v-else v-for="o in selectOptions(f, form)" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
              <el-date-picker
                v-else-if="f.type === 'date'"
                v-model="form[f.name]"
                type="date"
                value-format="YYYY-MM-DD"
                :disabled="!!f.readOnly"
                style="width: 100%"
              />
              <el-date-picker
                v-else-if="f.type === 'datetime'"
                v-model="form[f.name]"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                :disabled="!!f.readOnly"
                style="width: 100%"
              />
              <el-input-number
                v-else-if="f.type === 'number'"
                v-model="form[f.name]"
                :controls="false"
                :disabled="!!f.readOnly"
                :min="f.minValue ?? undefined"
                :max="f.maxValue ?? undefined"
                style="width: 100%"
              />
              <el-input
                v-else-if="f.type === 'address'"
                v-model="form[f.name]"
                clearable
                :disabled="!!f.readOnly"
                :maxlength="f.maxLength || undefined"
                :placeholder="f.placeholder || '请输入地址，或点击右侧地图选址'"
              >
                <template #append>
                  <el-button :icon="Location" :disabled="!!f.readOnly" @click="openAddress(f)">地图选址</el-button>
                </template>
              </el-input>
              <el-switch
                v-else-if="f.type === 'switch'"
                v-model="form[f.name]"
                :active-value="f.switchOnValue || '1'"
                :inactive-value="f.switchOffValue || '0'"
                :active-text="f.switchOnText || '开启'"
                :inactive-text="f.switchOffText || '关闭'"
              />
              <el-input
                v-else-if="f.type === 'textarea'"
                v-model="form[f.name]"
                type="textarea"
                :disabled="!!f.readOnly"
                :maxlength="f.maxLength || undefined"
                :rows="4"
                :placeholder="f.placeholder || ''"
              />
              <SkillLevelEditor
                v-else-if="f.type === 'skills'"
                v-model="form[f.name]"
                :disabled="!!f.readOnly"
              />
              <FileUpload
                v-else-if="f.type === 'file'"
                v-model="form[f.name]"
                :category="props.resource"
                :disabled="!!f.readOnly"
              />
              <el-input v-else v-model="form[f.name]" clearable :disabled="!!f.readOnly" :maxlength="f.maxLength || undefined" :placeholder="f.placeholder || ''" @input="onCodeInput(f, $event)" />
            </el-form-item>
          </el-col>
          <el-col v-if="props.resource === 'department'" :span="12">
            <el-form-item label="对接人">
              <el-select
                v-model="form.contactId"
                clearable
                filterable
                :disabled="!form.departmentId"
                :placeholder="form.departmentId ? '请选择本科室对接人' : '保存科室后可选择对接人'"
                style="width: 100%"
              >
                <el-option v-for="o in (departmentContactOptions || [])" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="props.resource === 'department'" :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="3" :maxlength="500" />
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
            :span="['address', 'remark', 'description', 'skillNames'].includes(f.name) ? 2 : 1"
          >
            <div v-if="f.name === 'skillNames'" class="detail-skill-tags">
              <el-tag v-for="tag in skillTags(detailRow[f.name])" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
              <span v-if="!skillTags(detailRow[f.name]).length">—</span>
            </div>
            <span v-else-if="f.dictType">{{ dictLabel(f.dictType, detailRow[f.name]) }}</span>
            <span v-else-if="f.options">{{ optionLabel(f.options, detailRow[f.name]) }}</span>
            <span v-else-if="f.type === 'date'">{{ formatDate(detailRow[f.name]) }}</span>
            <el-tag v-else-if="f.tag && detailRow[f.name]" type="warning" effect="light" size="small">{{ detailRow[f.name] }}</el-tag>
            <SecureFileLink v-else-if="f.type === 'file' && detailRow[f.name]" :path="detailRow[f.name]" text="查看文件" />
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
import FileUpload from '@/components/FileUpload.vue'
import SecureFileLink from '@/components/SecureFileLink.vue'
import SkillPicker from '@/components/SkillPicker.vue'
import SkillLevelEditor from '@/components/SkillLevelEditor.vue'
import { crudMeta, crudPage, crudGet, crudOptions, crudAdd, crudUpdate, crudDelete } from '@/api/crud'

const props = defineProps({
  resource: { type: String, required: true },
  detail: { type: Boolean, default: true },
  maxColumns: { type: Number, default: 6 },
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
watch(() => form.operator, (value) => {
  if (props.resource === 'alarmRule' && value && value !== 'between') form.threshold2 = undefined
})
const query = reactive({ pageNum: 1, pageSize: store.pageSize || 10 })
const dicts = reactive({})
const options = reactive({})
const optionCache = reactive({})
const regionManagerOptions = ref(null)
const departmentContactOptions = ref(null)
const contractManagerOptions = ref(null)
const planAssigneeOptions = ref(null)
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
  try { localStorage.removeItem(columnStorageKey.value); } catch (e) { /* ignore */ }
  applyDefaultColumns()
}

function applyDefaultColumns() {
  try {
    if (localStorage.getItem(columnStorageKey.value)) return
  } catch (e) { /* ignore */ }
  const limit = props.maxColumns > 0 ? props.maxColumns : listFields.value.length
  hiddenColumns.value = listFields.value.slice(limit).map((f) => f.name)
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
  contact: { campusId: 'hospitalId', departmentId: 'campusId' },
  equipment: { campusId: 'hospitalId', departmentId: 'campusId' },
  inspectionPlan: { campusId: 'hospitalId', departmentId: 'campusId', assigneeId: 'hospitalId' },
  maintenancePlan: { equipmentId: 'hospitalId', assigneeId: 'hospitalId' },
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
const formFields = computed(() => fields.value.filter((f) => f.form && f.name !== 'contactId'
  && !(props.resource === 'department' && f.name === 'remark')
  && !(props.resource === 'alarmRule' && f.name === 'threshold2' && form.operator !== 'between')
  && !effectiveHiddenFields.value.includes(f.name)))
const searchFields = computed(() => fields.value.filter((f) => f.query && f.query !== 'none' && !effectiveHiddenFields.value.includes(f.name)).slice(0, 5))
const detailFields = computed(() => fields.value.filter((f) => (f.list || f.form) && f.name !== 'skillIds'))
const detailTitle = computed(() => {
  const name = detailRow.value?.[meta.value.nameColumn]
  return name || (meta.value.title + '详情')
})

const rules = computed(() => {
  const r = {}
  formFields.value.forEach((f) => {
    const list = []
    if (f.required) list.push({ required: true, message: '请填写' + f.label, trigger: 'blur' })
    if (f.maxLength) list.push({ max: f.maxLength, message: f.label + '不能超过' + f.maxLength + '个字符', trigger: 'blur' })
    if (f.pattern) {
      list.push({
        validator: (_rule, value, callback) => {
          if (!value || new RegExp(f.pattern).test(String(value))) callback()
          else callback(new Error(f.validationMessage || (f.label + '格式不正确')))
        },
        trigger: 'blur'
      })
    }
    if (list.length) r[f.name] = list
  })
  return r
})

function can(action) {
  if (props.readonly && ['add', 'edit', 'remove'].includes(action)) return false
  return store.hasPerm(`${meta.value.permPrefix}:${action}`)
}

function rowIndex(i) {
  return i + 1
}

function loadOptions(ref, params) {
  if (ref === 'user') return request.get('/system/user/options')
  return crudOptions(ref, params)
}

function groupedOptions(f, model) {
  const list = selectOptions(f, model) || []
  if (!list.some((o) => o.category)) return []
  const groups = new Map()
  list.forEach((o) => {
    const key = o.category || '其他'
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key).push(o)
  })
  return [...groups.entries()].map(([category, options]) => ({ category, options }))
}

function selectOptions(f, model) {
  if (props.resource === 'region' && f.name === 'managerId' && regionManagerOptions.value) {
    return regionManagerOptions.value
  }
  if (props.resource === 'department' && f.name === 'contactId' && departmentContactOptions.value) {
    return departmentContactOptions.value
  }
  if (props.resource === 'contract' && f.name === 'managerId' && contractManagerOptions.value) {
    return contractManagerOptions.value
  }
  if (['inspectionPlan', 'maintenancePlan'].includes(props.resource)
      && f.name === 'assigneeId' && planAssigneeOptions.value) {
    return planAssigneeOptions.value
  }
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
function skillTags(value) {
  if (!value) return []
  return String(value).split(/[、,，]/).map((s) => s.trim()).filter(Boolean)
}
function formatDate(v) {
  return v ? String(v).slice(0, 10) : '—'
}

async function loadMeta() {
  const res = await crudMeta(props.resource)
  meta.value = res.data
  applyDefaultColumns()
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
  regionManagerOptions.value = null
  departmentContactOptions.value = null
  contractManagerOptions.value = null
  planAssigneeOptions.value = null
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
  formFields.value.forEach((f) => {
    if (f.type !== 'autocomplete' || !f.dictType) return
    const value = form[f.name]
    if (!value) return
    const matched = (dicts[f.dictType] || []).find((d) => String(d.dictValue) === String(value))
    if (matched) form[f.name] = matched.dictLabel
  })
  applyFormDefaults()
  if (store.hospitalId) form.hospitalId = store.hospitalId
  if (props.resource === 'region') await loadRegionManagerOptions()
  if (props.resource === 'department') await loadDepartmentContactOptions()
  if (props.resource === 'contract') await loadContractManagerOptions()
  if (['inspectionPlan', 'maintenancePlan'].includes(props.resource)) await loadPlanAssigneeOptions()
  dialog.value = true
}

async function loadContractManagerOptions() {
  contractManagerOptions.value = null
  if (!form.hospitalId) return
  try {
    const res = await request.get(`/customer/hospital/${form.hospitalId}/manager-options`)
    contractManagerOptions.value = res.data || []
  } catch (e) {
    contractManagerOptions.value = []
  }
}

watch(() => form.hospitalId, () => {
  if (props.resource === 'contract' && dialog.value) loadContractManagerOptions()
  if (['inspectionPlan', 'maintenancePlan'].includes(props.resource) && dialog.value) loadPlanAssigneeOptions()
})

async function loadPlanAssigneeOptions() {
  planAssigneeOptions.value = null
  if (!form.hospitalId) return
  try {
    const res = await request.get(`/customer/hospital/${form.hospitalId}/manager-options`)
    planAssigneeOptions.value = res.data || []
  } catch (e) {
    planAssigneeOptions.value = []
  }
}

async function loadDepartmentContactOptions() {
  departmentContactOptions.value = null
  if (!form.departmentId || !form.hospitalId) return
  try {
    const res = await request.get('/crud/contact/options', {
      params: { hospitalId: form.hospitalId, departmentId: form.departmentId, status: '0' }
    })
    departmentContactOptions.value = res.data || []
  } catch (e) {
    departmentContactOptions.value = []
  }
}

async function loadRegionManagerOptions() {
  if (!form.regionId) {
    regionManagerOptions.value = null
    return
  }
  try {
    const res = await request.get(`/customer/region/${form.regionId}/manager-options`)
    regionManagerOptions.value = res.data || []
  } catch (e) {
    regionManagerOptions.value = null
  }
}

function isSwitchOn(f, row) {
  return String(row[f.name]) === String(f.switchOnValue || '1')
}

async function toggleTableSwitch(row, f, value) {
  if (!can('edit')) return
  try {
    await request.put(`/crud/${props.resource}`, {
      [meta.value.pk]: row[meta.value.pk],
      [f.name]: value ? (f.switchOnValue || '1') : (f.switchOffValue || '0')
    })
    if (value) {
      ElMessage.success('已开启')
    } else {
      ElMessage({ message: '已关闭', type: 'warning', customClass: 'toast-off' })
    }
    await load()
  } catch (e) {
    await load()
  }
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
  const idFields = /^(hospitalId|campusId|departmentId|equipmentId|engineerId|deviceTypeId)$/
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
.switch-cell { display: inline-flex; align-items: center; gap: 4px; white-space: nowrap; }
.switch-label { color: #9aa4b2; font-size: 11px; }
.switch-label.active { color: #303133; font-weight: 600; }
.detail-skill-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.skill-tags { min-width: 0; }
.skill-tag-box { display: inline-flex; align-items: center; flex-wrap: nowrap; gap: 4px; max-width: 190px; overflow: hidden; }
.skill-tag { max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.crud-dialog-head { padding-right: 28px; }
.crud-dialog-title { font-size: 17px; font-weight: 600; color: #1f2d3d; letter-spacing: .01em; }
.crud-dialog-sub { margin-top: 4px; font-size: 12px; color: #8a99ad; }
.crud-form :deep(.el-form-item__label) { color: #52637a; font-weight: 500; }
.crud-form :deep(.el-row) { row-gap: 2px; }
</style>
