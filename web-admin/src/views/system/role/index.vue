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
        <div class="role-actions">
          <el-select v-model="roleSort" style="width: 150px" @change="syncFilterUrl">
            <el-option label="等级高 → 低" value="rankDesc" />
            <el-option label="等级低 → 高" value="rankAsc" />
            <el-option label="默认排序" value="default" />
          </el-select>
          <el-button @click="groupCollapsed = !groupCollapsed">{{ groupCollapsed ? '展开分组' : '折叠分组' }}</el-button>
          <el-checkbox v-model="defaultCollapse" @change="onDefaultCollapseChange">默认折叠</el-checkbox>
          <el-button @click="exportRoles">导出 Excel</el-button>
          <el-select v-model="assignableFilter" class="assignable-select" style="width: 140px" placeholder="可分配" @change="syncFilterUrl">
            <el-option label="全部角色" value="" />
            <el-option label="可分配" value="1" />
            <el-option label="不可分配" value="0" />
          </el-select>
          <el-button v-if="has('add')" type="primary" :icon="Plus" @click="openAdd">新增角色</el-button>
        </div>
      </div>
      <el-radio-group v-model="groupFilter" size="small" class="role-groups">
        <el-radio-button label="">全部（{{ records.length }}）</el-radio-button>
        <el-radio-button label="platform">平台（{{ groupCounts.platform }}）</el-radio-button>
        <el-radio-button label="company">公司（{{ groupCounts.company }}）</el-radio-button>
        <el-radio-button label="hospital">院方（{{ groupCounts.hospital }}）</el-radio-button>
      </el-radio-group>
      <div v-if="groupCollapsed" class="group-summary">
        <el-card v-for="g in groupSummary" :key="g.key" shadow="never" class="group-card" @click="expandGroup(g.key)">
          <div class="group-name">{{ g.label }}（{{ g.roles.length }}）</div>
          <div class="group-roles">{{ g.roles.map((r) => r.roleName).join('、') || '暂无角色' }}</div>
        </el-card>
      </div>
      <el-table v-else :data="filteredRecords" stripe>
        <el-table-column type="index" label="序号" width="62" align="center" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleKey" label="权限字符" min-width="150" />
        <el-table-column prop="roleRank" label="等级" width="80" />
        <el-table-column label="分组" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="roleGroup(row) === 'hospital' ? 'warning' : roleGroup(row) === 'company' ? 'success' : 'info'">
              {{ roleGroupText(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.roleType === 'standard' ? 'success' : 'info'" size="small">
              {{ row.roleType === 'standard' ? '标准' : '自定义' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="可分配" width="90">
          <template #default="{ row }">
            <el-tag :type="row.assignable === '1' ? 'success' : 'info'" size="small">
              {{ row.assignable === '1' ? '可分配' : '不可分配' }}
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
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-if="has('edit') && canEditRow(row)" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" v-if="has('edit') && canEditRow(row) && row.roleId !== 1" @click="openMenus(row)">菜单权限</el-button>
            <el-button link type="primary" @click="openAssignLogs(row)">分配记录</el-button>
            <el-button link type="danger" v-if="has('remove') && canEditRow(row) && row.roleId !== 1 && row.roleType !== 'standard'" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="form.roleId ? '编辑角色' : '新增角色'" width="520px">
      <el-alert
        v-if="form.roleType === 'standard'"
        title="平台标准角色不可删除，权限字符不可修改；仅可调整名称、排序、数据范围和备注。"
        type="warning"
        :closable="false"
        show-icon
        class="standard-alert"
      />
      <el-form :model="form" label-width="86px">
        <el-form-item label="角色名称" required><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="权限字符" required>
          <el-input v-model="form.roleKey" :disabled="form.roleType === 'standard'" placeholder="如 hospital_admin" />
          <div v-if="form.roleType === 'standard'" class="standard-hint">平台标准角色的权限字符不可修改</div>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option label="全部数据" value="all" /><el-option label="本区域" value="region" /><el-option label="本医院" value="hospital" /><el-option label="仅本人" value="self" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="store.isSuperAdmin" label="可分配">
          <el-switch v-model="form.assignable" active-value="1" inactive-value="0" />
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

    <el-drawer v-model="assignDrawer" size="560px" :title="'分配记录 · ' + (assignRole?.roleName || '')">
      <div class="assign-toolbar">
        <el-select v-model="assignAction" style="width: 140px" @change="loadAssignLogs(1)">
          <el-option label="全部动作" value="" />
          <el-option label="分配" value="assign" />
          <el-option label="移除" value="revoke" />
          <el-option label="创建" value="create" />
          <el-option label="修改" value="update" />
          <el-option label="删除" value="delete" />
        </el-select>
        <span class="assign-total">共 {{ assignTotal }} 条</span>
      </div>
      <el-table :data="assignLogs" stripe>
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column prop="action" label="动作" width="80">
          <template #default="{ row }">{{ actionText(row.action) }}</template>
        </el-table-column>
        <el-table-column prop="userName" label="用户" />
        <el-table-column prop="operatorName" label="操作人" width="110" />
        <el-table-column prop="detail" label="说明" show-overflow-tooltip />
      </el-table>
      <el-pagination
        v-model:current-page="assignPage"
        :page-size="assignSize"
        :total="assignTotal"
        layout="prev, pager, next"
        class="assign-pager"
        @change="loadAssignLogs"
      />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const route = useRoute()
const router = useRouter()
const has = (a) => store.hasPerm('system:role:' + a)
const canEditRow = (row) => store.isSuperAdmin || !!row.companyId
const records = ref([])
const assignableFilter = ref(String(route.query.assignable || ''))
const groupFilter = ref(String(route.query.group || ''))
const roleSort = ref(String(route.query.sort || 'rankDesc'))
const defaultCollapse = ref(localStorage.getItem('laminar_role_default_collapsed') === '1')
const groupCollapsed = ref(defaultCollapse.value)
function onDefaultCollapseChange(value) {
  localStorage.setItem('laminar_role_default_collapsed', value ? '1' : '0')
  groupCollapsed.value = value
}
const roleGroup = (row) => String(row.roleKey || '').startsWith('hospital_') ? 'hospital'
  : row.companyId ? 'company' : 'platform'
const roleGroupText = (row) => ({ platform: '平台', company: '公司', hospital: '院方' }[roleGroup(row)])
const groupCounts = computed(() => records.value.reduce((acc, row) => {
  acc[roleGroup(row)] = (acc[roleGroup(row)] || 0) + 1
  return acc
}, { platform: 0, company: 0, hospital: 0 }))
const groupSummary = computed(() => [
  { key: 'platform', label: '平台角色', roles: records.value.filter((r) => roleGroup(r) === 'platform') },
  { key: 'company', label: '公司角色', roles: records.value.filter((r) => roleGroup(r) === 'company') },
  { key: 'hospital', label: '院方角色', roles: records.value.filter((r) => roleGroup(r) === 'hospital') }
])
function expandGroup(key) {
  groupFilter.value = key
  groupCollapsed.value = false
}
async function exportRoles() {
  const res = await request.get('/system/role/export', { responseType: 'blob' })
  const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'roles.xlsx'
  a.click()
  URL.revokeObjectURL(url)
}
const assignDrawer = ref(false)
const assignRole = ref(null)
const assignLogs = ref([])
const assignAction = ref('')
const assignPage = ref(1)
const assignSize = ref(10)
const assignTotal = ref(0)
const filteredRecords = computed(() => {
  const list = records.value
    .filter((row) => !groupFilter.value || roleGroup(row) === groupFilter.value)
    .filter((row) => !assignableFilter.value || row.assignable === assignableFilter.value)
    .slice()
  if (roleSort.value === 'rankDesc') list.sort((a, b) => Number(b.roleRank || 0) - Number(a.roleRank || 0))
  if (roleSort.value === 'rankAsc') list.sort((a, b) => Number(a.roleRank || 0) - Number(b.roleRank || 0))
  return list
})
const dialog = ref(false)
const form = reactive({})
const current = ref(null)
const menuDrawer = ref(false)
const menus = ref([])
const treeRef = ref()
const scopeText = (s) => ({ all: '全部', region: '本区域', hospital: '本医院', self: '仅本人' }[s] || s)
const actionText = (a) => ({ assign: '分配', revoke: '移除', create: '创建', update: '修改', delete: '删除' }[a] || a)

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
function openAdd() { Object.keys(form).forEach((k) => delete form[k]); Object.assign(form, { sort: 1, dataScope: 'self', status: '0', assignable: '1' }); dialog.value = true }
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

async function openAssignLogs(row) {
  assignRole.value = row
  assignDrawer.value = true
  assignAction.value = ''
  assignPage.value = 1
  await loadAssignLogs()
}

async function loadAssignLogs(page = assignPage.value) {
  assignPage.value = page || 1
  const res = await request.get(`/system/role/${assignRole.value.roleId}/assignmentLogs`, {
    params: {
      action: assignAction.value || undefined,
      pageNum: assignPage.value,
      pageSize: assignSize.value
    }
  })
  assignLogs.value = res.data.records || []
  assignTotal.value = res.data.total || 0
}

function syncFilterUrl() {
  router.replace({ query: { ...route.query, assignable: assignableFilter.value || undefined } })
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
.role-actions { display: flex; align-items: center; gap: 10px; }
.role-groups { margin-bottom: 12px; }
.group-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 12px; }
.group-card { cursor: pointer; border-radius: 10px; }
.group-name { font-weight: 600; margin-bottom: 6px; }
.group-roles { color: #6b7280; font-size: 12px; line-height: 1.6; }
.standard-hint { width: 100%; margin-top: 4px; color: #909399; font-size: 11px; line-height: 1.4; }
.standard-alert { margin-bottom: 12px; }
.assign-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.assign-total { color: #909399; font-size: 12px; }
.assign-pager { margin-top: 12px; justify-content: flex-end; }
.perm-presets { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.perm-label { color: #606266; font-size: 13px; }
.perm-hint { color: #909399; font-size: 12px; line-height: 1.5; margin-bottom: 10px; }
</style>





