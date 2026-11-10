<template>
  <div class="app-container">
    <div class="page-card">
      <el-form :model="query" inline size="default" class="search-form">
        <el-form-item label="医院名称">
          <el-input v-model="query.hospitalName" placeholder="名称 / 编码" clearable style="width: 190px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="医院等级">
          <el-select v-model="query.grade" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="d in dicts.grade" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属区域">
          <el-select v-model="query.regionId" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="r in regions" :key="r.regionId" :label="r.regionName" :value="r.regionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="d in dicts.status" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <div class="toolbar-left">
          <el-button v-if="canEditCustomer && store.hasPerm('customer:hospital:add')" type="primary" :icon="Plus" @click="openAdd">新增医院</el-button>
          <el-button v-if="canEditCustomer && store.hasPerm('customer:hospital:remove')" :disabled="!selection.length" :icon="Delete" @click="removeSelected">批量删除</el-button>
        </div>
        <div class="toolbar-right">
          <ListTools
            v-model="visibleCols"
            :columns="hospitalColumns"
            :default-keys="hospitalDefaultKeys"
            :exporting="exporting"
            @reset="resetCols"
            @export="exportCsv"
          />
          <span>共 {{ total }} 家合作医院</span>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe @selection-change="(v) => (selection = v)">
        <el-table-column type="selection" width="46" />
        <el-table-column type="index" label="序号" width="62" align="center" :index="i => (query.pageNum - 1) * query.pageSize + i + 1" />
        <el-table-column v-if="isVisible('hospitalName')" label="医院" min-width="230">
          <template #default="{ row }">
            <div class="hospital-cell">
              <div class="hospital-avatar">{{ row.hospitalName.slice(0, 1) }}</div>
              <div>
                <div class="hospital-name">{{ row.hospitalName }}</div>
                <div class="hospital-code">{{ row.hospitalCode || '—' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="isVisible('grade')" label="等级" min-width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.grade === '三甲' ? 'danger' : 'info'" effect="plain">{{ label(dicts.grade, row.grade) || row.grade || '—' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="isVisible('hospitalType')" label="类型" min-width="120">
          <template #default="{ row }">{{ label(dicts.type, row.hospitalType) || '—' }}</template>
        </el-table-column>
        <el-table-column v-if="isVisible('regionName')" prop="regionName" label="所属区域" min-width="120" />
        <el-table-column v-if="isVisible('address')" prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column v-if="isVisible('contact')" label="联系人" min-width="150">
          <template #default="{ row }">
            <div>{{ row.contactPerson || '—' }}</div>
            <div class="sub-text">{{ row.phone || '' }}</div>
          </template>
        </el-table-column>
        <el-table-column v-if="isVisible('status')" label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">{{ row.status === '0' ? '合作中' : '已停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canEditCustomer" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="$router.push({ path: '/customer/campus', query: { hospitalId: row.hospitalId } })">院区</el-button>
            <el-button v-if="canEditCustomer && store.hasPerm('customer:hospital:remove')" link type="danger" @click="removeOne(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pager"
        @change="load"
      />
    </div>

    <el-dialog v-model="dialog" :title="form.hospitalId ? '编辑医院' : '新增医院'" width="720px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="医院名称" prop="hospitalName"><el-input v-model="form.hospitalName" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="医院编码">
              <el-input :model-value="form.hospitalCode" placeholder="如 HXM001（自动转大写）" @input="onCodeInput" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属区域" prop="regionId">
              <el-select v-model="form.regionId" clearable style="width: 100%">
                <el-option v-for="r in regions" :key="r.regionId" :label="r.regionName" :value="r.regionId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="医院等级">
              <el-select v-model="form.grade" style="width: 100%">
                <el-option v-for="d in dicts.grade" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="医院类型">
              <el-select v-model="form.hospitalType" style="width: 100%">
                <el-option v-for="d in dicts.type" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="医院性质">
              <el-select v-model="form.nature" style="width: 100%">
                <el-option v-for="d in dicts.nature" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人"><el-input v-model="form.contactPerson" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="详细地址">
              <el-input v-model="form.address" placeholder="可手动输入，或点击右侧按钮在地图上选址">
                <template #append>
                  <el-button :icon="Location" @click="openMap">地图选址</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经度"><el-input-number v-model="form.longitude" :precision="6" :step="0.001" :controls="false" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度"><el-input-number v-model="form.latitude" :precision="6" :step="0.001" :controls="false" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio value="0">合作中</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" size="920px" :title="detail.hospital.hospitalName || '医院详情'" destroy-on-close>
      <div v-loading="detailLoading" class="detail-body">
        <div class="detail-hero">
          <div class="hospital-avatar detail-avatar">{{ (detail.hospital.hospitalName || '医').slice(0, 1) }}</div>
          <div class="hero-main">
            <div class="hero-title">{{ detail.hospital.hospitalName || '—' }}</div>
            <div class="hero-sub">{{ detail.hospital.hospitalCode || '—' }} · {{ detail.hospital.address || '—' }}</div>
            <div class="hero-tags">
              <el-tag size="small" effect="plain">{{ label(dicts.grade, detail.hospital.grade) || detail.hospital.grade || '未定级' }}</el-tag>
              <el-tag size="small" type="info" effect="plain">{{ label(dicts.type, detail.hospital.hospitalType) || '—' }}</el-tag>
              <el-tag size="small" :type="detail.hospital.status === '0' ? 'success' : 'info'">
                {{ detail.hospital.status === '0' ? '合作中' : '已停用' }}
              </el-tag>
            </div>
          </div>
        </div>

        <el-descriptions :column="3" border size="small" class="detail-desc">
          <el-descriptions-item label="所属区域">{{ detail.hospital.regionName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="医院性质">{{ label(dicts.nature, detail.hospital.nature) || '—' }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detail.hospital.contactPerson || '—' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.hospital.phone || '—' }}</el-descriptions-item>
          <el-descriptions-item label="经度">{{ detail.hospital.longitude ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="纬度">{{ detail.hospital.latitude ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="详细地址" :span="3">{{ detail.hospital.address || '—' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="3">{{ detail.hospital.remark || '—' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-stats">
          <div v-for="s in detailStats" :key="s.label" class="stat-card">
            <div class="stat-value">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>

        <el-tabs v-model="detailTab" class="detail-tabs">
          <el-tab-pane label="院区" name="campus">
            <el-table :data="detail.campuses" size="small" stripe max-height="300">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="campusName" label="院区名称" min-width="160" />
              <el-table-column prop="campusCode" label="编码" width="120" />
              <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
              <el-table-column prop="bedCount" label="床位" width="80" align="center" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === '0' ? 'success' : 'info'">{{ row.status === '0' ? '启用' : '停用' }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="科室" name="department">
            <el-table :data="detail.departments" size="small" stripe max-height="300">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="departmentName" label="科室名称" min-width="160" />
              <el-table-column prop="deptCode" label="编码" width="120" />
              <el-table-column prop="phone" label="联系电话" width="140" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === '0' ? 'success' : 'info'">{{ row.status === '0' ? '启用' : '停用' }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="设备" name="equipment">
            <el-table :data="detail.equipments" size="small" stripe max-height="300">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="equipmentCode" label="设备编号" width="140" />
              <el-table-column prop="equipmentName" label="设备名称" min-width="180" />
              <el-table-column prop="model" label="型号" width="130" />
              <el-table-column label="运行状态" width="110">
                <template #default="{ row }">{{ label(dicts.runStatus, row.runStatus) || '—' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="合同" name="contract">
            <el-table :data="detail.contracts" size="small" stripe max-height="300">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="contractNo" label="合同编号" width="150" />
              <el-table-column prop="contractName" label="合同名称" min-width="200" />
              <el-table-column prop="endDate" label="到期日期" width="120" />
              <el-table-column prop="amount" label="金额(元)" width="120" align="right" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">{{ { 0: '执行中', 1: '已终止', 2: '已过期', 3: '未开始' }[row.status] || '—' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="对接人" name="contact">
            <el-table :data="detail.contacts" size="small" stripe max-height="300">
              <el-table-column type="index" label="序号" width="60" align="center" />
              <el-table-column prop="contactName" label="姓名" width="120" />
              <el-table-column prop="position" label="职务" width="140" />
              <el-table-column prop="phone" label="联系电话" width="150" />
              <el-table-column label="主联系人" width="100">
                <template #default="{ row }">
                  <el-tag v-if="row.isPrimary === '1'" size="small" type="success">是</el-tag>
                  <span v-else>—</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button @click="$router.push({ path: '/customer/campus', query: { hospitalId: detail.hospital.hospitalId } })">查看全部院区</el-button>
        <el-button v-if="canEditCustomer" type="primary" @click="openEdit(detail.hospital)">编辑资料</el-button>
      </template>
    </el-drawer>

    <AmapPicker ref="amapPicker" @select="onLocationSelect" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete, Location } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import AmapPicker from '@/components/AmapPicker.vue'
import { pageHospital, addHospital, updateHospital, delHospital, regionOptions } from '@/api/customer/hospital'
import ListTools from '@/components/ListTools.vue'
import { useColumnPrefs } from '@/composables/useColumnPrefs'
import { downloadCsv } from '@/utils/csv'

const hospitalColumns = [
  { key: 'hospitalName', label: '医院' },
  { key: 'grade', label: '等级' },
  { key: 'hospitalType', label: '类型' },
  { key: 'regionName', label: '所属区域' },
  { key: 'address', label: '地址' },
  { key: 'contact', label: '联系人' },
  { key: 'status', label: '状态' }
]
const hospitalDefaultKeys = hospitalColumns.map((c) => c.key)
const { visible: visibleCols, reset: resetCols } = useColumnPrefs('laminar_cols_hospital', hospitalDefaultKeys)
const exporting = ref(false)
const isVisible = (key) => visibleCols.value.includes(key)

const store = useUserStore()
const canEditCustomer = computed(() => !store.companyId)
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const selection = ref([])
const regions = ref([])
const dialog = ref(false)
const formRef = ref()
const amapPicker = ref()
const dicts = reactive({ grade: [], type: [], nature: [], status: [], runStatus: [] })
const query = reactive({ pageNum: 1, pageSize: store.pageSize || 10, hospitalName: '', grade: '', regionId: '', status: '' })
const form = reactive({})
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailTab = ref('campus')
const detail = reactive({
  hospital: {},
  campuses: [],
  departments: [],
  equipments: [],
  contracts: [],
  contacts: [],
  totals: { campus: 0, department: 0, equipment: 0, contract: 0, contact: 0 }
})
const detailStats = computed(() => [
  { label: '院区', value: detail.totals.campus },
  { label: '科室', value: detail.totals.department },
  { label: '设备', value: detail.totals.equipment },
  { label: '合同', value: detail.totals.contract },
  { label: '对接人', value: detail.totals.contact }
])

const rules = {
  hospitalName: [{ required: true, message: '请输入医院名称', trigger: 'blur' }],
  regionId: [{ required: true, message: '请选择所属区域', trigger: 'change' }]
}

function label(arr, v) {
  return arr.find((d) => d.dictValue === v)?.dictLabel
}

async function loadDicts() {
  const types = [
    ['grade', 'hospital_grade'],
    ['type', 'hospital_type'],
    ['nature', 'hospital_nature'],
    ['status', 'sys_normal_disable'],
    ['runStatus', 'run_status']
  ]
  for (const [key, type] of types) {
    const res = await request.get('/system/dict/type/' + type)
    dicts[key] = res.data
  }
}

async function load() {
  loading.value = true
  try {
    const res = await pageHospital(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

async function exportCsv() {
  exporting.value = true
  try {
    const res = await pageHospital({ ...query, pageNum: 1, pageSize: 5000 })
    const rows = res.data?.records || []
    if (!rows.length) {
      ElMessage.warning('当前筛选条件下没有数据')
      return
    }
    const cols = hospitalColumns.filter((c) => isVisible(c.key))
    const headers = cols.map((c) => c.label)
    const data = rows.map((row) => cols.map((c) => {
      if (c.key === 'grade') return label(dicts.grade, row.grade) || row.grade || ''
      if (c.key === 'hospitalType') return label(dicts.type, row.hospitalType) || ''
      if (c.key === 'contact') return row.contactPerson || ''
      if (c.key === 'status') return row.status === '0' ? '合作中' : '已停用'
      return row[c.key] ?? ''
    }))
    downloadCsv(`医院-${new Date().toISOString().slice(0, 10)}.csv`, headers, data)
  } finally {
    exporting.value = false
  }
}

function reset() {
  Object.assign(query, { pageNum: 1, hospitalName: '', grade: '', regionId: '', status: '' })
  load()
}

function onCodeInput(v) {
  form.hospitalCode = String(v || '').toUpperCase().replace(/\s+/g, '')
}

function openMap() {
  amapPicker.value?.open({
    address: form.address,
    longitude: form.longitude,
    latitude: form.latitude
  })
}

function onLocationSelect(location) {
  form.address = location.address
  form.longitude = location.longitude ? Number(location.longitude) : undefined
  form.latitude = location.latitude ? Number(location.latitude) : undefined
}

function openAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { status: '0', nature: 'public', hospitalType: 'general' })
  dialog.value = true
}

async function openEdit(row) {
  const res = await request.get('/customer/hospital/' + row.hospitalId)
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, res.data)
  if (form.hospitalCode) form.hospitalCode = String(form.hospitalCode).toUpperCase()
  dialog.value = true
}

async function openDetail(row) {
  detailVisible.value = true
  detailTab.value = 'campus'
  detailLoading.value = true
  detail.hospital = row
  detail.campuses = []
  detail.departments = []
  detail.equipments = []
  detail.contracts = []
  detail.contacts = []
  try {
    const params = { hospitalId: row.hospitalId, pageNum: 1, pageSize: 100 }
    const [h, ca, de, eq, co, ct] = await Promise.allSettled([
      request.get('/customer/hospital/' + row.hospitalId),
      request.get('/crud/campus/page', { params }),
      request.get('/crud/department/page', { params }),
      request.get('/crud/equipment/page', { params }),
      request.get('/crud/contract/page', { params }),
      request.get('/crud/contact/page', { params })
    ])
    const take = (p) => (p.status === 'fulfilled' ? p.value.data : null)
    detail.hospital = take(h) || row
    setDetailList('campuses', 'campus', take(ca))
    setDetailList('departments', 'department', take(de))
    setDetailList('equipments', 'equipment', take(eq))
    setDetailList('contracts', 'contract', take(co))
    setDetailList('contacts', 'contact', take(ct))
  } finally {
    detailLoading.value = false
  }
}

function setDetailList(key, totalKey, data) {
  detail[key] = data?.records || []
  detail.totals[totalKey] = data?.total || 0
}

async function submit() {
  await formRef.value.validate()
  if (form.hospitalCode) form.hospitalCode = String(form.hospitalCode).trim().toUpperCase()
  saving.value = true
  try {
    if (form.hospitalId) await updateHospital(form)
    else await addHospital(form)
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function removeOne(row) {
  await ElMessageBox.confirm(`确认删除医院「${row.hospitalName}」？`, '提示', { type: 'warning' })
  await delHospital(row.hospitalId)
  ElMessage.success('已删除')
  load()
}

async function removeSelected() {
  await ElMessageBox.confirm(`确认删除选中的 ${selection.value.length} 家医院？`, '提示', { type: 'warning' })
  await delHospital(selection.value.map((r) => r.hospitalId).join(','))
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  await loadDicts()
  const r = await regionOptions()
  regions.value = r.data
  load()
})
</script>

<style scoped>
.search-form { margin-bottom: 4px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin: 6px 0 12px; }
.toolbar-right { display: flex; align-items: center; gap: 10px; color: #8494a8; font-size: 12px; }
.hospital-cell { display: flex; gap: 10px; align-items: center; }
.hospital-avatar {
  width: 34px; height: 34px; border-radius: 9px; flex-shrink: 0;
  background: linear-gradient(135deg, #e8f0fe, #dbeafe); color: #1f6feb;
  display: flex; align-items: center; justify-content: center; font-weight: 700;
}
.hospital-name { font-size: 13px; color: #1f2d3d; font-weight: 600; }
.hospital-code { font-size: 11px; color: #94a3b8; margin-top: 2px; }
.sub-text { font-size: 11px; color: #94a3b8; }
.pager { margin-top: 14px; justify-content: flex-end; }
.detail-body { display: flex; flex-direction: column; gap: 14px; }
.detail-hero { display: flex; align-items: center; gap: 14px; padding: 2px 2px 4px; }
.detail-avatar { width: 52px; height: 52px; font-size: 20px; border-radius: 14px; }
.hero-main { flex: 1; min-width: 0; }
.hero-title { font-size: 18px; font-weight: 700; color: #1f2d3d; }
.hero-sub { margin-top: 3px; font-size: 12px; color: #8494a8; }
.hero-tags { display: flex; gap: 6px; margin-top: 8px; }
.detail-stats { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; }
.stat-card { padding: 10px 14px; background: #f6f9fe; border: 1px solid #e8eef7; border-radius: 10px; }
.stat-value { font-size: 20px; font-weight: 700; line-height: 1.2; color: #1f6feb; }
.stat-label { margin-top: 2px; font-size: 12px; color: #7c8ba1; }
.detail-tabs { margin-top: 2px; }
</style>
