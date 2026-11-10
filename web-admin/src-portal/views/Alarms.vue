<template>
  <div class="page">
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <div><h2>本院设备告警</h2><div class="sub">仅展示本院洁净区域告警</div></div>
        <el-radio-group v-model="query.status" size="small" @change="search">
          <el-radio-button label="open">活动</el-radio-button>
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="3">已消缺</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="records" size="small" style="margin-top:12px">
        <el-table-column type="index" label="序号" width="62" align="center" />
        <el-table-column prop="alarmNo" label="编号" width="160" />
        <el-table-column label="级别" width="76">
          <template #default="{ row }"><el-tag :type="row.level === '1' ? 'danger' : row.level === '2' ? 'warning' : 'info'" size="small">{{ levelText(row.level) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="equipmentName" label="设备" min-width="150" />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag size="small" :type="row.status === '3' ? 'success' : 'warning'">{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="firstTime" label="发生时间" width="150" />
        <el-table-column label="操作" width="80">
        <template #default="{ row }"><el-button link type="primary" @click="open(row)">查看</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" layout="total, prev, pager, next" class="pager" @change="load" />
    </div>

    <el-drawer v-model="drawer" size="520px" title="告警详情">
      <div v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="编号">{{ detail.alarm.alarm_no }}</el-descriptions-item>
          <el-descriptions-item label="设备">{{ detail.alarm.equipment_name }}</el-descriptions-item>
          <el-descriptions-item label="区域">{{ detail.alarm.area_name }}</el-descriptions-item>
          <el-descriptions-item label="内容">{{ detail.alarm.content }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(detail.alarm.status) }}</el-descriptions-item>
          <el-descriptions-item label="原因">{{ detail.alarm.root_cause || '—' }}</el-descriptions-item>
          <el-descriptions-item label="措施">{{ detail.alarm.solution || '—' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin:18px 0 10px">处理时间线</h4>
        <el-timeline>
          <el-timeline-item v-for="(l, i) in detail.logs" :key="i" :timestamp="l.createTime">
            <div style="font-size:13px">{{ l.operatorName }} · {{ l.content }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import request from '@/request'

const records = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, status: 'open' })
const drawer = ref(false)
const detail = ref(null)
const levelText = (l) => ({ 1: '紧急', 2: '重要', 3: '一般', 4: '提示' }[l] || '一般')
const statusText = (s) => ({ 0: '未确认', 1: '已确认', 2: '处理中', 3: '已消缺', 4: '已忽略' }[s] || s)

async function load() {
  const res = await request.get('/portal/alarms', { params: query })
  records.value = res.data.records
  total.value = res.data.total
}
function search() { query.pageNum = 1; load() }
async function open(row) { drawer.value = true; detail.value = (await request.get('/portal/alarms/' + row.alarmId)).data }
onMounted(load)
</script>
