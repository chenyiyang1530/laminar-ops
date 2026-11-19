<template>
  <div class="returns-page">
    <div class="head">
      <div>
        <h2>退料记录</h2>
        <p>退料入库审计记录，可按时间倒序查看和导出。</p>
      </div>
      <el-button @click="exportCsv">导出 CSV</el-button>
    </div>
    <el-table :data="records" size="small">
      <el-table-column prop="returnNo" label="退料单号" width="190" />
      <el-table-column prop="reqNo" label="领用单号" width="180" />
      <el-table-column prop="partName" label="备件" min-width="160" />
      <el-table-column prop="quantity" label="数量" width="80" />
      <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
      <el-table-column prop="operatorName" label="操作人" width="110" />
      <el-table-column prop="createTime" label="退料时间" width="170" />
    </el-table>
    <div v-if="!records.length" class="empty">暂无退料记录</div>
    <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" class="pager" @change="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

async function load() {
  const res = await request.get('/parts/returns/page', { params: { pageNum: pageNum.value, pageSize: pageSize.value } })
  records.value = res.data.records || []
  total.value = res.data.total || 0
}

async function exportCsv() {
  try {
    const res = await request.get('/parts/returns/export', { responseType: 'blob' })
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'part-returns.csv'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.warning('导出失败')
  }
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 16px; }
.head h2 { margin: 0; font-size: 20px; font-weight: 600; }
.head p { margin: 5px 0 0; color: #6b7280; font-size: 12px; }
.empty { padding: 20px; text-align: center; color: #9ca3af; font-size: 12px; }
.pager { margin-top: 14px; justify-content: flex-end; }
</style>
