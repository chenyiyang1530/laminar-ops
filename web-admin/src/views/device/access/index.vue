<template>
  <div class="access-page">
    <div class="access-head">
      <div>
        <h2>设备接入</h2>
        <p>管理现场网关、Token 与上报通道。设备与点位请在“设备台账”“监测点位”中维护。</p>
      </div>
      <div class="head-actions">
        <el-button @click="testVisible = true">测试上报</el-button>
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
            <el-button link type="primary" v-if="canEdit" @click="rotate(row)">轮换 Token</el-button>
            <el-button link type="danger" v-if="canEdit" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !gateways.length" class="empty">
        还没有接入网关。新增网关后，把 Token 配置到现场网关即可开始上报。
      </div>
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

    <el-dialog v-model="testVisible" title="测试上报" width="560px">
      <el-form :model="testForm" label-width="92px">
        <el-form-item label="设备编码"><el-input v-model="testForm.equipmentCode" placeholder="例如 AHU-04" /></el-form-item>
        <el-form-item label="点位编码"><el-input v-model="testForm.pointCode" placeholder="例如 AHU-04-TEMP" /></el-form-item>
        <el-form-item label="测试值"><el-input v-model="testForm.value" placeholder="例如 23.6" /></el-form-item>
      </el-form>
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
import { onMounted, reactive, ref } from 'vue'
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
const testForm = reactive({ equipmentCode: '', pointCode: '', value: '' })

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

async function remove(row) {
  await ElMessageBox.confirm(`确认删除网关「${row.gatewayName}」？设备会解除绑定。`, '提示', { type: 'warning' })
  await request.delete(`/monitor/access/gateways/${row.gatewayId}`)
  ElMessage.success('已删除')
  await load()
}

async function runTest() {
  if (!testForm.equipmentCode || !testForm.pointCode || !testForm.value) {
    ElMessage.warning('请填写设备编码、点位编码和测试值')
    return
  }
  testing.value = true
  testResult.value = ''
  try {
    const res = await request.post('/monitor/access/test', testForm)
    testResult.value = `上报成功：写入 ${res.data.count} 个点位，当前值 ${res.data.value}`
  } catch (e) {
    testResult.value = '上报失败，请检查设备编码、点位编码和模拟开关'
  } finally {
    testing.value = false
  }
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

onMounted(load)
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
</style>
