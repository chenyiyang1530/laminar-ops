<template>
  <div class="control-page">
    <div class="head">
      <div>
        <h2>远程控制</h2>
        <p>先选择 PLC 品牌和协议；未接硬件时使用模拟器验证启动/停止流程。</p>
      </div>
      <el-tag v-if="status.configured" :type="status.online ? 'success' : 'danger'">
        {{ status.online ? '控制器在线' : '控制器离线' }}
      </el-tag>
    </div>

    <el-card shadow="never" class="card">
      <el-form label-width="100px" style="max-width: 720px">
        <el-form-item label="设备">
          <el-select v-model="equipmentId" filterable style="width: 100%" @change="loadStatus">
            <el-option v-for="e in equipmentOptions" :key="e.value" :label="e.label" :value="e.value" />
          </el-select>
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
          <el-button type="success" :disabled="!status.online || busy" @click="command('START')">远程启动</el-button>
          <el-button type="danger" :disabled="!status.online || busy" @click="command('STOP')">远程停止</el-button>
          <span class="hint">启动前会检查就地模式、联锁和故障码；确认文本为设备编码。</span>
        </div>
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
              <el-option label="S7 网关（暂未接入）" value="S7_GATEWAY" />
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
        <template #header><b>控制命令记录</b></template>
        <el-table :data="status.history || []" size="small">
          <el-table-column prop="createTime" label="时间" width="170" />
          <el-table-column prop="action" label="动作" width="80" />
          <el-table-column prop="status" label="结果" width="100" />
          <el-table-column prop="operatorName" label="操作人" width="110" />
          <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
          <el-table-column prop="errorMessage" label="失败原因" min-width="180" show-overflow-tooltip />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const equipmentOptions = ref([])
const equipmentId = ref(null)
const status = ref({ configured: false })
const config = computed(() => status.value.config || {})
const editing = ref(false)
const saving = ref(false)
const busy = ref(false)
const form = reactive({
  equipmentId: null, brand: 'SIEMENS_LOGO', protocol: 'SIMULATOR', enabled: true, host: '', port: 502, unitId: 1,
  area: 'HOLDING', startAddress: 0, stopAddress: 1, runStatusAddress: 2, localRemoteAddress: 3,
  interlockAddress: 4, faultAddress: 5, writeValue: 1, timeoutMs: 3000, remark: ''
})
const runText = computed(() => status.value.values?.runStatus === 1 ? '运行' : status.value.values?.runStatus === 0 ? '停止' : '—')
const localText = computed(() => status.value.values?.localRemote === 1 ? '远程' : status.value.values?.localRemote === 0 ? '就地' : '—')
const interlockText = computed(() => status.value.values?.interlockOk === 1 ? '满足' : status.value.values?.interlockOk === 0 ? '不满足' : '—')

async function loadEquipment() {
  equipmentOptions.value = (await request.get('/control/equipment/options')).data || []
}

async function loadStatus() {
  if (!equipmentId.value) return
  status.value = (await request.get(`/control/status/${equipmentId.value}`)).data || { configured: false }
  editing.value = !status.value.configured
  if (status.value.configured) Object.assign(form, { equipmentId: equipmentId.value, ...status.value.config })
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
    if (result.status === 'SUCCESS') ElMessage.success('命令执行成功')
    else ElMessage.error(result.errorMessage || '命令未确认成功')
    await loadStatus()
  } finally {
    busy.value = false
  }
}

onMounted(loadEquipment)
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 16px; }
.head h2 { margin: 0; font-size: 20px; font-weight: 600; }
.head p { margin: 5px 0 0; color: #6b7280; font-size: 12px; }
.card { margin-bottom: 14px; border-radius: 12px; }
.card-head { display: flex; justify-content: space-between; align-items: center; }
.actions { display: flex; gap: 10px; align-items: center; margin-top: 14px; }
.hint { color: #6b7280; font-size: 12px; }
</style>
