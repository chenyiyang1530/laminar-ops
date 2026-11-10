<template>
  <div class="app-container">
    <div class="page-card">
      <div class="page-title"><div><h2>菜单权限总览</h2><div class="sub">菜单、按钮与权限标识（授权请到角色管理）</div></div></div>
      <el-table :data="tree" row-key="menuId" default-expand-all :tree-props="{ children: 'children' }" size="small">
        <el-table-column type="index" label="序号" width="62" align="center" />
        
        <el-table-column prop="menuName" label="菜单名称" min-width="220" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }"><el-tag size="small" :type="row.menuType === 'M' ? 'warning' : row.menuType === 'C' ? 'primary' : 'info'">{{ typeText(row.menuType) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="path" label="路由" width="200" />
        <el-table-column prop="component" label="组件" width="220" show-overflow-tooltip />
        <el-table-column prop="perms" label="权限标识" width="220" show-overflow-tooltip />
        <el-table-column prop="orderNum" label="排序" width="70" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import request from '@/utils/request'

const tree = ref([])
function build(list) {
  const map = {}, roots = []
  list.forEach((m) => (map[m.menuId] = { ...m, children: [] }))
  list.forEach((m) => m.parentId && map[m.parentId] ? map[m.parentId].children.push(map[m.menuId]) : roots.push(map[m.menuId]))
  return roots
}
const typeText = (t) => ({ M: '目录', C: '菜单', F: '按钮' }[t] || t)
onMounted(async () => { tree.value = build((await request.get('/system/menu/tree')).data) })
</script>
