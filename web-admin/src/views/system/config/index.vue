<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div>
          <h2>平台参数</h2>
          <div class="sub">平台级运行配置，仅系统管理员可维护；不包含医院/公司业务参数</div>
        </div>
        <el-button v-if="can('add')" type="primary" :icon="Plus" @click="openAdd">新增参数</el-button>
      </div>

      <el-form :model="query" inline class="search-form">
        <el-form-item label="参数名称">
          <el-input v-model="query.configName" clearable placeholder="搜索参数名称" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="参数键名">
          <el-input v-model="query.configKey" clearable placeholder="搜索参数键名" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="query.groupCode" clearable placeholder="全部分组" style="width: 150px">
            <el-option v-for="g in groups" :key="g.value" :label="g.label" :value="g.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column type="index" label="序号" width="58" align="center" />
        <el-table-column prop="configName" label="参数名称" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.configName }}
            <el-tag v-if="isReserved(row.configKey)" size="small" type="info" effect="plain">预留</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="configKey" label="参数键名" min-width="230">
          <template #default="{ row }"><code class="key">{{ row.configKey }}</code></template>
        </el-table-column>
        <el-table-column label="分组" width="110">
          <template #default="{ row }">{{ groupLabel(row.groupCode) }}</template>
        </el-table-column>
        <el-table-column prop="configValue" label="当前值" min-width="150" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.valueType }}</el-tag></template>
        </el-table-column>
        <el-table-column label="范围" width="120">
          <template #default="{ row }">{{ rangeText(row) }}</template>
        </el-table-column>
        <el-table-column prop="defaultValue" label="默认值" min-width="110" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="150" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('edit')" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="can('edit')" link type="warning" @click="resetValue(row)">恢复默认</el-button>
            <el-button link @click="openLogs(row)">修改记录</el-button>
            <el-button v-if="can('remove')" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无参数" :image-size="46" /></template>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pager"
        @change="load"
      />
    </div>

    <el-dialog v-model="dialog" :title="form.configId ? '编辑参数' : '新增参数'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-alert
          v-if="isReserved(form.configKey)"
          title="该通知渠道尚未接入真实服务，仅保留开关；当前不能开启，开启不会发送。"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 14px"
        />
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="form.configName" maxlength="64" placeholder="例如：登录失败次数上限" />
        </el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input v-model="form.configKey" maxlength="100" placeholder="例如：sys.user.login.maxFail" />
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="form.groupCode" style="width: 100%">
            <el-option v-for="g in groups" :key="g.value" :label="g.label" :value="g.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="值类型">
          <el-select v-model="form.valueType" style="width: 100%" @change="onTypeChange">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="当前值" prop="configValue">
          <el-radio-group v-if="form.valueType === 'BOOLEAN'" v-model="form.configValue" :disabled="isReserved(form.configKey)">
            <el-radio value="true">true</el-radio>
            <el-radio value="false">false</el-radio>
          </el-radio-group>
          <el-input v-else-if="form.valueType === 'INTEGER' || form.valueType === 'DECIMAL'" v-model="form.configValue" :disabled="isReserved(form.configKey)" placeholder="请输入数字" />
          <el-input v-else v-model="form.configValue" :disabled="isReserved(form.configKey)" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item v-if="form.valueType === 'INTEGER' || form.valueType === 'DECIMAL'" label="最小值">
          <el-input-number v-model="form.minValue" :controls="false" placeholder="可不填" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="form.valueType === 'INTEGER' || form.valueType === 'DECIMAL'" label="最大值">
          <el-input-number v-model="form.maxValue" :controls="false" placeholder="可不填" style="width: 100%" />
        </el-form-item>
        <el-form-item label="默认值">
          <el-input v-model="form.defaultValue" maxlength="500" placeholder="恢复默认时使用" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="敏感参数">
          <el-switch v-model="form.isSensitive" :active-value="1" :inactive-value="0" />
          <span class="hint">敏感参数预留标记（当前界面仍显示实际值）</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logDialog" title="参数修改记录" width="820px">
      <el-table :data="logs" size="small" max-height="420">
        <el-table-column prop="createTime" label="时间" width="165" />
        <el-table-column prop="operatorName" label="操作人" width="110" />
        <el-table-column prop="oldValue" label="旧值" min-width="150" show-overflow-tooltip />
        <el-table-column prop="newValue" label="新值" min-width="150" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
      </el-table>
      <el-empty v-if="!logs.length" description="暂无修改记录" :image-size="44" />
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
const can = (action) => store.hasPerm('system:config:' + action)
const groups = [
  { value: 'account_security', label: '账号安全' },
  { value: 'session', label: '登录会话' },
  { value: 'upload', label: '文件上传' },
  { value: 'platform', label: '平台展示' },
  { value: 'notify', label: '通知设置' },
  { value: 'log', label: '日志保留' }
]
const types = ['BOOLEAN', 'INTEGER', 'DECIMAL', 'STRING', 'ENUM', 'SECRET']
const reservedKeys = new Set(['platform.notify.emailEnabled', 'platform.notify.smsEnabled'])
const isReserved = (key) => reservedKeys.has(key)
const groupLabel = (value) => groups.find((g) => g.value === value)?.label || value || '平台'
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const logDialog = ref(false)
const logs = ref([])
const formRef = ref()
const records = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, configName: '', configKey: '', groupCode: undefined })
const form = reactive({
  configId: undefined, configName: '', configKey: '', groupCode: 'platform', configValue: '',
  valueType: 'STRING', minValue: null, maxValue: null, defaultValue: '', sort: 0, isSensitive: 0, remark: ''
})
const rules = {
  configName: [{ required: true, message: '请填写参数名称', trigger: 'blur' }],
  configKey: [
    { required: true, message: '请填写参数键名', trigger: 'blur' },
    { pattern: /^[A-Za-z][A-Za-z0-9_.-]{2,99}$/, message: '字母开头，3-100 位字母、数字、下划线、点或横线', trigger: 'blur' }
  ]
}

async function load() {
  loading.value = true
  try {
    const res = await request.get('/system/config/page', { params: query })
    records.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}
function search() { query.pageNum = 1; load() }
function resetSearch() { Object.assign(query, { pageNum: 1, configName: '', configKey: '', groupCode: undefined }); load() }
function resetForm(row = {}) {
  Object.assign(form, {
    configId: undefined, configName: '', configKey: '', groupCode: 'platform', configValue: '',
    valueType: 'STRING', minValue: null, maxValue: null, defaultValue: '', sort: 0, isSensitive: 0, remark: ''
  }, row)
}
function rangeText(row) {
  if (row.minValue == null && row.maxValue == null) return '—'
  return `${row.minValue ?? '不限'} ~ ${row.maxValue ?? '不限'}`
}
function onTypeChange() {
  if (form.valueType === 'BOOLEAN' && !['true', 'false'].includes(String(form.configValue))) form.configValue = 'false'
}
function openAdd() { resetForm(); dialog.value = true }
async function openEdit(row) {
  const res = await request.get('/system/config/' + row.configId)
  resetForm(res.data)
  if (isReserved(form.configKey)) form.configValue = 'false'
  dialog.value = true
}
async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.configId) await request.put('/system/config/' + form.configId, form)
    else await request.post('/system/config', form)
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function resetValue(row) {
  await ElMessageBox.confirm(`确认将「${row.configName}」恢复为默认值？`, '恢复默认', { type: 'warning' })
  await request.post(`/system/config/${row.configId}/reset`)
  ElMessage.success('已恢复默认值')
  load()
}
async function openLogs(row) {
  logDialog.value = true
  logs.value = (await request.get('/system/config/logs', { params: { configId: row.configId } })).data || []
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除参数「${row.configName}」？`, '提示', { type: 'warning' })
  await request.delete('/system/config/' + row.configId)
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>

<style scoped>
.key { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; color: #475569; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; }
.hint { margin-left: 10px; font-size: 12px; color: #94a3b8; }
.pager { margin-top: 14px; justify-content: flex-end; }
</style>
