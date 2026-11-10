<template>
  <div class="login-page">
    <div class="brand-panel">
      <div class="brand-top">
        <div class="brand-mark">LO</div>
        <div>
          <div class="brand-name">层流净化设备智能运维平台</div>
          <div class="brand-en">Laminar Purification Intelligent O&M Platform</div>
        </div>
      </div>
      <div class="brand-body">
        <h1>让每一间洁净手术室<br /><span>始终处于受控状态</span></h1>
        <p>多医院 · 多院区 · 多科室 · 洁净区域全生命周期监测与运维闭环</p>
        <ul>
          <li><el-icon><Monitor /></el-icon> 层流机组 / 净化空调 / 新风 / 除湿 / 供氧 / 负压在线监测</li>
          <li><el-icon><Warning /></el-icon> 告警分级、确认、消缺、全链路追溯</li>
          <li><el-icon><Tools /></el-icon> 报修—派工—处置—验收—报价—回访工单闭环</li>
          <li><el-icon><Calendar /></el-icon> 周期巡检与预防性保养计划自动生成</li>
        </ul>
      </div>
      <div class="brand-stats">
        <div><b>3</b><span>合作医院</span></div>
        <div><b>8</b><span>洁净区域</span></div>
        <div><b>45</b><span>监测点位</span></div>
        <div><b>24h</b><span>在线守护</span></div>
      </div>
    </div>

    <div class="form-panel">
      <div class="form-box">
        <div class="form-head">
          <h2>欢迎回来</h2>
          <p>登录运维后台，查看设备与工单实时状态</p>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" size="large" @keyup.enter="submit">
          <el-form-item prop="companyCode">
            <el-input v-model="form.companyCode" placeholder="公司编码（可选，手机号/邮箱登录可不填）" :prefix-icon="OfficeBuilding" clearable />
          </el-form-item>
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="手机号 / 邮箱 / 登录账号" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">登 录</el-button>
        </el-form>
        <div class="login-tip">手机号或邮箱可直接登录；同名账号需填写公司编码；平台管理员可不填</div>

        <div v-if="demoEnabled" class="demo">
          <div class="demo-title">测试账号（点击直接登录，密码均为 123456）</div>
          <div class="demo-list">
            <span @click="fill('admin')">平台管理员 admin</span>
            <span @click="quickLogin('test_ceo')">测试公司高管 test_ceo</span>
            <span @click="quickLogin('test_boss')">测试总负责人 test_boss</span>
            <span @click="quickLogin('test_area')">测试大区负责人 test_area</span>
            <span @click="quickLogin('test_region')">测试区域负责人 test_region</span>
            <span @click="quickLogin('test_eng')">测试工程师 test_eng</span>
          </div>
        </div>
      </div>
      <div class="copyright">© 2026 Laminar Ops · 层流净化设备智能监测运维一体化平台</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, OfficeBuilding } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { addDynamicRoutes } from '@/router'

const store = useUserStore()
const router = useRouter()
const route = useRoute()
const formRef = ref()
const loading = ref(false)
const form = reactive({
  companyCode: '',
  username: '',
  password: ''
})
const demoEnabled = ref(false)
const rules = {
  username: [{ required: true, message: '请输入手机号、邮箱或账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function fill(name) {
  const companyCodeMap = {
    test_ceo: 'TEST-OPS',
    test_boss: 'TEST-OPS',
    test_area: 'TEST-OPS',
    test_region: 'TEST-OPS',
    test_eng: 'TEST-OPS',
    manager: 'CLEAN-XM',
    engineer1: 'CLEAN-XM',
    ceo_asl: 'FJASL-ZZ',
    regional_asl: 'FJASL-ZZ',
    engineer_asl: 'FJASL-ZZ'
  }
  form.companyCode = companyCodeMap[name] || ''
  form.username = name
  form.password = '123456'
}

async function quickLogin(name) {
  fill(name)
  await submit()
}

onMounted(async () => {
  try {
    const res = await request.get('/public/features')
    demoEnabled.value = !!res.data?.demoLoginEnabled
  } catch (e) {
    demoEnabled.value = false
  }
})

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    if (form.companyCode) {
      form.companyCode = form.companyCode.trim().toUpperCase()
      localStorage.setItem('laminar_company_code', form.companyCode)
    } else {
      localStorage.removeItem('laminar_company_code')
    }
    await store.login(form)
    await store.getInfo()
    addDynamicRoutes(store.menus)
    if (store.mustChangePassword) {
      ElMessage.warning('请先修改初始密码')
      router.replace('/profile?force=1')
      return
    }
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/dashboard'
    router.replace(String(redirect))
  } catch (e) {
    // 错误已由请求拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: flex; background: #fff; }
.brand-panel {
  width: 56%; min-width: 0; padding: 42px 56px; color: #fff; display: flex; flex-direction: column;
  background: radial-gradient(1200px 600px at 10% 10%, #1d4ed8 0%, transparent 55%),
              radial-gradient(900px 500px at 90% 90%, #0891b2 0%, transparent 50%),
              linear-gradient(135deg, #0b1728 0%, #0f2744 60%, #0b3b52 100%);
  position: relative; overflow: hidden;
}
.brand-panel::after {
  content: ''; position: absolute; inset: 0;
  background-image: linear-gradient(rgba(255,255,255,.04) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.04) 1px, transparent 1px);
  background-size: 42px 42px; pointer-events: none;
}
.brand-top { display: flex; gap: 14px; align-items: center; position: relative; z-index: 1; }
.brand-mark { width: 44px; height: 44px; border-radius: 12px; background: linear-gradient(135deg, #3b82f6, #06b6d4); display: flex; align-items: center; justify-content: center; font-weight: 800; }
.brand-name { font-size: 17px; font-weight: 700; letter-spacing: .5px; }
.brand-en { font-size: 11px; color: #7f9ab8; margin-top: 4px; letter-spacing: .3px; }
.brand-body { margin-top: auto; margin-bottom: auto; position: relative; z-index: 1; max-width: 620px; }
.brand-body h1 { font-size: 38px; line-height: 1.3; margin: 0 0 18px; font-weight: 700; letter-spacing: 1px; }
.brand-body h1 span { color: #5eead4; }
.brand-body > p { color: #9fb3c8; margin: 0 0 26px; font-size: 14px; }
.brand-body ul { list-style: none; padding: 0; margin: 0; display: grid; gap: 14px; }
.brand-body li { display: flex; align-items: center; gap: 10px; color: #cfdae6; font-size: 13px; }
.brand-body li .el-icon { color: #5eead4; font-size: 18px; }
.brand-stats { display: flex; gap: 46px; position: relative; z-index: 1; }
.brand-stats div { display: flex; flex-direction: column; }
.brand-stats b { font-size: 26px; color: #5eead4; }
.brand-stats span { font-size: 12px; color: #8298b0; margin-top: 2px; }

.form-panel { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px; }
.form-box { width: 100%; max-width: 380px; }
.form-head h2 { font-size: 26px; margin: 0 0 8px; }
.form-head p { color: #7a8ba0; margin: 0 0 28px; font-size: 13px; }
.submit-btn { width: 100%; height: 44px; font-size: 15px; margin-top: 4px; border-radius: 8px; }
.login-tip { margin-top: 10px; text-align: center; font-size: 12px; color: #93a1b5; }
.demo { margin-top: 30px; padding-top: 18px; border-top: 1px dashed #e4e9f0; }
.demo-title { font-size: 12px; color: #93a1b5; margin-bottom: 10px; }
.demo-list { display: flex; flex-wrap: wrap; gap: 8px; }
.demo-list span { font-size: 12px; color: #46617f; background: #f2f6fb; border: 1px solid #e2eaf4; border-radius: 20px; padding: 5px 12px; cursor: pointer; }
.demo-list span:hover { color: var(--brand); border-color: #b9d2f5; background: #eef5ff; }
.copyright { position: absolute; bottom: 22px; font-size: 11px; color: #a7b4c5; }

@media (max-width: 980px) {
  .brand-panel { display: none; }
  .form-panel { flex: 1; }
}
</style>
