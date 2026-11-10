<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div><h2>统计报表</h2><div class="sub">预算执行、工单与告警经营分析</div></div>
        <el-select v-model="year" style="width: 120px" @change="load">
          <el-option v-for="y in years" :key="y" :label="y + ' 年'" :value="y" />
        </el-select>
      </div>

      <el-row :gutter="14" class="kpis">
        <el-col :xs="12" :sm="6"><div class="kpi"><div class="n">{{ money(data.contractAmount) }}</div><div class="l">在执行合同金额</div></div></el-col>
        <el-col :xs="12" :sm="6"><div class="kpi"><div class="n">{{ data.workorderTotal || 0 }}</div><div class="l">累计工单</div></div></el-col>
        <el-col :xs="12" :sm="6"><div class="kpi"><div class="n">{{ data.alarmTotal || 0 }}</div><div class="l">累计告警</div></div></el-col>
        <el-col :xs="12" :sm="6"><div class="kpi"><div class="n">{{ money(data.budgetTotal?.used) }}</div><div class="l">预算已执行 / 总额 {{ money(data.budgetTotal?.amount) }}</div></div></el-col>
      </el-row>

      <el-row :gutter="14">
        <el-col :xs="24" :lg="12">
          <div class="block">
            <h3>月度工单量</h3>
            <EChart :option="monthlyOption" height="260px" />
          </div>
        </el-col>
        <el-col :xs="24" :lg="12">
          <div class="block">
            <h3>费用构成</h3>
            <el-table :data="data.costByType || []" size="small">
        <el-table-column type="index" label="序号" width="62" align="center" />
              <el-table-column label="类型" width="120">
        <template #default="{ row }">{{ costText(row.costType) }}</template></el-table-column>
              <el-table-column label="金额">
        <template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
            </el-table>
            <div class="status-line">
              <span v-for="s in data.workorderByStatus || []" :key="s.status" class="status-chip">
                {{ statusText(s.status) }} <b>{{ s.count }}</b>
              </span>
            </div>
          </div>
        </el-col>
      </el-row>

      <div class="block" style="margin-top:14px">
        <h3>预算执行明细</h3>
        <el-table :data="data.budgets || []" size="small">
        <el-table-column type="index" label="序号" width="62" align="center" />
          <el-table-column prop="hospitalName" label="医院" min-width="170" />
          <el-table-column prop="budgetName" label="预算名称" min-width="200" show-overflow-tooltip />
          <el-table-column label="科目" width="100">
        <template #default="{ row }">{{ subjectText(row.subject) }}</template></el-table-column>
          <el-table-column label="预算" width="120">
        <template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
          <el-table-column label="已执行" width="120">
        <template #default="{ row }">{{ money(row.usedAmount) }}</template></el-table-column>
          <el-table-column label="执行率" min-width="180">
            <template #default="{ row }">
              <el-progress :percentage="Math.min(100, Number(row.rate || 0))" :status="Number(row.rate) > 100 ? 'exception' : Number(row.rate) > 80 ? 'warning' : ''" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }"><el-button link type="primary" @click="openExec(row)">登记执行</el-button></template>
          </el-table-column>
        </el-table>
      </div>

      <el-dialog v-model="execDialog" title="预算执行登记" width="420px">
        <el-form label-width="80px">
          <el-form-item label="预算">{{ execRow?.budgetName }}</el-form-item>
          <el-form-item label="金额"><el-input-number v-model="execForm.amount" :min="0" :precision="2" style="width: 100%" /></el-form-item>
          <el-form-item label="日期"><el-date-picker v-model="execForm.occurDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          <el-form-item label="备注"><el-input v-model="execForm.remark" /></el-form-item>
        </el-form>
        <template #footer><el-button @click="execDialog = false">取消</el-button><el-button type="primary" @click="submitExec">保存</el-button></template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import EChart from '@/components/EChart.vue'

const year = ref(new Date().getFullYear())
const years = [year.value - 1, year.value, year.value + 1]
const data = ref({})

const monthlyOption = ref({})
const execDialog = ref(false)
const execRow = ref(null)
const execForm = ref({ amount: 0, occurDate: '', remark: '' })

function openExec(row) {
  execRow.value = row
  execForm.value = { amount: 0, occurDate: new Date().toISOString().slice(0, 10), remark: '' }
  execDialog.value = true
}
async function submitExec() {
  if (!execForm.value.amount || execForm.value.amount <= 0) { ElMessage.warning('请输入执行金额'); return }
  await request.post('/budget/execution', { budgetId: execRow.value.budgetId, ...execForm.value })
  ElMessage.success('已登记预算执行')
  execDialog.value = false
  load()
}
const money = (v) => v == null ? '—' : '¥' + Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
const costText = (t) => ({ part: '备件', labor: '人工', travel: '差旅', outsource: '外包', other: '其他' }[t] || t)
const subjectText = (s) => ({ labor: '人工', parts: '备件', travel: '差旅', outsource: '外包', purchase: '采购', other: '其他' }[s] || s)
const statusText = (s) => ({ 0: '待受理', 1: '待派工', 2: '处理中', 3: '待验收', 4: '待回访', 5: '已完成', 6: '已取消' }[s] || s)

async function load() {
  const res = await request.get('/report/summary', { params: { year: year.value } })
  data.value = res.data
  const months = res.data.monthly || []
  monthlyOption.value = {
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 16, top: 20, bottom: 26 },
    xAxis: { type: 'category', data: months.map((m) => m.month), axisLabel: { color: '#7a8ba0' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f7' } } },
    series: [{ type: 'bar', data: months.map((m) => m.count), barWidth: 18, itemStyle: { color: '#0f766e', borderRadius: [6, 6, 0, 0] } }]
  }
}
onMounted(load)
</script>

<style scoped>
.kpis { margin-bottom: 4px; }
.kpi { background: #fff; border: 1px solid #eef2f7; border-radius: 10px; padding: 14px 16px; margin-bottom: 14px; }
.kpi .n { font-size: 21px; font-weight: 700; color: #0f766e; }
.kpi .l { font-size: 11px; color: #8494a8; margin-top: 4px; }
.block { background: #fff; border: 1px solid #eef2f7; border-radius: 10px; padding: 14px 16px; }
.block h3 { margin: 0 0 12px; font-size: 14px; color: #33465c; }
.status-line { margin-top: 12px; display: flex; gap: 8px; flex-wrap: wrap; }
.status-chip { font-size: 11px; color: #5c6f86; background: #f1f5f9; border-radius: 12px; padding: 4px 10px; }
.status-chip b { color: #0f766e; margin-left: 4px; }
</style>
