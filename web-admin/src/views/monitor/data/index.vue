<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title"><div><h2>数据监控</h2><div class="sub">业务表行数与容量（information_schema）</div></div></div>
      <el-input v-model="kw" clearable placeholder="筛选表名" style="width: 240px; margin-bottom: 10px" />
      <el-table :data="filtered" size="small" max-height="640">
        <el-table-column type="index" label="序号" width="62" align="center" />
        
        <el-table-column prop="tableName" label="表名" min-width="240" />
        <el-table-column prop="tableComment" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="tableRows" label="行数" width="110" />
        <el-table-column prop="sizeMb" label="容量(MB)" width="110" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '@/utils/request'

const records = ref([])
const kw = ref('')
const filtered = computed(() => records.value.filter((r) => !kw.value || String(r.tableName).includes(kw.value)))
onMounted(async () => { records.value = (await request.get('/system/monitor/data')).data })
</script>
