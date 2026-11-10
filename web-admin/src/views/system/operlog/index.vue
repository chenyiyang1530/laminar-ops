<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title"><div><h2>操作审计</h2><div class="sub">业务写操作全程留痕</div></div></div>
      <el-form inline>
        <el-form-item label="关键字"><el-input v-model="keyword" clearable placeholder="接口 / 操作人" style="width: 220px" @keyup.enter="search" /></el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" @click="search">查询</el-button></el-form-item>
      </el-form>
      <el-table :data="records" size="small">
        <el-table-column type="index" label="序号" width="62" align="center" />
        <el-table-column prop="title" label="请求地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="方法" width="80" />
        <el-table-column prop="operName" label="操作人" width="110" />
        <el-table-column prop="operIp" label="IP" width="130" />
        <el-table-column label="结果" width="80">
        <template #default="{ row }"><el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">{{ row.status === '0' ? '成功' : '失败' }}</el-tag></template></el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="90" />
        <el-table-column prop="operTime" label="时间" width="160" />
      </el-table>
      <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" class="pager" @change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const records = ref([])
const total = ref(0)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(store.pageSize || 10)
async function load() {
  const res = await request.get('/system/log/oper', { params: { pageNum: pageNum.value, pageSize: pageSize.value, title: keyword.value } })
  records.value = res.data.records
  total.value = res.data.total
}
function search() { pageNum.value = 1; load() }
onMounted(load)
</script>
<style scoped>.pager { margin-top: 14px; justify-content: flex-end; }</style>
