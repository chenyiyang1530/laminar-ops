<template>
  <div>
    <CrudPage resource="contract" detail>
      <template #row-actions="{ row }">
        <el-button link type="primary" @click="openScope(row)">服务范围</el-button>
      </template>
    </CrudPage>

    <el-dialog v-model="scopeDialog" title="合同服务范围" width="760px" destroy-on-close>
      <div v-loading="scopeLoading">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="按科室、洁净区域或设备限定本合同的服务范围；不限定则默认全院。多家公司可按不同范围分别签约。"
          class="scope-tip"
        />
        <el-form label-width="100px" class="scope-form">
          <el-form-item label="服务范围">
            <el-checkbox v-model="wholeHospital">全院（不限定范围）</el-checkbox>
          </el-form-item>
          <template v-if="!wholeHospital">
            <el-form-item label="科室">
              <el-select v-model="departmentIds" multiple filterable collapse-tags style="width: 100%" placeholder="选择服务科室">
                <el-option v-for="d in departments" :key="d.departmentId" :label="d.departmentName" :value="d.departmentId" />
              </el-select>
            </el-form-item>
            <el-form-item label="洁净区域">
              <el-select v-model="areaIds" multiple filterable collapse-tags style="width: 100%" placeholder="选择服务洁净区域">
                <el-option v-for="a in areas" :key="a.areaId" :label="a.areaName" :value="a.areaId" />
              </el-select>
            </el-form-item>
            <el-form-item label="设备">
              <el-select v-model="equipmentIds" multiple filterable collapse-tags style="width: 100%" placeholder="选择服务设备">
                <el-option v-for="e in equipments" :key="e.equipmentId" :label="e.equipmentName" :value="e.equipmentId" />
              </el-select>
            </el-form-item>
          </template>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="scopeDialog = false">取消</el-button>
        <el-button type="primary" :loading="scopeSaving" @click="saveScope">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import CrudPage from '@/components/CrudPage.vue'
import request from '@/utils/request'

const scopeDialog = ref(false)
const scopeLoading = ref(false)
const scopeSaving = ref(false)
const current = ref({})
const wholeHospital = ref(false)
const departmentIds = ref([])
const areaIds = ref([])
const equipmentIds = ref([])
const departments = ref([])
const areas = ref([])
const equipments = ref([])

async function openScope(row) {
  current.value = row
  scopeDialog.value = true
  scopeLoading.value = true
  wholeHospital.value = false
  departmentIds.value = []
  areaIds.value = []
  equipmentIds.value = []
  try {
    const hospitalId = row.hospitalId
    const [scopes, dep, area, eq] = await Promise.all([
      request.get('/contract-scope/' + row.contractId),
      request.get('/crud/department/page', { params: { hospitalId, pageNum: 1, pageSize: 500 } }),
      request.get('/crud/cleanArea/page', { params: { hospitalId, pageNum: 1, pageSize: 500 } }),
      request.get('/crud/equipment/page', { params: { hospitalId, pageNum: 1, pageSize: 500 } })
    ])
    departments.value = dep.data.records || []
    areas.value = area.data.records || []
    equipments.value = eq.data.records || []
    const list = scopes.data || []
    wholeHospital.value = list.length === 0 || list.some((s) => s.scopeType === 'hospital')
    departmentIds.value = list.filter((s) => s.scopeType === 'department').map((s) => s.scopeValue)
    areaIds.value = list.filter((s) => s.scopeType === 'area').map((s) => s.scopeValue)
    equipmentIds.value = list.filter((s) => s.scopeType === 'equipment').map((s) => s.scopeValue)
  } finally {
    scopeLoading.value = false
  }
}

async function saveScope() {
  const scopes = []
  if (wholeHospital.value) {
    scopes.push({ scopeType: 'hospital', scopeValue: null })
  } else {
    departmentIds.value.forEach((id) => scopes.push({ scopeType: 'department', scopeValue: id }))
    areaIds.value.forEach((id) => scopes.push({ scopeType: 'area', scopeValue: id }))
    equipmentIds.value.forEach((id) => scopes.push({ scopeType: 'equipment', scopeValue: id }))
    if (!scopes.length) {
      ElMessage.warning('请选择服务范围，或勾选全院')
      return
    }
  }
  scopeSaving.value = true
  try {
    await request.post('/contract-scope/' + current.value.contractId, scopes)
    ElMessage.success('服务范围已保存')
    scopeDialog.value = false
  } finally {
    scopeSaving.value = false
  }
}
</script>

<style scoped>
.scope-tip { margin-bottom: 14px; }
.scope-form { margin-top: 6px; }
</style>
