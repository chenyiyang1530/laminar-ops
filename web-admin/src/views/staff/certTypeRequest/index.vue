<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div>
          <h2>证书类型申请</h2>
          <div class="sub">新增证书时输入的新类型会提交审核，驳回后可按原因修改并重新提交</div>
        </div>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column type="index" label="序号" width="62" align="center" />
        <el-table-column prop="certType" label="证书类型" min-width="220" />
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewComment" label="审核意见" min-width="240" show-overflow-tooltip />
        <el-table-column prop="reviewTime" label="处理时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'rejected'" link type="primary" @click="resubmit(row)">重新提交</el-button>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'

const rows = ref([])
const loading = ref(false)
const statusText = (s) => ({ pending: '待审核', approved: '已通过', rejected: '已驳回' }[s] || s)
const statusType = (s) => ({ pending: 'warning', approved: 'success', rejected: 'danger' }[s] || 'info')

async function load() {
  loading.value = true
  try {
    const res = await request.get('/system/cert-type/my')
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function resubmit(row) {
  await ElMessageBox.confirm(`确认重新提交「${row.certType}」的审核申请？`, '重新提交', { type: 'warning' })
  await request.post(`/system/cert-type/requests/${row.requestId}/resubmit`)
  ElMessage.success('已重新提交审核')
  load()
}

onMounted(load)
</script>

<style scoped>
.muted { color: #909399; font-size: 12px; }
</style>
