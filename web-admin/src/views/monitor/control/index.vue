<template>
  <div class="control-page">
    <div class="head">
      <div>
        <h2>远程控制</h2>
        <p>先选择 PLC 品牌和协议；未接硬件时使用模拟器验证启动/停止流程。</p>
      </div>
      <div>
        <el-tag v-if="status.configured" :type="status.online ? 'success' : 'danger'">
          {{ status.online ? '控制器在线' : '控制器离线' }}
        </el-tag>
        <el-tag v-if="status.pendingCount" type="warning" effect="dark" style="margin-left: 8px">
          待复核 {{ status.pendingCount }}
        </el-tag>
      </div>
    </div>

    <el-card shadow="never" class="card">
      <el-form label-width="100px" style="max-width: 720px">
        <el-form-item label="设备">
          <el-input v-model="equipmentKeyword" clearable placeholder="搜索设备名称、编码、医院" style="width: 100%; margin-bottom: 6px" />
          <el-select v-model="equipmentId" filterable style="width: 100%" @change="loadStatus">
            <el-option v-for="e in filteredEquipmentOptions" :key="e.value"
              :label="`${e.equipmentCode || ''} · ${e.equipmentName || ''} · ${e.hospitalName || '未关联医院'} · ${e.equipmentStatus === '0' ? '正常' : '停用'}`"
              :value="e.value" />
          </el-select>
          <div v-if="!equipmentOptions.length" class="empty-hint">{{ emptyHint }}</div>
        </el-form-item>
      </el-form>
    </el-card>

    <template v-if="equipmentId">
      <el-card v-if="status.configured" shadow="never" class="card">
        <template #header>
          <div class="card-head">
            <b>控制状态</b>
            <el-button link type="primary" @click="editing = !editing">{{ editing ? '收起配置' : '编辑配置' }}</el-button>
          </div>
        </template>
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="品牌">{{ config.brand || '—' }}</el-descriptions-item>
          <el-descriptions-item label="协议">{{ config.protocol || '—' }}</el-descriptions-item>
          <el-descriptions-item label="运行状态">{{ runText }}</el-descriptions-item>
          <el-descriptions-item label="就地/远程">{{ localText }}</el-descriptions-item>
          <el-descriptions-item label="联锁">{{ interlockText }}</el-descriptions-item>
          <el-descriptions-item label="故障码">{{ status.values?.faultCode ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="PLC 地址">{{ config.host || '模拟器' }}:{{ config.port || '—' }}</el-descriptions-item>
          <el-descriptions-item label="超时">{{ config.timeoutMs || 3000 }} ms</el-descriptions-item>
        </el-descriptions>
        <el-alert v-if="status.error" :title="status.error" type="error" :closable="false" show-icon style="margin-top: 12px" />
        <div class="actions">
          <el-button type="success" :disabled="!status.online || busy || !canControl" @click="command('START')">远程启动</el-button>
          <el-button type="danger" :disabled="!status.online || busy || !canControl" @click="command('STOP')">远程停止</el-button>
          <el-button plain :disabled="!status.online" @click="testConnection">测试 PLC 连通性</el-button>
          <span class="hint">提交后进入待复核，第二人复核通过才会写 PLC。</span>
        </div>
        <el-alert v-if="testResult" :title="testResult" :type="testOk ? 'success' : 'error'" :closable="true" show-icon style="margin-top: 10px" />
      </el-card>

      <el-card v-if="editing || !status.configured" shadow="never" class="card">
        <template #header><b>{{ status.configured ? '编辑控制配置' : '新增控制配置' }}</b></template>
        <el-form :model="form" label-width="120px" style="max-width: 760px">
          <el-form-item label="PLC 品牌">
            <el-select v-model="form.brand" style="width: 100%">
              <el-option label="西门子 S7-200（经典）" value="SIEMENS_S7_200" />
              <el-option label="西门子 S7-200 SMART" value="SIEMENS_S7_200_SMART" />
              <el-option label="西门子 LOGO!" value="SIEMENS_LOGO" />
              <el-option label="其他 PLC" value="OTHER" />
            </el-select>
          </el-form-item>
          <el-form-item label="通信协议">
            <el-select v-model="form.protocol" style="width: 100%">
              <el-option label="模拟器（无硬件联调）" value="SIMULATOR" />
              <el-option label="Modbus TCP（SMART / LOGO! 8.1+）" value="MODBUS_TCP" />
              <el-option label="S7 网关（HTTP 边缘网关）" value="S7_GATEWAY" />
            </el-select>
          </el-form-item>
          <el-form-item label="启用控制">
            <el-switch v-model="form.enabled" />
          </el-form-item>
          <el-form-item label="PLC IP">
            <el-input v-model="form.host" placeholder="例如 192.168.10.20；模拟器可留空" />
          </el-form-item>
          <el-form-item label="端口 / 站号">
            <div style="display:flex; gap:10px; width:100%">
              <el-input-number v-model="form.port" :min="1" :max="65535" style="flex:1" />
              <el-input-number v-model="form.unitId" :min="1" :max="247" style="flex:1" />
            </div>
          </el-form-item>
          <el-form-item label="区域">
            <el-radio-group v-model="form.area">
              <el-radio-button label="HOLDING">保持寄存器</el-radio-button>
              <el-radio-button label="COIL">线圈</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="启动地址"><el-input-number v-model="form.startAddress" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="停止地址"><el-input-number v-model="form.stopAddress" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="运行反馈地址"><el-input-number v-model="form.runStatusAddress" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="就地/远程地址"><el-input-number v-model="form.localRemoteAddress" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="联锁地址"><el-input-number v-model="form.interlockAddress" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="故障码地址"><el-input-number v-model="form.faultAddress" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="写入值 / 超时">
            <div style="display:flex; gap:10px; width:100%">
              <el-input-number v-model="form.writeValue" :min="0" :max="65535" style="flex:1" />
              <el-input-number v-model="form.timeoutMs" :min="500" :max="10000" :step="500" style="flex:1" />
            </div>
          </el-form-item>
          <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
          <el-form-item><el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button></el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="card">
        <template #header>
          <div class="card-head">
            <b>控制命令记录</b>
            <div class="history-filters">
              <el-select v-model="historyAction" size="small" placeholder="动作" style="width: 90px" clearable>
                <el-option label="启动" value="START" /><el-option label="停止" value="STOP" />
              </el-select>
              <el-select v-model="historyStatus" size="small" placeholder="状态" style="width: 130px" clearable>
                <el-option label="待复核" value="PENDING_REVIEW" /><el-option label="成功" value="SUCCESS" />
                <el-option label="超时" value="TIMEOUT" /><el-option label="失败" value="FAILED" />
                <el-option label="驳回" value="REJECTED" /><el-option label="已撤销" value="EXPIRED" />
              </el-select>
              <el-input v-model="historyOperator" size="small" placeholder="操作人" style="width: 120px" clearable />
              <el-date-picker v-model="historyRange" type="daterange" value-format="YYYY-MM-DD" size="small"
                start-placeholder="开始日期" end-placeholder="结束日期" style="width: 240px" />
              <el-button size="small" @click="openMonthly">月度审计</el-button>
            </div>
          </div>
        </template>
        <el-table :data="filteredHistory" size="small">
          <el-table-column prop="createTime" label="时间" width="170" />
          <el-table-column prop="action" label="动作" width="80" />
          <el-table-column prop="status" label="结果" width="120" />
          <el-table-column prop="operatorName" label="操作人" width="110" />
          <el-table-column prop="reviewBy" label="复核人" width="100" />
          <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
          <el-table-column prop="errorMessage" label="失败原因" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING_REVIEW' && canApprove">
                <el-button link type="success" @click="openReview(row)">复核</el-button>
              </template>
              <span v-else class="hint">—</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="canScope" shadow="never" class="card">
        <template #header><b>公司子管理员可控制设备范围</b></template>
        <el-form label-width="110px" style="max-width: 680px">
          <el-form-item label="子管理员">
            <el-select v-model="scopeForm.userId" style="width: 100%" @change="loadScope">
              <el-option v-for="u in scopeUsers" :key="u.userId" :label="`${u.nickName}（${u.username}）`" :value="u.userId" />
            </el-select>
          </el-form-item>
          <el-form-item label="可控制设备">
            <el-select v-model="scopeForm.equipmentIds" multiple filterable collapse-tags style="width: 100%">
              <el-option v-for="e in equipmentOptions" :key="e.value"
                :label="`${e.equipmentCode || ''} · ${e.equipmentName || ''}`" :value="e.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingScope" :disabled="!scopeForm.userId" @click="saveScope">保存范围</el-button>
            <el-button :disabled="!scopeForm.userId" @click="importDialog = true">批量导入</el-button>
            <span class="hint">未配置范围的子管理员看不到任何设备。</span>
          </el-form-item>
        </el-form>
      </el-card>

      <el-dialog v-model="importDialog" title="批量导入可控制设备" width="560px">
        <p class="hint">一行一个设备编码，也支持逗号分隔。</p>
        <el-input v-model="importCodes" type="textarea" :rows="8" placeholder="AHU-01&#10;AHU-02" />
        <template #footer>
          <el-button @click="importDialog = false">取消</el-button>
          <el-button type="primary" @click="importScope">导入</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="reviewDialog" title="控制命令复核" width="720px">
        <el-descriptions v-if="reviewRow" :column="2" border size="small">
          <el-descriptions-item label="命令号">{{ reviewRow.commandNo }}</el-descriptions-item>
          <el-descriptions-item label="动作">{{ reviewRow.action }}</el-descriptions-item>
          <el-descriptions-item label="发起人">{{ reviewRow.operatorName }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ reviewRow.createTime }}</el-descriptions-item>
          <el-descriptions-item label="原因" :span="2">{{ reviewRow.reason || '—' }}</el-descriptions-item>
        </el-descriptions>
        <div class="review-block">
          <b>提交时联锁快照</b>
          <pre class="snapshot">{{ pretty(reviewRow?.interlockSnapshot) }}</pre>
        </div>
        <div class="review-block">
          <b>当前实时状态</b>
          <pre class="snapshot">{{ JSON.stringify(status.values || {}, null, 2) }}</pre>
          <el-button size="small" @click="loadStatus">刷新状态</el-button>
        </div>
        <el-form label-width="90px" style="margin-top: 12px">
          <el-form-item label="复核意见">
            <el-input v-model="reviewRemark" type="textarea" :rows="2" placeholder="同意/驳回原因" />
          </el-form-item>
          <el-form-item label="确认文本">
            <el-input v-model="reviewConfirmText" placeholder="通过时必须输入 CONFIRM" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="reviewDialog = false">取消</el-button>
          <el-button type="danger" @click="submitReview(false)">驳回</el-button>
          <el-button type="success" @click="submitReview(true)">复核通过并执行</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="monthlyDialog" title="月度控制审计报表" width="860px">
        <div class="monthly-toolbar">
          <el-input v-model="monthlyValue" placeholder="YYYY-MM" style="width: 130px" />
          <el-button @click="loadMonthly">查询</el-button>
          <el-button @click="exportMonthly">导出 CSV</el-button>
          <span class="hint">总数 {{ monthly.summary.total || 0 }} · 成功 {{ monthly.summary.success || 0 }} · 待复核 {{ monthly.summary.pending || 0 }} · 其他 {{ monthly.summary.failed || 0 }}</span>
        </div>
        <el-table :data="monthly.records" size="small" max-height="420">
          <el-table-column prop="createTime" label="时间" width="165" />
          <el-table-column prop="equipmentName" label="设备" min-width="140" />
          <el-table-column prop="action" label="动作" width="80" />
          <el-table-column prop="status" label="状态" width="110" />
          <el-table-column prop="operatorName" label="操作人" width="100" />
          <el-table-column prop="reviewBy" label="复核人" width="100" />
          <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
        </el-table>
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canControl = store.hasPerm('device:control')
const canApprove = store.hasPerm('device:control:approve')
const canScope = store.hasPerm('device:control:scope')

const equipmentOptions = ref([])
const equipmentContext = ref({})
const equipmentKeyword = ref('')
const filteredEquipmentOptions = computed(() => {
  const keyword = equipmentKeyword.value.trim().toLowerCase()
  if (!keyword) return equipmentOptions.value
  return equipmentOptions.value.filter((e) =>
    `${e.equipmentName || ''} ${e.equipmentCode || ''} ${e.hospitalName || ''}`.toLowerCase().includes(keyword))
})
const equipmentId = ref(null)
const status = ref({ configured: false })
const config = computed(() => status.value.config || {})
const editing = ref(false)
const saving = ref(false)
const busy = ref(false)
const testResult = ref('')
const testOk = ref(false)
const scopeUsers = ref([])
const savingScope = ref(false)
const scopeForm = reactive({ userId: null, equipmentIds: [] })
const importDialog = ref(false)
const importCodes = ref('')
const reviewDialog = ref(false)
const reviewRow = ref(null)
const reviewRemark = ref('')
const reviewConfirmText = ref('')
const historyAction = ref('')
const historyStatus = ref('')
const historyOperator = ref('')
const historyRange = ref([])
const monthlyDialog = ref(false)
const monthly = ref({ summary: {}, records: [] })
const monthlyValue = ref(new Date().toISOString().slice(0, 7))
const filteredHistory = computed(() => (status.value.history || []).filter((row) => {
  const date = String(row.createTime || '').slice(0, 10)
  const inRange = !historyRange.value?.length || (date >= historyRange.value[0] && date <= historyRange.value[1])
  return inRange &&
    (!historyAction.value || row.action === historyAction.value) &&
    (!historyStatus.value || row.status === historyStatus.value) &&
    (!historyOperator.value || String(row.operatorName || '').includes(historyOperator.value))
}))
const form = reactive({
  equipmentId: null, brand: 'SIEMENS_LOGO', protocol: 'SIMULATOR', enabled: true, host: '', port: 502, unitId: 1,
  area: 'HOLDING', startAddress: 0, stopAddress: 1, runStatusAddress: 2, localRemoteAddress: 3,
  interlockAddress: 4, faultAddress: 5, writeValue: 1, timeoutMs: 3000, remark: ''
})
const runText = computed(() => status.value.values?.runStatus === 1 ? '运行' : status.value.values?.runStatus === 0 ? '停止' : '—')
const localText = computed(() => status.value.values?.localRemote === 1 ? '远程' : status.value.values?.localRemote === 0 ? '就地' : '—')
const interlockText = computed(() => status.value.values?.interlockOk === 1 ? '满足' : status.value.values?.interlockOk === 0 ? '不满足' : '—')
const emptyHint = computed(() => {
  if (equipmentContext.value.reason === 'NO_SCOPE') return '公司管理员尚未为您配置可控制设备范围，请联系公司管理员在远程控制页配置'
  if (equipmentContext.value.reason === 'NO_EQUIPMENT') return '本公司暂无正常设备，请先到设备台账新增'
  return '暂无可用设备'
})

async function loadEquipment() {
  const [list, ctx] = await Promise.all([
    request.get('/control/equipment/options'),
    request.get('/control/equipment/context')
  ])
  equipmentOptions.value = list.data || []
  equipmentContext.value = ctx.data || {}
}

async function loadScopeUsers() {
  if (!canScope) return
  scopeUsers.value = (await request.get('/control/scope/users')).data || []
}

async function loadScope() {
  if (!scopeForm.userId) return
  const res = await request.get(`/control/scope/${scopeForm.userId}`)
  scopeForm.equipmentIds = res.data.equipmentIds || []
}

async function saveScope() {
  savingScope.value = true
  try {
    await request.post(`/control/scope/${scopeForm.userId}`, { equipmentIds: scopeForm.equipmentIds })
    ElMessage.success('控制范围已保存')
    await loadScopeUsers()
  } finally {
    savingScope.value = false
  }
}

async function testConnection() {
  const res = await request.get(`/control/test/${equipmentId.value}`)
  const data = res.data || {}
  testOk.value = !!data.ok
  testResult.value = data.ok
    ? `连接成功：${JSON.stringify(data.values || {})}`
    : `连接失败：${data.error || '未知错误'}`
}

async function loadStatus() {
  if (!equipmentId.value) return
  status.value = (await request.get(`/control/status/${equipmentId.value}`)).data || { configured: false }
  editing.value = !status.value.configured
  if (status.value.configured) Object.assign(form, { equipmentId: equipmentId.value, ...status.value.config })
}

function openMonthly() {
  monthlyDialog.value = true
  loadMonthly()
}

async function loadMonthly() {
  if (!monthlyValue.value) return
  monthly.value = (await request.get('/control/audit/monthly', { params: { month: monthlyValue.value } })).data || { summary: {}, records: [] }
}

function exportMonthly() {
  const rows = monthly.value.records || []
  const header = ['时间', '设备', '动作', '状态', '操作人', '复核人', '原因', '结果']
  const lines = [header, ...rows.map((r) => [r.createTime, r.equipmentName, r.action, r.status, r.operatorName, r.reviewBy, r.reason, r.errorMessage])]
  const csv = '\uFEFF' + lines.map((line) => line.map((v) => '"' + String(v ?? '').replace(/"/g, '""') + '"').join(',')).join('\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=UTF-8' }))
  const a = document.createElement('a')
  a.href = url
  a.download = `control-audit-${monthlyValue.value}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

async function saveConfig() {
  saving.value = true
  try {
    form.equipmentId = equipmentId.value
    await request.post('/control/config', { ...form })
    ElMessage.success('控制配置已保存')
    editing.value = false
    await loadStatus()
  } finally {
    saving.value = false
  }
}

async function command(action) {
  const code = equipmentOptions.value.find((e) => e.value === equipmentId.value)?.equipmentCode || ''
  const { value } = await ElMessageBox.prompt(
    `将向 PLC 下发${action === 'START' ? '启动' : '停止'}命令。请输入设备编码确认：${code}`,
    '远程控制确认',
    { type: 'warning', inputPlaceholder: code }
  )
  if (!value) return
  const reason = await ElMessageBox.prompt('请填写操作原因', '操作原因', { inputPlaceholder: '例如：手术室开机前检查' })
  if (!reason.value) return
  busy.value = true
  try {
    const res = await request.post(`/control/command/${equipmentId.value}`, {
      action, reason: reason.value, confirmText: value, requestId: crypto.randomUUID()
    })
    const result = res.data
    if (result.status === 'PENDING_REVIEW') ElMessage.success('命令已提交，等待第二人复核')
    else ElMessage.error(result.errorMessage || '命令状态异常')
    await loadStatus()
  } finally {
    busy.value = false
  }
}

async function importScope() {
  if (!scopeForm.userId || !importCodes.value.trim()) { ElMessage.warning('请选择子管理员并填写设备编码'); return }
  await request.post(`/control/scope/${scopeForm.userId}/import`, { codes: importCodes.value })
  ElMessage.success('设备范围已导入')
  importDialog.value = false
  importCodes.value = ''
  await loadScope()
  await loadScopeUsers()
}

function pretty(value) {
  if (!value) return '暂无快照'
  try { return JSON.stringify(typeof value === 'string' ? JSON.parse(value) : value, null, 2) } catch { return String(value) }
}

function openReview(row) {
  reviewRow.value = row
  reviewRemark.value = ''
  reviewConfirmText.value = ''
  reviewDialog.value = true
}

async function submitReview(pass) {
  if (!pass && !reviewRemark.value.trim()) { ElMessage.warning('驳回时必须填写复核意见'); return }
  await request.post(`/control/command/${reviewRow.value.commandId}/review`, {
    pass, remark: reviewRemark.value, confirmText: pass ? reviewConfirmText.value : ''
  })
  ElMessage.success(pass ? '复核通过，已下发 PLC' : '命令已驳回')
  reviewDialog.value = false
  await loadStatus()
}

onMounted(async () => {
  await loadEquipment()
  await loadScopeUsers()
})
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 16px; }
.head h2 { margin: 0; font-size: 20px; font-weight: 600; }
.head p { margin: 5px 0 0; color: #6b7280; font-size: 12px; }
.card { margin-bottom: 14px; border-radius: 12px; }
.card-head { display: flex; justify-content: space-between; align-items: center; }
.actions { display: flex; gap: 10px; align-items: center; margin-top: 14px; }
.hint { color: #6b7280; font-size: 12px; }
.empty-hint { width: 100%; margin-top: 6px; color: #b45309; font-size: 12px; }
.review-block { margin-top: 12px; }
.history-filters { display: flex; gap: 6px; }
.monthly-toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.snapshot { margin: 6px 0; padding: 10px; max-height: 170px; overflow: auto; background: #0f172a; color: #dbeafe; border-radius: 8px; font-size: 12px; white-space: pre-wrap; }
</style>
