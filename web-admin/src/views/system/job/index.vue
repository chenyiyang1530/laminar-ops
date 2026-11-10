<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title"><div><h2>定时任务</h2><div class="sub">采集、任务生成与到期提醒</div></div></div>
      <el-table :data="records" size="small">
        <el-table-column type="index" label="序号" width="62" align="center" />
        
        <el-table-column prop="jobName" label="任务名称" width="180" />
        <el-table-column prop="beanName" label="Bean" width="180" />
        <el-table-column prop="methodName" label="方法" width="120" />
        <el-table-column prop="cron" label="Cron" width="140" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-switch :model-value="row.status === '0'" @change="(v) => toggle(row, v)" /></template>
        </el-table-column>
        <el-table-column prop="lastRunTime" label="最近执行" width="160" />
        <el-table-column prop="lastResult" label="结果" min-width="160" show-overflow-tooltip />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const records = ref([])
async function load() { records.value = (await request.get('/system/job/list')).data }
async function toggle(row, v) {
  await request.put(`/system/job/${row.jobId}/status?status=${v ? '0' : '1'}`)
  row.status = v ? '0' : '1'
  ElMessage.success('状态已更新')
}
onMounted(load)
</script>
