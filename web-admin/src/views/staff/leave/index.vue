<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div>
          <h2>请假申请</h2>
          <div class="sub">工程师提交，区域负责人审批；超过 3 天由总区域负责人终审</div>
        </div>
        <el-button v-if="store.engineerId" type="primary" :icon="Plus" :disabled="!applyReady" @click="openApply">提交请假</el-button>
      </div>

      <el-alert
        v-if="!store.engineerId"
        title="当前账号未绑定工程师，不能提交请假"
        type="warning"
        :closable="false"
        show-icon
        class="leave-tip"
      />

      <el-alert
        v-if="store.engineerId && !applyReady"
        :title="applyReason"
        type="warning"
        :closable="false"
        show-icon
        class="leave-tip"
      />

      <el-alert
        title="请假提交后会固化审批链；提交后调整区域或负责人，不会改变已提交申请的审批人。"
        type="info"
        :closable="false"
        show-icon
        class="leave-tip"
      />

      <el-table v-loading="loading" :data="myList" stripe>
        <el-table-column prop="leaveType" label="类型" width="100">
          <template #default="{ row }">{{ typeText(row.leaveType) }}</template>
        </el-table-column>
        <el-table-column label="开始日期" width="145">
          <template #default="{ row }">{{ row.startDate }} {{ periodText(row.startPeriod) }}</template>
        </el-table-column>
        <el-table-column label="结束日期" width="145">
          <template #default="{ row }">{{ row.endDate }} {{ periodText(row.endPeriod) }}</template>
        </el-table-column>
        <el-table-column prop="days" label="天数" width="70" />
        <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前步骤" width="130">
          <template #default="{ row }">{{ stepText(row.status, row.currentStep) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="['PENDING_REGION','PENDING_COMPANY'].includes(row.status)"
              link
              type="danger"
              @click="cancelLeave(row)"
            >取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="applyVisible" title="提交请假" width="560px" destroy-on-close>
      <el-form :model="form" label-width="86px">
        <el-form-item label="请假类型">
          <el-select v-model="form.leaveType" style="width: 100%">
            <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" required>
          <div class="date-period">
            <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" :disabled-date="disabledDate" style="flex: 1" />
            <el-select v-model="form.startPeriod" style="width: 86px">
              <el-option label="上午" value="AM" />
              <el-option label="下午" value="PM" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="结束日期" required>
          <div class="date-period">
            <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" :disabled-date="disabledDate" style="flex: 1" />
            <el-select v-model="form.endPeriod" style="width: 86px">
              <el-option label="上午" value="AM" />
              <el-option label="下午" value="PM" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="请假天数">
          <span>{{ days }} 天</span>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const tab = ref('my')
const loading = ref(false)
const saving = ref(false)
const applyVisible = ref(false)
const myList = ref([])
const pendingList = ref([])
const applyReady = ref(true)
const applyReason = ref('')
const form = reactive({ leaveType: 'personal', startDate: '', endDate: '', startPeriod: 'AM', endPeriod: 'PM', reason: '' })

const typeOptions = [
  { value: 'annual', label: '年假' },
  { value: 'sick', label: '病假' },
  { value: 'personal', label: '事假' },
  { value: 'training', label: '培训' },
  { value: 'temporary', label: '临时离岗' }
]
const typeText = (v) => typeOptions.find((t) => t.value === v)?.label || v
const statusText = (v) => ({
  PENDING_REGION: '待区域审批',
  PENDING_COMPANY: '待总区域审批',
  PENDING_HANDOVER: '待交接',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  CANCELLED: '已取消'
}[v] || v)
const statusType = (v) => ({ APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }[v] || 'warning')
const stepText = (status, step) => {
  if (status === 'PENDING_REGION') return `第 ${step || 1} 级审批`
  if (status === 'PENDING_COMPANY') return `第 ${step || 2} 级审批`
  if (status === 'PENDING_HANDOVER') return '待改派在手单'
  if (status === 'APPROVED') return '审批完成'
  return '—'
}
const periodText = (period) => period === 'AM' ? '上午' : period === 'PM' ? '下午' : ''
const days = computed(() => {
  if (!form.startDate || !form.endDate) return 0
  const a = new Date(form.startDate)
  const b = new Date(form.endDate)
  if (Number.isNaN(a.getTime()) || Number.isNaN(b.getTime()) || b < a) return 0
  let total = Math.round((b - a) / 86400000) + 1
  if (form.startPeriod === 'PM') total -= 0.5
  if (form.endPeriod === 'AM') total -= 0.5
  return Math.max(0, total)
})

const disabledDate = (date) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

async function loadMy() {
  myList.value = (await request.get('/leave/my')).data || []
}

async function loadApplyStatus() {
  if (!store.engineerId) return
  try {
    const res = await request.get('/engineer/me/status')
    applyReady.value = !!res.data?.canApplyLeave
    applyReason.value = res.data?.regionId == null
      ? '所属区域未配置，无法确定审批人，请先完善工程师档案'
      : (res.data?.managerId == null
        ? '所属区域未配置区域负责人，暂不能提交请假'
        : '所属区域负责人不可用，暂不能提交请假')
  } catch (e) {
    applyReady.value = false
  }
}

async function loadPending() {
  pendingList.value = (await request.get('/leave/pending')).data || []
}

async function loadTab() {
  loading.value = true
  try {
    if (tab.value === 'my') await loadMy()
    else await loadPending()
  } finally {
    loading.value = false
  }
}

function openApply() {
  Object.assign(form, { leaveType: 'personal', startDate: '', endDate: '', startPeriod: 'AM', endPeriod: 'PM', reason: '' })
  applyVisible.value = true
}

async function submit() {
  if (!form.startDate || !form.endDate) {
    ElMessage.warning('请选择请假日期')
    return
  }
  if (days.value <= 0) {
    ElMessage.warning('请假时段不正确，结束时间不能早于开始时间')
    return
  }
  saving.value = true
  try {
    await request.post('/leave/apply', form)
    ElMessage.success('请假申请已提交')
    applyVisible.value = false
    tab.value = 'my'
    await loadMy()
  } finally {
    saving.value = false
  }
}

async function approve(row, pass) {
  const { value } = await ElMessageBox.prompt(pass ? '审批意见（可选）' : '请填写驳回原因', pass ? '通过' : '驳回', {
    inputType: 'textarea',
    inputPlaceholder: pass ? '同意' : '请填写原因'
  })
  await request.post(`/leave/${row.leaveId}/${pass ? 'approve' : 'reject'}`, { comment: value || '' })
  ElMessage.success(pass ? '已通过' : '已驳回')
  await loadPending()
}

async function cancelLeave(row) {
  await ElMessageBox.confirm('确认取消这条请假申请？', '提示', { type: 'warning' })
  await request.post(`/leave/${row.leaveId}/cancel`)
  ElMessage.success('已取消')
  await loadMy()
}

watch(tab, loadTab)
onMounted(() => {
  loadApplyStatus()
  loadTab()
})
</script>

<style scoped>
.date-period { display: flex; align-items: center; gap: 8px; width: 100%; }
.leave-tip { margin-bottom: 12px; }
</style>
