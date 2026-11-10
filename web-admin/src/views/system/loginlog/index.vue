<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title"><div><h2>登录日志</h2><div class="sub">登录行为全程留痕</div></div></div>
      <el-form inline>
        <el-form-item label="账号"><el-input v-model="username" clearable style="width: 180px" @keyup.enter="search" /></el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" @click="search">查询</el-button></el-form-item>
      </el-form>
      <el-table :data="records" size="small">
        <el-table-column type="index" label="序号" width="62" align="center" />
        <el-table-column prop="username" label="账号" width="140" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="browser" label="浏览器" width="110" />
        <el-table-column prop="os" label="操作系统" width="110" />
        <el-table-column label="结果" width="90">
        <template #default="{ row }"><el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">{{ row.status === '0' ? '成功' : '失败' }}</el-tag></template></el-table-column>
        <el-table-column prop="msg" label="信息" min-width="160" />
        <el-table-column prop="loginTime" label="登录时间" width="160" />
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
const username = ref('')
const pageNum = ref(1)
const pageSize = ref(store.pageSize || 10)
async function load() {
  const res = await request.get('/system/log/login', { params: { pageNum: pageNum.value, pageSize: pageSize.value, username: username.value } })
  records.value = res.data.records
  total.value = res.data.total
}
function search() { pageNum.value = 1; load() }
onMounted(load)
</script>

<style scoped>.pager { margin-top: 14px; justify-content: flex-end; }</style>
