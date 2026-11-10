<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title">
        <div>
          <h2>证书类型审核</h2>
          <div class="sub">公司新增证书时输入的新类型，审核通过后加入通用证书字典</div>
        </div>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-form inline class="search-form">
        <el-form-item label="状态">
          <el-select v-model="status" clearable placeholder="全部" style="width: 150px" @change="load">
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column type="index" label="序号" width="62" align="center" />
        <el-table-column prop="companyName" label="申请公司" min-width="160" />
        <el-table-column prop="certType" label="证书类型" min-width="200" />
        <el-table-column prop="requesterName" label="申请人" width="120" />
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewComment" label="审核意见" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-button link type="success" @click="approve(row)">通过</el-button>
              <el-button link type="danger" @click="reject(row)">驳回</el-button>
            </template>
            <span v-else class="muted">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import request from '@/utils/request'

const rows = ref([])
const loading = ref(false)
const status = ref('pending')

const statusText = (s) => ({ pending: '待审核', approved: '已通过', rejected: '已驳回' }[s] || s)
const statusType = (s) => ({ pending: 'warning', approved: 'success', rejected: 'danger' }[s] || 'info')

async function load() {
  loading.value = true
  try {
    const res = await request.get('/system/cert-type/requests', { params: { status: status.value || undefined } })
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function approve(row) {
  await ElMessageBox.confirm(`确认将「${row.certType}」加入通用证书类型字典？`, '审核通过', { type: 'success' })
  await request.post(`/system/cert-type/requests/${row.requestId}/approve`, { comment: '审核通过' })
  ElMessage.success('已通过并加入字典')
  load()
}

async function reject(row) {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回申请', {
    inputPlaceholder: '例如：与现有证书类型重复'
  })
  await request.post(`/system/cert-type/requests/${row.requestId}/reject`, { comment: value || '' })
  ElMessage.success('已驳回')
  load()
}

onMounted(load)
</script>

<style scoped>
.muted { color: #909399; font-size: 12px; }
</style>
