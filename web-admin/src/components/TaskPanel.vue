<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div>
          <h2>{{ isInspection ? '巡检任务' : '保养任务' }}</h2>
          <div class="sub">{{ isInspection ? '周期巡检执行、逾期提醒、异常转工单' : '预防性保养执行、记录归档、异常转工单' }}</div>
        </div>
        <el-button v-if="canPlanEdit" type="primary" plain :icon="Refresh" @click="generate">生成到期任务</el-button>
      </div>

      <el-radio-group v-model="query.status" size="small" class="tabs" @change="search">
        <el-radio-button label="todo">待执行/逾期</el-radio-button>
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="0">待执行</el-radio-button>
        <el-radio-button label="1">执行中</el-radio-button>
        <el-radio-button label="2">已完成</el-radio-button>
        <el-radio-button label="3">已逾期</el-radio-button>
      </el-radio-group>

      <el-form inline class="search-form">
        <el-form-item label="医院">
          <el-select v-model="query.hospitalId" clearable placeholder="全部" style="width: 200px" @change="search">
            <el-option v-for="h in hospitals" :key="h.value" :label="h.label" :value="h.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" clearable placeholder="任务编号" style="width: 180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column type="index" label="序号" width="62" align="center" />
        
        <el-table-column prop="taskNo" label="任务编号" width="160" />
        <el-table-column prop="planName" label="计划" min-width="180" show-overflow-tooltip />
        <el-table-column :prop="isInspection ? 'planTime' : 'planDate'" label="计划日期" width="120" />
        <el-table-column prop="hospitalName" label="医院" min-width="160" show-overflow-tooltip />
        <el-table-column v-if="isInspection" prop="departmentName" label="科室" min-width="150" show-overflow-tooltip />
        <el-table-column prop="equipmentName" label="设备" min-width="150" show-overflow-tooltip />
        <el-table-column prop="assigneeName" label="执行人" width="90" v-if="isInspection" />
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="86">
          <template #default="{ row }">
            <el-tag v-if="row.result !== null" :type="row.result === '1' ? 'success' : 'danger'" size="small">
              {{ row.result === '1' ? '正常' : '异常' }}
            </el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button v-if="['0','1','3'].includes(row.status) && canExecuteRow(row)" link type="primary" @click="openExecute(row)">
              {{ row.status === '1' ? '继续执行' : '执行' }}
            </el-button>
            <el-button link @click="openExecute(row, true)">查看</el-button>
            <el-button v-if="row.workorderId" link type="warning" @click="$router.push('/workorder/list')">工单</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :layout="'total, sizes, prev, pager, next'"
        :page-sizes="[10, 20, 50, 100]"
        class="pager"
        @change="load"
      />
    </div>

    <el-dialog v-model="execDialog" :title="(isInspection ? '巡检执行' : '保养执行') + ' · ' + (task?.task_no || '')" width="820px" destroy-on-close>
      <div v-if="task" class="exec">
        <el-descriptions :column="3" size="small" border>
          <el-descriptions-item label="计划">{{ task.plan_name || '—' }}</el-descriptions-item>
          <el-descriptions-item label="医院">{{ task.hospital_name }}</el-descriptions-item>
          <el-descriptions-item label="设备">{{ task.equipment_name || '—' }}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="items" size="small" class="items">
        <el-table-column type="index" label="序号" width="62" align="center" />
        
          <el-table-column prop="itemName" :label="isInspection ? '检查项' : '保养项'" min-width="220" />
          <el-table-column label="结果" width="150">
            <template #default="{ row }">
              <el-radio-group v-model="row.result" size="small" :disabled="readonly">
                <el-radio value="1">正常</el-radio>
                <el-radio value="0">异常</el-radio>
              </el-radio-group>
            </template>
          </el-table-column>
          <el-table-column label="实测/记录" width="180">
            <template #default="{ row }"><el-input v-model="row.itemValue" size="small" :disabled="readonly" /></template>
          </el-table-column>
          <el-table-column label="备注" width="160">
            <template #default="{ row }"><el-input v-model="row.remark" size="small" :disabled="readonly" /></template>
          </el-table-column>
        </el-table>

        <el-form :model="form" label-width="96px" class="exec-form">
          <el-form-item label="整体结果">
            <el-radio-group v-model="form.result" :disabled="readonly">
              <el-radio value="1">正常</el-radio>
              <el-radio value="0">异常</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="isInspection" label="问题描述">
            <el-input v-model="form.problemDesc" type="textarea" :rows="2" :disabled="readonly" placeholder="结果异常时必填" />
          </el-form-item>
          <el-form-item v-else label="保养记录">
            <el-input v-model="form.content" type="textarea" :rows="3" :disabled="readonly" placeholder="记录保养过程、更换备件等" />
          </el-form-item>
          <el-form-item v-if="!isInspection" label="使用备件">
            <el-input v-model="form.partsUsed" :disabled="readonly" placeholder="如：G4 初效过滤器 4 只" />
          </el-form-item>
          <el-form-item v-if="!readonly && form.result === '0'" label="转工单">
            <el-switch v-model="form.createWorkorder" />
            <span class="hint">结果异常时自动生成维修工单</span>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="execDialog = false">{{ readonly ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!readonly" type="primary" :loading="saving" @click="submit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const props = defineProps({ type: { type: String, default: 'inspection' } })
const store = useUserStore()
const isInspection = computed(() => props.type === 'inspection')
const loading = ref(false)
const saving = ref(false)
const records = ref([])
const total = ref(0)
const hospitals = ref([])
const query = reactive({ pageNum: 1, pageSize: store.pageSize || 10, status: 'todo', hospitalId: undefined, keyword: undefined })
const execDialog = ref(false)
const readonly = ref(false)
const task = ref(null)
const items = ref([])
const form = reactive({ result: '1', problemDesc: '', content: '', partsUsed: '', createWorkorder: true, score: null })

const canExecute = computed(() => store.hasPerm(`${props.type}:task:execute`))
const canPlanEdit = computed(() => store.hasPerm(`${props.type}:plan:edit`))
function canExecuteRow(row) {
  if (!canExecute.value) return false
  if (store.isSuperAdmin) return true
  if (row.assigneeId === null || row.assigneeId === undefined) return true
  return store.engineerId !== null && store.engineerId !== undefined
    && String(row.assigneeId) === String(store.engineerId)
}

function statusText(s) { return { 0: '待执行', 1: '执行中', 2: '已完成', 3: '已逾期', 4: '已取消' }[s] || s }
function statusType(s) { return { 0: 'warning', 1: 'primary', 2: 'success', 3: 'danger', 4: 'info' }[s] || 'info' }

async function load() {
  loading.value = true
  try {
    const res = await request.get(`/${props.type}/task/page`, { params: query })
    records.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
function search() { query.pageNum = 1; load() }
function reset() {
  Object.assign(query, { pageNum: 1, status: 'todo', hospitalId: undefined, keyword: undefined })
  load()
}

function openExecute(row, viewOnly = false) {
  readonly.value = viewOnly
  execDialog.value = true
  task.value = null
  request.get(`/${props.type}/task/${row.taskId}`).then((res) => {
    task.value = res.data.task
    items.value = (res.data.items || []).map((i) => ({ ...i, result: i.result ?? '1', itemValue: i.itemValue ?? '', remark: i.remark ?? '' }))
    Object.assign(form, {
      result: res.data.task.result ?? '1',
      problemDesc: res.data.task.problem_desc ?? '',
      content: res.data.task.content ?? '',
      partsUsed: res.data.task.parts_used ?? '',
      createWorkorder: true,
      score: res.data.task.score ?? null
    })
  })
}

async function submit() {
  if (!isInspection.value && !form.content.trim()) { ElMessage.warning('请填写保养记录'); return }
  if (isInspection.value && form.result === '0' && !form.problemDesc.trim()) { ElMessage.warning('请填写问题描述'); return }
  saving.value = true
  try {
    const payload = { ...form, items: items.value }
    const res = await request.post(`/${props.type}/task/${task.value.task_id}/complete`, payload)
    ElMessage.success(res.data ? '任务完成，已生成工单 #' + res.data : '任务已完成')
    execDialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function generate() {
  const res = await request.post(`/${props.type}/generate`)
  ElMessage.success('已生成 ' + res.data + ' 个到期任务')
  load()
}

onMounted(async () => {
  const h = await request.get('/crud/hospital/options')
  hospitals.value = h.data
  load()
})
</script>

<style scoped>
.tabs { margin-bottom: 12px; }
.search-form { margin-bottom: 4px; }
.pager { margin-top: 14px; justify-content: flex-end; }
.items { margin-top: 14px; }
.exec-form { margin-top: 16px; }
.hint { margin-left: 10px; color: #94a3b8; font-size: 12px; }
</style>
