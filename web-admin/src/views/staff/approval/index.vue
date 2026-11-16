<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div>
          <h2>审批中心</h2>
          <div class="sub">待我审批与已处理记录</div>
        </div>
        <el-button :icon="Refresh" @click="loadTab">刷新</el-button>
      </div>

      <el-tabs v-model="tab" @tab-change="loadTab">
        <el-tab-pane label="待我审批" name="pending">
          <el-table v-loading="loading" :data="pendingList" stripe>
            <el-table-column prop="engineerName" label="工程师" width="120" />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">{{ typeText(row.leaveType) }}</template>
            </el-table-column>
            <el-table-column prop="startDate" label="开始日期" width="120" />
            <el-table-column prop="endDate" label="结束日期" width="120" />
            <el-table-column prop="days" label="天数" width="70" />
            <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
            <el-table-column label="当前步骤" width="130">
              <template #default="{ row }">第 {{ row.stepNo }} 级审批</template>
            </el-table-column>
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" @click="act(row, true)">通过</el-button>
                <el-button link type="danger" @click="act(row, false)">驳回</el-button>
                <el-button link type="primary" @click="openTransfer(row)">转交</el-button>
                <el-button link @click="openFlow(row)">流程</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="审批历史" name="history">
          <el-table v-loading="loading" :data="historyList" stripe>
            <el-table-column prop="engineerName" label="工程师" width="120" />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">{{ typeText(row.leaveType) }}</template>
            </el-table-column>
            <el-table-column prop="startDate" label="开始日期" width="120" />
            <el-table-column prop="endDate" label="结束日期" width="120" />
            <el-table-column prop="days" label="天数" width="70" />
            <el-table-column label="结果" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'APPROVED' ? 'success' : 'danger'" size="small">
                  {{ row.status === 'APPROVED' ? '通过' : '驳回' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="comment" label="意见" min-width="180" show-overflow-tooltip />
            <el-table-column prop="actedAt" label="处理时间" width="170" />
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button link @click="openFlow(row)">流程</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane v-if="canViewCompany" label="公司审批记录" name="company">
          <el-table v-loading="loading" :data="companyList" stripe>
            <el-table-column prop="engineerName" label="申请人" min-width="100" />
            <el-table-column label="类型" min-width="70">
              <template #default="{ row }">{{ typeText(row.leaveType) }}</template>
            </el-table-column>
            <el-table-column prop="startDate" label="开始日期" min-width="100" />
            <el-table-column prop="endDate" label="结束日期" min-width="100" />
            <el-table-column prop="days" label="天数" min-width="55" />
            <el-table-column label="状态" min-width="85">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="当前进度" min-width="80">
              <template #default="{ row }">{{ row.currentStep ? `第 ${row.currentStep} 级` : '—' }}</template>
            </el-table-column>
            <el-table-column prop="approvalTrail" label="审批轨迹" min-width="170" show-overflow-tooltip />
            <el-table-column prop="currentApproverName" label="当前审批人" min-width="95" />
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.currentTaskId" link type="warning" @click="urge(row)">督办</el-button>
                <el-button v-if="row.currentTaskId" link type="primary" @click="openCompanyTransfer(row)">转办</el-button>
                <el-button link @click="openFlow(row)">流程</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="transferVisible" title="转交审批" width="460px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="转交给" required>
          <el-select v-model="transferTarget" filterable style="width: 100%" placeholder="选择审批人">
            <el-option v-for="u in transferUsers" :key="u.value" :label="u.label" :value="u.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTransfer">确认转交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="flowVisible" title="审批流转" width="580px">
      <template v-if="flow.leave">
        <div class="flow-head">
          <strong>{{ flow.leave.engineerName }}</strong>
          <span>{{ typeText(flow.leave.leaveType) }} · {{ flow.leave.startDate }} 至 {{ flow.leave.endDate }} · {{ flow.leave.days }} 天</span>
        </div>
        <el-timeline class="flow-line">
          <el-timeline-item
            v-for="step in flow.tasks"
            :key="step.stepNo"
            :timestamp="step.actedAt || step.createTime || ''"
            :type="step.status === 'APPROVED' ? 'success' : step.status === 'REJECTED' ? 'danger' : 'primary'"
          >
            <div class="flow-step">
              <span>第 {{ step.stepNo }} 级审批</span>
              <el-tag size="small" :type="step.status === 'APPROVED' ? 'success' : step.status === 'REJECTED' ? 'danger' : 'warning'">
                {{ step.status === 'APPROVED' ? '已通过' : step.status === 'REJECTED' ? '已驳回' : '待审批' }}
              </el-tag>
            </div>
            <div class="flow-approver">审批人：{{ step.approverName || '—' }}</div>
            <div v-if="step.comment" class="flow-comment">意见：{{ step.comment }}</div>
          </el-timeline-item>
        </el-timeline>
        <template v-if="flow.logs && flow.logs.length">
          <div class="flow-log-head">操作记录</div>
          <el-timeline class="flow-line">
            <el-timeline-item
              v-for="(log, i) in flow.logs"
              :key="i"
              :timestamp="log.createTime || ''"
              :type="log.action === 'REJECT' ? 'danger' : log.action === 'TRANSFER' ? 'warning' : 'success'"
            >
              <div class="flow-step">
                <span>{{ actionText(log.action) }}</span>
              </div>
              <div class="flow-approver">
                {{ log.action === 'TRANSFER'
                  ? `${log.fromName || '—'} 将审批转交给 ${log.toName || '—'}`
                  : `操作人：${log.fromName || '—'}` }}
              </div>
              <div v-if="log.comment" class="flow-comment">意见：{{ log.comment }}</div>
            </el-timeline-item>
          </el-timeline>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const canViewCompany = computed(() => store.roles.includes('company_executive') || store.roles.includes('maintenance_manager'))
const tab = ref('pending')
const loading = ref(false)
const pendingList = ref([])
const historyList = ref([])
const companyList = ref([])
const flowVisible = ref(false)
const flow = ref({ leave: null, tasks: [], logs: [] })
const transferVisible = ref(false)
const transferTarget = ref()
const transferUsers = ref([])
const transferTask = ref(null)

const typeText = (v) => ({
  annual: '年假',
  sick: '病假',
  personal: '事假',
  training: '培训',
  temporary: '临时离岗'
}[v] || v)

const statusText = (v) => ({
  PENDING_REGION: '审批中',
  PENDING_COMPANY: '审批中',
  PENDING_HANDOVER: '待交接',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  CANCELLED: '已取消'
}[v] || v)
const statusType = (v) => ({ APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info', PENDING_HANDOVER: 'warning' }[v] || 'warning')

async function loadTab() {
  loading.value = true
  try {
    if (tab.value === 'pending') {
      pendingList.value = (await request.get('/approval/pending')).data || []
    } else if (tab.value === 'history') {
      historyList.value = (await request.get('/approval/history')).data || []
    } else {
      companyList.value = (await request.get('/approval/company-records')).data || []
    }
  } finally {
    loading.value = false
  }
}

async function act(row, approved) {
  const { value } = await ElMessageBox.prompt(
    approved ? '审批意见（可选）' : '请填写驳回原因',
    approved ? '通过' : '驳回',
    { inputType: 'textarea', inputPlaceholder: approved ? '同意' : '请填写原因' }
  )
  await request.post(`/approval/${row.taskId}/act`, { approved, comment: value || '' })
  ElMessage.success(approved ? '已通过' : '已驳回')
  await loadTab()
}

async function openFlow(row) {
  flow.value = { leave: null, tasks: [], logs: [] }
  flowVisible.value = true
  const res = await request.get(`/approval/leave/${row.leaveId}/flow`)
  flow.value = res.data
}

const actionText = (action) => ({ APPROVE: '审批通过', REJECT: '审批驳回', TRANSFER: '转交审批' }[action] || action)

async function openTransfer(row) {
  transferTask.value = row
  transferTarget.value = undefined
  transferUsers.value = (await request.get('/approval/transferOptions')).data || []
  transferVisible.value = true
}

function openCompanyTransfer(row) {
  openTransfer({ ...row, taskId: row.currentTaskId })
}

async function urge(row) {
  await ElMessageBox.confirm('确认督办这条审批？系统会通知当前审批人。', '审批督办', { type: 'warning' })
  await request.post(`/approval/${row.currentTaskId}/urge`)
  ElMessage.success('已督办')
}

async function submitTransfer() {
  if (!transferTarget.value) { ElMessage.warning('请选择审批人'); return }
  await request.post(`/approval/${transferTask.value.taskId}/transfer`, { targetUserId: transferTarget.value })
  ElMessage.success('已转交')
  transferVisible.value = false
  await loadTab()
}

onMounted(loadTab)
</script>

<style scoped>
.flow-head { display: flex; flex-direction: column; gap: 4px; margin-bottom: 16px; }
.flow-head span { color: #8a99ab; font-size: 13px; }
.flow-step { display: flex; justify-content: space-between; gap: 12px; font-weight: 600; }
.flow-approver { margin-top: 4px; color: #5d6b7a; font-size: 13px; }
.flow-log-head { margin: 18px 0 10px; padding-top: 14px; border-top: 1px solid #eef2f7; font-weight: 600; color: #1f2d3d; }
.flow-comment { margin-top: 4px; color: #8a99ab; font-size: 13px; }
</style>
