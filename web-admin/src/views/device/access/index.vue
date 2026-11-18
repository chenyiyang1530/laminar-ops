<template>
  <div class="access-page">
    <div class="access-head">
      <div>
        <h2>设备接入</h2>
        <p>管理现场网关、Token 与上报通道。设备与点位请在“设备台账”“监测点位”中维护。</p>
      </div>
      <div class="head-actions">
        <el-button @click="openTest">测试上报</el-button>
        <el-button v-if="canEdit" type="primary" @click="openAdd">新增网关</el-button>
      </div>
    </div>

    <div class="endpoint-strip">
      <div class="endpoint-main">
        <span class="endpoint-label">网关上报地址</span>
        <code>POST /api/open/monitor/report</code>
        <button class="copy-btn" type="button" @click="copy('/api/open/monitor/report')">复制</button>
      </div>
      <div class="endpoint-meta">
        <span>批量：<code>/api/open/monitor/report/batch</code></span>
        <span>Header：<code>X-Monitor-Token</code></span>
      </div>
    </div>

    <div class="gateway-table">
      <el-table :data="gateways" v-loading="loading" size="small">
        <el-table-column prop="gatewayCode" label="网关编码" width="150" />
        <el-table-column prop="gatewayName" label="网关名称" min-width="160" />
        <el-table-column prop="protocol" label="协议" width="110" />
        <el-table-column prop="address" label="地址" min-width="170" show-overflow-tooltip />
        <el-table-column prop="ipWhitelist" label="IP 白名单" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.ipWhitelist || '不限制' }}</template>
        </el-table-column>
        <el-table-column label="Token" min-width="210">
          <template #default="{ row }">
            <span class="token">{{ mask(row.token) }}</span>
            <el-button v-if="canEdit" link type="primary" @click="copy(row.token)">复制</el-button>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled === '1'" :disabled="!canEdit" @change="(v) => toggle(row, v)" />
          </template>
        </el-table-column>
        <el-table-column prop="lastSeen" label="最后上报" width="160" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-if="canEdit" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" v-if="canEdit" @click="rotate(row)">轮换</el-button>
            <el-button link type="primary" @click="openTokenLogs(row)">审计</el-button>
            <el-button link type="danger" v-if="canEdit" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !gateways.length" class="empty">
        还没有接入网关。新增网关后，把 Token 配置到现场网关即可开始上报。
      </div>
    </div>

    <div class="test-log-section">
      <div class="log-head">
        <div>
          <h3>最近测试记录</h3>
          <p>最多显示 10 条，可按设备、结果和时间筛选</p>
        </div>
        <div class="log-filters">
          <el-select v-model="logFilter.equipmentCode" clearable filterable placeholder="全部设备" style="width: 190px" @change="loadTestLogs">
            <el-option v-for="item in equipmentOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="logFilter.result" clearable placeholder="全部结果" style="width: 110px" @change="loadTestLogs">
            <el-option label="成功" value="1" />
            <el-option label="失败" value="0" />
          </el-select>
          <el-date-picker v-model="logFilter.range" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 240px" @change="loadTestLogs" />
        </div>
      </div>
      <el-table :data="testLogs" size="small" class="log-table" v-loading="testLogLoading">
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column prop="equipmentName" label="设备" min-width="150" show-overflow-tooltip />
        <el-table-column prop="pointName" label="点位" min-width="150" show-overflow-tooltip />
        <el-table-column label="测试值" width="110">
          <template #default="{ row }">{{ row.value }}{{ row.unit || '' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="88">
          <template #default="{ row }">
            <el-tag size="small" :type="row.result === '1' ? 'success' : 'danger'" effect="plain">
              {{ row.result === '1' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="告警" width="76">
          <template #default="{ row }">
            <el-tag v-if="row.alarmTriggered === '1'" size="small" type="warning" effect="plain">触发</el-tag>
            <span v-else class="log-muted">未触发</span>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人" width="110" show-overflow-tooltip />
      </el-table>
      <div v-if="!testLogs.length" class="empty">暂无测试记录</div>
    </div>

    <el-dialog v-model="dialog" :title="form.gatewayId ? '编辑网关' : '新增网关'" width="560px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="网关编码" required><el-input v-model="form.gatewayCode" placeholder="例如 GW-AHU-01" /></el-form-item>
        <el-form-item label="网关名称" required><el-input v-model="form.gatewayName" placeholder="例如 住院楼 3F 网关" /></el-form-item>
        <el-form-item label="协议">
          <el-select v-model="form.protocol" style="width: 100%">
            <el-option label="Modbus TCP" value="Modbus TCP" />
            <el-option label="Modbus RTU" value="Modbus RTU" />
            <el-option label="BACnet" value="BACnet" />
            <el-option label="OPC UA" value="OPC UA" />
            <el-option label="MQTT" value="MQTT" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="网关地址"><el-input v-model="form.address" placeholder="如 192.168.10.20:502" /></el-form-item>
        <el-form-item label="IP 白名单"><el-input v-model="form.ipWhitelist" type="textarea" :rows="2" placeholder="允许上报的 IP，逗号分隔；支持 192.168.1.*，留空不限制" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tokenVisible" title="网关 Token" width="520px">
      <el-alert title="Token 只在这里展示，请复制到现场网关配置中；不要写入公开文档。" type="warning" :closable="false" show-icon />
      <div class="token-box">{{ issuedToken }}</div>
      <template #footer><el-button type="primary" @click="copy(issuedToken)">复制 Token</el-button></template>
    </el-dialog>

    <el-dialog v-model="tokenLogVisible" :title="`Token 审计 · ${tokenLogTitle}`" width="600px">
      <el-table :data="tokenLogs" size="small">
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column label="动作" width="90">
          <template #default="{ row }">{{ row.action === 'CREATE' ? '生成' : '轮换' }}</template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="tokenSuffix" label="Token 后 8 位" min-width="150">
          <template #default="{ row }">…{{ row.tokenSuffix }}</template>
        </el-table-column>
      </el-table>
      <div v-if="!tokenLogs.length" class="empty">暂无 Token 审计记录</div>
    </el-dialog>

    <el-dialog v-model="testVisible" title="测试上报" width="560px">
      <el-form :model="testForm" label-width="92px">
        <el-form-item label="设备">
          <el-select v-model="testForm.equipmentCode" filterable placeholder="选择设备" style="width: 100%" @change="loadTestPoints">
            <el-option v-for="item in equipmentOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="监测点位">
          <el-select v-model="testForm.pointCode" filterable :disabled="!testForm.equipmentCode" placeholder="选择监测点位" style="width: 100%" @change="scheduleRulePreview">
            <el-option v-for="item in pointOptions" :key="item.value" :label="item.label + (item.unit ? ` (${item.unit})` : '')" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedPoint" label="阈值提示">
          <div class="threshold-hint">
            告警：{{ boundText(selectedPoint.alarmLower, selectedPoint.alarmUpper, selectedPoint.unit) }}
            <span>预警：{{ boundText(selectedPoint.warnLower, selectedPoint.warnUpper, selectedPoint.unit) }}</span>
          </div>
          <div v-if="selectedPoint.latestTime" class="latest-value">
            最近值：{{ selectedPoint.latestValue }}{{ selectedPoint.unit || '' }} · {{ selectedPoint.latestTime }}
          </div>
        </el-form-item>
        <el-form-item label="测试值">
          <el-input v-model="testForm.value" placeholder="例如 23.6" @input="scheduleRulePreview">
            <template v-if="selectedPoint?.unit" #append>{{ selectedPoint.unit }}</template>
          </el-input>
        </el-form-item>
        <div v-if="preview" class="value-preview" :class="preview.type">{{ preview.text }}</div>
        <div v-if="rulePreview" class="rule-preview" :class="{ danger: rulePreview.breach }">
          <span class="rule-label">规则预览</span>
          <span>{{ rulePreview.message }}</span>
          <span v-if="rulePreview.ruleMatched" class="rule-name">已匹配：{{ rulePreview.ruleName }}</span>
        </div>
      </el-form>
      <div v-if="!equipmentOptions.length && testVisible" class="empty">当前没有可测试的设备，请先在设备台账维护设备。</div>
      <div v-if="testResult" class="test-result">
        {{ testResult }}
      </div>
      <template #footer>
        <el-button @click="testVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testing" @click="runTest">发送测试</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canEdit = store.hasPerm('device:equipment:edit')
const loading = ref(false)
const gateways = ref([])
const dialog = ref(false)
const saving = ref(false)
const form = reactive({})
const tokenVisible = ref(false)
const issuedToken = ref('')
const testVisible = ref(false)
const testing = ref(false)
const testResult = ref('')
const equipmentOptions = ref([])
const pointOptions = ref([])
const testLogs = ref([])
const testLogLoading = ref(false)
const logFilter = reactive({ equipmentCode: '', result: '', range: [] })
const rulePreview = ref(null)
const previewTimer = ref(null)
const tokenLogVisible = ref(false)
const tokenLogTitle = ref('')
const tokenLogs = ref([])
const testForm = reactive({ equipmentCode: '', pointCode: '', value: '' })
const selectedPoint = computed(() => pointOptions.value.find((p) => p.value === testForm.pointCode) || null)
const preview = computed(() => {
  const p = selectedPoint.value
  const value = Number(testForm.value)
  if (!p || testForm.value === '' || Number.isNaN(value)) return null
  const hasThreshold = [p.alarmLower, p.alarmUpper, p.warnLower, p.warnUpper].some((v) => v != null)
  if (!hasThreshold) return { type: 'muted', text: '该点位未配置阈值，无法提前判断；以告警规则和实际上报结果为准' }
  const alarm = (p.alarmLower != null && value < Number(p.alarmLower)) || (p.alarmUpper != null && value > Number(p.alarmUpper))
  const warn = (p.warnLower != null && value < Number(p.warnLower)) || (p.warnUpper != null && value > Number(p.warnUpper))
  if (alarm) return { type: 'danger', text: '当前值超出告警范围，发送后会触发告警' }
  if (warn) return { type: 'warning', text: '当前值超出预警范围，尚未达到告警阈值' }
  return { type: 'normal', text: '当前值在正常范围内' }
})

async function load() {
  loading.value = true
  try {
    gateways.value = (await request.get('/monitor/access/gateways')).data || []
  } finally {
    loading.value = false
  }
}

function openAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { protocol: 'Modbus TCP' })
  dialog.value = true
}

function openEdit(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { ...row })
  dialog.value = true
}

async function submit() {
  if (!form.gatewayCode || !form.gatewayName) {
    ElMessage.warning('请填写网关编码和名称')
    return
  }
  saving.value = true
  try {
    const res = await request.post('/monitor/access/gateways', form)
    dialog.value = false
    await load()
    issuedToken.value = res.data.token
    tokenVisible.value = Boolean(res.data.token)
    ElMessage.success('网关已保存')
  } finally {
    saving.value = false
  }
}

async function toggle(row, enabled) {
  await request.put(`/monitor/access/gateways/${row.gatewayId}/enabled`, null, { params: { enabled: enabled ? '1' : '0' } })
  row.enabled = enabled ? '1' : '0'
  ElMessage.success(enabled ? '网关已启用' : '网关已停用')
}

async function rotate(row) {
  await ElMessageBox.confirm(`确认轮换网关「${row.gatewayName}」的 Token？旧 Token 会立即失效。`, '提示', { type: 'warning' })
  const res = await request.post(`/monitor/access/gateways/${row.gatewayId}/rotate`)
  issuedToken.value = res.data.token
  tokenVisible.value = true
  await load()
}

async function openTokenLogs(row) {
  tokenLogTitle.value = row.gatewayName
  tokenLogVisible.value = true
  const res = await request.get(`/monitor/access/gateways/${row.gatewayId}/tokenLogs`)
  tokenLogs.value = res.data || []
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除网关「${row.gatewayName}」？设备会解除绑定。`, '提示', { type: 'warning' })
  await request.delete(`/monitor/access/gateways/${row.gatewayId}`)
  ElMessage.success('已删除')
  await load()
}

async function runTest() {
  if (!testForm.equipmentCode || !testForm.pointCode || !testForm.value) {
    ElMessage.warning('请选择设备、监测点位并填写测试值')
    return
  }
  testing.value = true
  testResult.value = ''
  try {
    const res = await request.post('/monitor/access/test', testForm)
    const d = res.data
    testResult.value = `${d.message}（写入 ${d.count} 个点位，当前值 ${d.value}${d.unit || ''}）`
    await loadTestLogs()
  } catch (e) {
    testResult.value = e?.response?.data?.msg || e?.message || '上报失败，请检查设备、点位和模拟开关'
  } finally {
    testing.value = false
  }
}

async function openTest() {
  testVisible.value = true
  testResult.value = ''
  testForm.equipmentCode = ''
  testForm.pointCode = ''
  testForm.value = ''
  pointOptions.value = []
  rulePreview.value = null
  if (!equipmentOptions.value.length) await loadEquipmentOptions()
}

async function loadTestPoints() {
  testForm.pointCode = ''
  pointOptions.value = []
  rulePreview.value = null
  if (!testForm.equipmentCode) return
  const res = await request.get('/monitor/access/test/points', { params: { equipmentCode: testForm.equipmentCode } })
  pointOptions.value = res.data || []
}

async function loadEquipmentOptions() {
  const res = await request.get('/monitor/access/test/equipment')
  equipmentOptions.value = res.data || []
}

async function loadTestLogs() {
  testLogLoading.value = true
  try {
    const params = { result: logFilter.result || undefined }
    if (logFilter.equipmentCode) params.equipmentCode = logFilter.equipmentCode
    if (logFilter.range?.length === 2) {
      params.startDate = logFilter.range[0]
      params.endDate = logFilter.range[1]
    }
    const res = await request.get('/monitor/access/test/logs', { params })
    testLogs.value = res.data || []
  } finally {
    testLogLoading.value = false
  }
}

function scheduleRulePreview() {
  if (previewTimer.value) clearTimeout(previewTimer.value)
  rulePreview.value = null
  if (!testForm.equipmentCode || !testForm.pointCode || testForm.value === '') return
  previewTimer.value = setTimeout(async () => {
    try {
      const res = await request.post('/monitor/access/test/preview', {
        equipmentCode: testForm.equipmentCode,
        pointCode: testForm.pointCode,
        value: testForm.value
      })
      rulePreview.value = res.data
    } catch (e) {
      rulePreview.value = { breach: false, message: e?.response?.data?.msg || '规则预览失败' }
    }
  }, 280)
}

async function copy(value) {
  if (!value) return
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success('已复制')
  } catch (e) {
    ElMessage.warning('浏览器不允许自动复制，请手动选择')
  }
}

function mask(token) {
  if (!token) return '—'
  return token.length > 14 ? `${token.slice(0, 10)}…${token.slice(-4)}` : token
}

function boundText(lower, upper, unit = '') {
  if (lower == null && upper == null) return '未配置'
  if (lower != null && upper != null) return `${lower} ~ ${upper}${unit}`
  if (lower != null) return `≥ ${lower}${unit}`
  return `≤ ${upper}${unit}`
}

onMounted(() => {
  load()
  loadEquipmentOptions()
  loadTestLogs()
})
</script>

<style scoped>
.access-page { --a-ink: #111111; --a-line: #e5e7eb; --a-soft: #f8f9fa; --a-mute: #6b7280; color: var(--a-ink); }
.access-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; padding: 4px 0 16px; border-bottom: 1px solid var(--a-line); }
.access-head h2 { margin: 0; font-size: 21px; font-weight: 600; letter-spacing: -.02em; }
.access-head p { margin: 5px 0 0; color: var(--a-mute); font-size: 12px; }
.head-actions { display: flex; gap: 8px; }
.endpoint-strip { margin: 16px 0; padding: 16px 18px; background: var(--a-soft); border: 1px solid var(--a-line); border-radius: 12px; }
.endpoint-main { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.endpoint-label { color: var(--a-mute); font-size: 12px; }
.endpoint-main code { font-family: Consolas, monospace; font-size: 12px; color: #111; }
.endpoint-meta { display: flex; gap: 18px; margin-top: 9px; color: var(--a-mute); font-size: 11px; flex-wrap: wrap; }
.endpoint-meta code { font-family: Consolas, monospace; color: #374151; }
.copy-btn { border: 1px solid var(--a-line); background: #fff; border-radius: 6px; height: 24px; padding: 0 8px; font-size: 11px; color: var(--a-mute); cursor: pointer; }
.copy-btn:hover { color: #111; }
.gateway-table { border: 1px solid var(--a-line); border-radius: 12px; overflow: hidden; background: #fff; }
.token { font-family: Consolas, monospace; font-size: 11px; color: #4b5563; margin-right: 6px; }
.empty { padding: 22px; text-align: center; color: #9ca3af; font-size: 12px; }
.token-box { margin-top: 16px; padding: 14px; border: 1px dashed #d1d5db; border-radius: 8px; background: #f8f9fa; font-family: Consolas, monospace; font-size: 12px; word-break: break-all; }
.test-result { margin-top: 12px; padding: 10px 12px; border-radius: 6px; background: #f0f9ff; color: #0c4a6e; font-size: 12px; }
.threshold-hint { color: #4b5563; font-size: 12px; line-height: 1.6; }
.threshold-hint span { margin-left: 16px; color: #9ca3af; }
.latest-value { margin-top: 2px; color: #9ca3af; font-size: 11px; }
.value-preview { margin: -2px 0 8px 92px; padding: 7px 10px; border-radius: 6px; font-size: 12px; background: #f8f9fa; color: #6b7280; }
.value-preview.normal { background: #f0fdf4; color: #166534; }
.value-preview.warning { background: #fffbeb; color: #92400e; }
.value-preview.danger { background: #fef2f2; color: #b91c1c; }
.value-preview.muted { background: #f8f9fa; color: #6b7280; }
.rule-preview { margin: -2px 0 10px 92px; padding: 7px 10px; border-radius: 6px; background: #f8f9fa; color: #4b5563; font-size: 11px; line-height: 1.5; }
.rule-preview.danger { background: #fff7ed; color: #9a3412; }
.rule-label { font-weight: 600; margin-right: 8px; }
.rule-name { margin-left: 8px; color: #9ca3af; }
.test-log-section { margin-top: 18px; border: 1px solid var(--a-line); border-radius: 12px; background: #fff; padding: 16px 18px; }
.log-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; }
.log-head h3 { margin: 0; font-size: 15px; font-weight: 600; }
.log-head p { margin: 4px 0 10px; color: var(--a-mute); font-size: 11px; }
.log-filters { display: flex; gap: 8px; flex-wrap: wrap; }
.log-table :deep(.el-table__header th) { background: transparent; color: var(--a-mute); font-size: 11px; font-weight: 500; }
.log-table :deep(.el-table__inner-wrapper::before) { display: none; }
.log-muted { color: #9ca3af; font-size: 11px; }
</style>
