<template>
  <div class="page">
    <div class="row">
      <div class="kpi"><div class="num">{{ stats.equipmentCount || 0 }}</div><div class="label">在册设备</div></div>
      <div class="kpi"><div class="num">{{ stats.onlineRate || 0 }}%</div><div class="label">设备在线率</div></div>
      <div class="kpi"><div class="num" style="color:#dc2626">{{ stats.alarmUnhandled || 0 }}</div><div class="label">未处理告警</div></div>
      <div class="kpi"><div class="num">{{ stats.workorderPending || 0 }}</div><div class="label">待受理/派工工单</div></div>
    </div>

    <div class="card" style="margin-top:14px">
      <div style="display:flex;justify-content:space-between;align-items:center">
        <div><h2>最新告警</h2><div class="sub">本院洁净设备运行告警</div></div>
        <el-button text type="primary" @click="$router.push('/alarms')">查看全部</el-button>
      </div>
      <el-table :data="alarms" size="small" style="margin-top:10px">
        <el-table-column type="index" label="序号" width="62" align="center" />
        
        <el-table-column label="级别" width="80">
          <template #default="{ row }"><el-tag :type="row.level === '1' ? 'danger' : row.level === '2' ? 'warning' : 'info'" size="small">{{ levelText(row.level) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="equipmentName" label="设备" min-width="150" />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="firstTime" label="时间" width="150" />
      </el-table>
    </div>

    <div class="card" style="margin-top:14px">
      <h2>合同到期提醒</h2>
      <div class="sub">本院维保合同 120 天内到期</div>
      <div v-if="!contracts.length" style="color:#94a3b8;padding:16px 0;font-size:12px">暂无即将到期合同</div>
      <div v-for="c in contracts" :key="c.contractId" style="display:flex;justify-content:space-between;padding:10px 0;border-bottom:1px dashed #eef2f7">
        <div><div style="font-size:13px">{{ c.contractName }}</div><div class="sub">到期 {{ c.endDate }}</div></div>
        <el-tag :type="c.days <= 30 ? 'danger' : 'warning'" size="small">剩 {{ c.days }} 天</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '@/request'

const data = ref({})
const stats = computed(() => data.value.stats || {})
const alarms = computed(() => (data.value.recentAlarms || []).slice(0, 5))
const contracts = computed(() => data.value.expiringContracts || [])
function levelText(l) { return { 1: '紧急', 2: '重要', 3: '一般', 4: '提示' }[l] || '一般' }
onMounted(async () => { data.value = (await request.get('/portal/dashboard')).data })
</script>
