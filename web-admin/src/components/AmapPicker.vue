<template>
  <el-dialog v-model="visible" :title="title" width="900px" append-to-body destroy-on-close @opened="initMap" @closed="cleanup">
    <div class="amap-container">
      <div class="search-box">
        <el-input v-model="keyword" placeholder="输入地址关键词，回车搜索" clearable @keyup.enter="search">
          <template #append>
            <el-button @click="search">搜索</el-button>
          </template>
        </el-input>
      </div>

      <div ref="mapEl" class="map-wrapper"></div>

      <div class="selected-info">
        <el-form label-width="80px" size="small">
          <el-row :gutter="12">
            <el-col :span="14">
              <el-form-item label="详细地址">
                <el-input v-model="selected.address" readonly placeholder="点击地图选择位置" />
              </el-form-item>
            </el-col>
            <el-col :span="5">
              <el-form-item label="经度">
                <el-input v-model="selected.longitude" readonly />
              </el-form-item>
            </el-col>
            <el-col :span="5">
              <el-form-item label="纬度">
                <el-input v-model="selected.latitude" readonly />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="!selected.address" @click="confirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  title: { type: String, default: '地图选址' },
  // 可在 .env 中配置 VITE_AMAP_KEY / VITE_AMAP_SECURITY_CODE
  amapKey: {
    type: String,
    default: (import.meta.env || {}).VITE_AMAP_KEY || 'c200ba422d35f1730af54c96d76ee2c8'
  },
  amapSecurityCode: {
    type: String,
    default: (import.meta.env || {}).VITE_AMAP_SECURITY_CODE || '947e55b303e7b01a1d2e11674e8ffb5b'
  }
})

const visible = ref(false)
const keyword = ref('')
const mapEl = ref()
const selected = reactive({ address: '', longitude: '', latitude: '' })

let map = null
let marker = null
let geocoder = null
let placeSearch = null
let sdkPromise = null

function open(location = {}) {
  selected.address = location.address || ''
  selected.longitude = location.longitude != null ? String(location.longitude) : ''
  selected.latitude = location.latitude != null ? String(location.latitude) : ''
  visible.value = true
}

function loadSdk() {
  if (window.AMap) return Promise.resolve()
  if (sdkPromise) return sdkPromise
  window._AMapSecurityConfig = { securityJsCode: props.amapSecurityCode }
  sdkPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.type = 'text/javascript'
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${props.amapKey}&plugin=AMap.Geocoder,AMap.PlaceSearch`
    script.onload = () => resolve()
    script.onerror = () => {
      sdkPromise = null
      reject(new Error('AMap SDK load failed'))
    }
    document.head.appendChild(script)
  })
  return sdkPromise
}

async function initMap() {
  try {
    await loadSdk()
    if (!mapEl.value) return
    if (map) {
      map.destroy()
      map = null
    }
    const center = selected.longitude && selected.latitude
      ? [Number(selected.longitude), Number(selected.latitude)]
      : [116.397428, 39.90923]
    map = new window.AMap.Map(mapEl.value, { zoom: 16, center, resizeEnable: true })
    geocoder = new window.AMap.Geocoder({ radius: 1000, extensions: 'all' })
    placeSearch = new window.AMap.PlaceSearch({ map })
    map.on('click', (e) => {
      setPoint(e.lnglat.getLng(), e.lnglat.getLat())
    })
    if (selected.address && selected.longitude && selected.latitude) {
      setMarker([Number(selected.longitude), Number(selected.latitude)])
    }
  } catch (e) {
    ElMessage.error('地图加载失败，请检查网络或高德地图 Key')
  }
}

function setMarker(lnglat) {
  if (marker) marker.setMap(null)
  marker = new window.AMap.Marker({ position: lnglat, animation: 'AMAP_ANIMATION_DROP' })
  map.add(marker)
  map.setCenter(lnglat)
}

function setPoint(lng, lat, address) {
  const lnglat = [lng, lat]
  setMarker(lnglat)
  selected.longitude = Number(lng).toFixed(6)
  selected.latitude = Number(lat).toFixed(6)
  if (address) {
    selected.address = address
    return
  }
  geocoder.getAddress(lnglat, (status, result) => {
    if (status === 'complete' && result.regeocode) {
      selected.address = result.regeocode.formattedAddress
    }
  })
}

function search() {
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入地址关键词')
    return
  }
  if (!placeSearch) {
    ElMessage.error('地图尚未初始化')
    return
  }
  placeSearch.search(keyword.value.trim(), (status, result) => {
    const poi = result?.pois?.[0]
    if (status === 'complete' && poi?.location) {
      setPoint(poi.location.lng, poi.location.lat, `${poi.name || ''}${poi.address || ''}`)
    } else {
      ElMessage.warning('未找到相关地址')
    }
  })
}

function confirm() {
  if (!selected.address) return
  visible.value = false
  emit('select', { ...selected })
}

function cleanup() {
  if (map) {
    map.destroy()
    map = null
  }
  marker = null
  geocoder = null
  placeSearch = null
  keyword.value = ''
}

const emit = defineEmits(['select'])
defineExpose({ open })
</script>

<style scoped>
.amap-container { min-height: 480px; }
.search-box { margin-bottom: 12px; }
.map-wrapper {
  width: 100%;
  height: 420px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
}
.selected-info {
  margin-top: 14px;
  padding: 12px 14px 0;
  background: #f6f9fe;
  border: 1px solid #e8eef7;
  border-radius: 8px;
}
.selected-info :deep(.el-form-item) { margin-bottom: 10px; }
</style>
