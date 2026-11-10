<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div>
          <h2>字典管理</h2>
          <div class="sub">展开字典类型，直接维护该类型下的字典数据</div>
        </div>
        <el-button v-if="can('add')" type="primary" :icon="Plus" @click="openTypeAdd">新增字典类型</el-button>
      </div>

      <el-form :model="query" inline class="search-form">
        <el-form-item label="字典名称">
          <el-input v-model="query.dictName" clearable placeholder="搜索字典名称" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="字典类型">
          <el-input v-model="query.dictType" clearable placeholder="搜索字典类型编码" @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="types" row-key="dictId" @expand-change="onExpand">
        <el-table-column type="expand" width="42">
          <template #default="{ row }">
            <div class="dict-data-panel">
              <div class="dict-data-head">
                <div class="dict-data-title">
                  <span class="dict-data-name">{{ row.dictName }}</span>
                  <code>{{ row.dictType }}</code>
                  <span class="dict-data-count">{{ (dataMap[row.dictType] || []).length }} 条字典数据</span>
                </div>
                <el-button v-if="can('add')" size="small" type="primary" plain :icon="Plus" @click="openDataAdd(row)">
                  新增字典数据
                </el-button>
              </div>
              <el-table
                v-loading="!!dataLoading[row.dictType]"
                :data="dataMap[row.dictType] || []"
                size="small"
                class="dict-data-table"
              >
                <el-table-column prop="dictLabel" label="标签" min-width="150" />
                <el-table-column prop="dictValue" label="键值" width="150">
                  <template #default="{ row: d }"><code class="dict-value">{{ d.dictValue }}</code></template>
                </el-table-column>
                <el-table-column prop="dictSort" label="排序" width="70" align="center" />
                <el-table-column label="状态" width="80">
                  <template #default="{ row: d }">
                    <el-tag :type="d.status === '0' ? 'success' : 'info'" size="small">
                      {{ d.status === '0' ? '正常' : '停用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
                <el-table-column label="操作" width="130" fixed="right">
                  <template #default="{ row: d }">
                    <el-button v-if="can('edit')" link type="primary" @click="openDataEdit(row, d)">编辑</el-button>
                    <el-button v-if="can('remove')" link type="danger" @click="removeData(row, d)">删除</el-button>
                  </template>
                </el-table-column>
                <template #empty>
                  <el-empty description="该字典暂无数据" :image-size="46" />
                </template>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="dictName" label="字典名称" min-width="180" />
        <el-table-column prop="dictType" label="字典类型" min-width="200">
          <template #default="{ row }"><code class="dict-type-code">{{ row.dictType }}</code></template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('edit')" link type="primary" @click="openTypeEdit(row)">编辑</el-button>
            <el-button v-if="can('remove')" link type="danger" @click="removeType(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadTypes"
          @size-change="loadTypes"
        />
      </div>
    </div>

    <el-dialog v-model="typeDialog" :title="typeForm.dictId ? '编辑字典类型' : '新增字典类型'" width="520px" destroy-on-close>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="例如：设备大类" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="typeForm.dictType" :disabled="typeCodeLocked" placeholder="例如：device_category" />
          <div v-if="typeCodeLocked" class="form-tip">该字典下已有数据，类型编码不可修改</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveType">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dataDialog" :title="dataForm.dictCode ? '编辑字典数据' : '新增字典数据'" width="520px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon class="data-dialog-tip">
        <template #title>字典类型：{{ dataForm.dictType }}（{{ dataTypeName }}）</template>
      </el-alert>
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="80px">
        <el-form-item label="标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="键值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="实际存储值" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dataForm.dictSort" :min="0" :controls="false" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataForm.status">
            <el-radio value="0">正常</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveData">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const can = (action) => store.hasPerm('system:dict:' + action)

const loading = ref(false)
const saving = ref(false)
const types = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: store.pageSize || 10, dictName: '', dictType: '' })
const dataMap = reactive({})
const dataLoading = reactive({})

const typeDialog = ref(false)
const typeFormRef = ref()
const typeCodeLocked = ref(false)
const typeForm = reactive({})
const typeRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }]
}

const dataDialog = ref(false)
const dataFormRef = ref()
const dataForm = reactive({})
const dataTypeName = ref('')
const dataRules = {
  dictLabel: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入键值', trigger: 'blur' }]
}

async function loadTypes() {
  loading.value = true
  try {
    const res = await request.get('/crud/dictType/page', { params: query })
    types.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const search = () => { query.pageNum = 1; loadTypes() }
const reset = () => {
  query.dictName = ''
  query.dictType = ''
  query.pageNum = 1
  loadTypes()
}

function openTypeAdd() {
  Object.keys(typeForm).forEach((k) => delete typeForm[k])
  Object.assign(typeForm, { status: '0', remark: '' })
  typeCodeLocked.value = false
  typeDialog.value = true
}

async function openTypeEdit(row) {
  const res = await request.get('/crud/dictData/page', {
    params: { dictType: row.dictType, pageNum: 1, pageSize: 1 }
  })
  typeCodeLocked.value = (res.data.total || 0) > 0
  Object.keys(typeForm).forEach((k) => delete typeForm[k])
  Object.assign(typeForm, {
    dictId: row.dictId,
    dictName: row.dictName,
    dictType: row.dictType,
    status: row.status || '0',
    remark: row.remark || ''
  })
  typeDialog.value = true
}

async function saveType() {
  await typeFormRef.value.validate()
  saving.value = true
  try {
    if (typeForm.dictId) {
      await request.put('/crud/dictType', typeForm)
    } else {
      await request.post('/crud/dictType', typeForm)
    }
    ElMessage.success('保存成功')
    typeDialog.value = false
    loadTypes()
  } finally {
    saving.value = false
  }
}

async function removeType(row) {
  const res = await request.get('/crud/dictData/page', {
    params: { dictType: row.dictType, pageNum: 1, pageSize: 1 }
  })
  if ((res.data.total || 0) > 0) {
    ElMessage.warning('该字典下还有数据，请先展开删除字典数据')
    return
  }
  await ElMessageBox.confirm(`确认删除字典类型「${row.dictName}」？`, '提示', { type: 'warning' })
  await request.delete('/crud/dictType/' + row.dictId)
  ElMessage.success('已删除')
  delete dataMap[row.dictType]
  loadTypes()
}

async function onExpand(row, expandedRows) {
  const expanded = expandedRows.some((r) => r.dictId === row.dictId)
  if (!expanded) return
  await loadData(row, false)
}

async function loadData(row, force = false) {
  if (!force && dataMap[row.dictType]) return
  dataLoading[row.dictType] = true
  try {
    const res = await request.get('/crud/dictData/page', {
      params: { dictType: row.dictType, pageNum: 1, pageSize: 500 }
    })
    dataMap[row.dictType] = res.data.records || []
  } finally {
    dataLoading[row.dictType] = false
  }
}

function openDataAdd(typeRow) {
  Object.keys(dataForm).forEach((k) => delete dataForm[k])
  Object.assign(dataForm, {
    dictType: typeRow.dictType,
    dictLabel: '',
    dictValue: '',
    dictSort: (dataMap[typeRow.dictType]?.length || 0) + 1,
    status: '0',
    remark: ''
  })
  dataTypeName.value = typeRow.dictName
  dataDialog.value = true
}

function openDataEdit(typeRow, row) {
  Object.keys(dataForm).forEach((k) => delete dataForm[k])
  Object.assign(dataForm, {
    dictCode: row.dictCode,
    dictType: typeRow.dictType,
    dictLabel: row.dictLabel,
    dictValue: row.dictValue,
    dictSort: row.dictSort ?? 0,
    status: row.status || '0',
    remark: row.remark || ''
  })
  dataTypeName.value = typeRow.dictName
  dataDialog.value = true
}

async function saveData() {
  await dataFormRef.value.validate()
  saving.value = true
  try {
    if (dataForm.dictCode) {
      await request.put('/crud/dictData', dataForm)
    } else {
      await request.post('/crud/dictData', dataForm)
    }
    ElMessage.success('保存成功')
    dataDialog.value = false
    const typeRow = types.value.find((t) => t.dictType === dataForm.dictType)
    if (typeRow) await loadData(typeRow, true)
  } finally {
    saving.value = false
  }
}

async function removeData(typeRow, row) {
  await ElMessageBox.confirm(`确认删除字典数据「${row.dictLabel}」？`, '提示', { type: 'warning' })
  await request.delete('/crud/dictData/' + row.dictCode)
  ElMessage.success('已删除')
  await loadData(typeRow, true)
}

onMounted(loadTypes)
</script>

<style scoped>
.search-form { margin-bottom: 4px; }
.table-footer { display: flex; justify-content: flex-end; margin-top: 16px; }
.dict-data-panel {
  margin: 4px 12px 12px 42px;
  padding: 12px 14px 6px;
  background: #f8fafc;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}
.dict-data-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}
.dict-data-title { display: flex; align-items: center; gap: 10px; min-width: 0; }
.dict-data-name { font-size: 13px; font-weight: 600; color: #1f2d3d; }
.dict-data-count { font-size: 12px; color: #8a99ab; }
.dict-data-table { background: #fff; border-radius: 6px; }
.dict-type-code, .dict-value {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: #315277;
  background: #f2f6fb;
  border-radius: 4px;
  padding: 2px 6px;
}
.form-tip { margin-top: 4px; font-size: 12px; color: #a8730b; }
.data-dialog-tip { margin-bottom: 14px; }
:deep(.el-table__expand-icon) { color: #5a6b80; }
</style>
