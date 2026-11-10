<template>
  <div class="app-container">
    <div class="page-card">
      <el-alert
        title="平台标准角色由平台统一维护，不可修改或删除；公司可新增、编辑、删除本公司自定义角色。新建角色默认无菜单权限，保存后请立即配置。"
        type="info"
        :closable="false"
        show-icon
        class="role-tip"
      />
      <div class="page-title">
        <div><h2>角色管理</h2><div class="sub">角色权限、数据范围与菜单授权</div></div>
        <el-button v-if="has('add')" type="primary" :icon="Plus" @click="openAdd">新增角色</el-button>
      </div>
      <el-table :data="records" stripe>
        <el-table-column type="index" label="序号" width="62" align="center" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleKey" label="权限字符" min-width="150" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.roleType === 'standard' ? 'success' : 'info'" size="small">
              {{ row.roleType === 'standard' ? '标准' : '自定义' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属公司" min-width="160">
          <template #default="{ row }">{{ row.companyName || '平台标准' }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="数据范围" width="120">
        <template #default="{ row }">{{ scopeText(row.dataScope) }}</template></el-table-column>
        <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">{{ row.status === '0' ? '正常' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-if="has('edit') && canEditRow(row)" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" v-if="has('edit') && canEditRow(row) && row.roleId !== 1" @click="openMenus(row)">菜单权限</el-button>
            <el-button link type="danger" v-if="has('remove') && canEditRow(row) && row.roleId !== 1 && row.roleType !== 'standard'" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="form.roleId ? '编辑角色' : '新增角色'" width="520px">
      <el-form :model="form" label-width="86px">
        <el-form-item label="角色名称" required><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="权限字符" required><el-input v-model="form.roleKey" :disabled="form.roleType === 'standard'" placeholder="如 hospital_admin" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option label="全部数据" value="all" /><el-option label="本区域" value="region" /><el-option label="本医院" value="hospital" /><el-option label="仅本人" value="self" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio value="0">正常</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="submit">保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="menuDrawer" size="460px" :title="'菜单授权 · ' + (current?.roleName || '')">
      <div class="perm-presets">
        <span class="perm-label">快速预设：</span>
        <el-button size="small" @click="applyPreset('read')">只读</el-button>
        <el-button size="small" @click="applyPreset('edit')">编辑</el-button>
        <el-button size="small" type="warning" plain @click="applyPreset('manage')">全部管理</el-button>
      </div>
      <div class="perm-hint">只读 = 页面查看；编辑 = 查看 + 新增/修改；全部管理 = 所有按钮。预设可再手动取消。</div>
      <el-tree
        ref="treeRef"
        :data="menus"
        node-key="menuId"
        show-checkbox
        check-strictly
        default-expand-all
        :props="{ label: 'menuName', children: 'children' }"
        @check="onCheck"
      />
      <div style="margin-top: 16px"><el-button type="primary" @click="saveMenus">保存授权</el-button></div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const has = (a) => store.hasPerm('system:role:' + a)
const canEditRow = (row) => store.isSuperAdmin || !!row.companyId
const records = ref([])
const dialog = ref(false)
const form = reactive({})
const current = ref(null)
const menuDrawer = ref(false)
const menus = ref([])
const treeRef = ref()
const scopeText = (s) => ({ all: '全部', region: '本区域', hospital: '本医院', self: '仅本人' }[s] || s)

function buildTree(list) {
  const map = {}, roots = []
  list.forEach((m) => (map[m.menuId] = { ...m, children: [] }))
  list.forEach((m) => m.parentId && map[m.parentId] ? map[m.parentId].children.push(map[m.menuId]) : roots.push(map[m.menuId]))
  return roots
}

function flatten(nodes) {
  const out = []
  const walk = (list) => list.forEach((n) => { out.push(n); if (n.children?.length) walk(n.children) })
  walk(nodes)
  return out
}

function collectReadIds(node) {
  const ids = []
  const walk = (n) => {
    if (n.menuType !== 'F') ids.push(n.menuId)
    ;(n.children || []).forEach(walk)
  }
  walk(node)
  return ids
}

function collectAllIds(node) {
  const ids = []
  const walk = (n) => { ids.push(n.menuId); (n.children || []).forEach(walk) }
  walk(node)
  return ids
}

function ancestorIds(id) {
  const parent = {}
  flatten(menus.value).forEach((n) => { parent[n.menuId] = n.parentId })
  const out = []
  let p = parent[id]
  while (p) { out.push(p); p = parent[p] }
  return out
}

function onCheck(data, info) {
  const keys = new Set(treeRef.value.getCheckedKeys())
  const checked = info.checkedKeys.includes(data.menuId)
  if (checked) {
    if (data.menuType === 'M') collectReadIds(data).forEach((id) => keys.add(id))
    else if (data.menuType === 'C') collectReadIds(data).forEach((id) => keys.add(id))
    ancestorIds(data.menuId).forEach((id) => keys.add(id))
  } else {
    collectAllIds(data).forEach((id) => keys.delete(id))
  }
  treeRef.value.setCheckedKeys([...keys])
}

function applyPreset(kind) {
  const all = flatten(menus.value)
  const read = all.filter((n) => n.menuType !== 'F').map((n) => n.menuId)
  let keys = read
  if (kind === 'edit') {
    keys = [...read, ...all.filter((n) => n.menuType === 'F' && /:(add|edit)$/.test(n.perms || '')).map((n) => n.menuId)]
  } else if (kind === 'manage') {
    keys = all.map((n) => n.menuId)
  }
  treeRef.value.setCheckedKeys(keys)
  ElMessage.info(kind === 'read' ? '已选择只读权限' : kind === 'edit' ? '已选择编辑权限' : '已选择全部管理权限')
}
async function load() { records.value = (await request.get('/system/role/list')).data }
function openAdd() { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, { sort: 1, dataScope: 'self', status: '0' }); dialog.value = true }
function openEdit(row) { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, row); dialog.value = true }
async function submit() {
  if (!form.roleName || !form.roleKey) { ElMessage.warning('请填写角色名称与权限字符'); return }
  const isNew = !form.roleId
  const roleKey = String(form.roleKey).trim().toLowerCase()
  if (isNew) await request.post('/system/role', form)
  else await request.put('/system/role', form)
  dialog.value = false
  await load()
  ElMessage.success(isNew ? '角色已创建，请配置菜单权限' : '保存成功')
  if (isNew) {
    const created = records.value.find((r) => r.roleKey === roleKey && r.companyId)
    if (created) await openMenus(created)
  }
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  await request.delete('/system/role/' + row.roleId); ElMessage.success('已删除'); load()
}
async function openMenus(row) {
  current.value = row
  if (!menus.value.length) menus.value = buildTree((await request.get('/system/menu/tree')).data)
  const checked = (await request.get(`/system/role/${row.roleId}/menus`)).data
  menuDrawer.value = true
  setTimeout(() => treeRef.value?.setCheckedKeys(checked), 50)
}
async function saveMenus() {
  const keys = treeRef.value.getCheckedKeys().concat(treeRef.value.getHalfCheckedKeys())
  await request.put(`/system/role/${current.value.roleId}/menus`, keys)
  ElMessage.success('授权已保存'); menuDrawer.value = false
}
onMounted(load)
</script>

<style scoped>
.role-tip { margin-bottom: 12px; }
.perm-presets { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.perm-label { color: #606266; font-size: 13px; }
.perm-hint { color: #909399; font-size: 12px; line-height: 1.5; margin-bottom: 10px; }
</style>
