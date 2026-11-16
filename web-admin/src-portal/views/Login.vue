<template>
  <div class="login">
    <div class="panel">
      <div class="head">
        <div class="mark">
          <img v-if="platform.logo" :src="platform.logo" alt="logo" />
          <span v-else>医</span>
        </div>
        <div><b>{{ platform.name }}</b><span>院方端 · 仅限本院数据</span></div>
      </div>
      <el-alert v-if="platform.maintenance" :title="platform.message" type="warning" :closable="false" show-icon style="margin-bottom: 16px" />
      <el-form :model="form" size="large" @keyup.enter="submit">
        <el-form-item><el-input v-model="form.username" placeholder="院方账号" :prefix-icon="User" /></el-form-item>
        <el-form-item><el-input v-model="form.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" /></el-form-item>
        <el-form-item v-if="captcha.enabled">
          <div class="captcha-row">
            <el-input v-model="form.code" placeholder="验证码" maxlength="4" />
            <img :src="captcha.img" class="captcha-img" alt="验证码" title="点击刷新" @click="loadCaptcha" />
          </div>
        </el-form-item>
        <el-button type="primary" class="btn" :loading="loading" @click="submit">登 录</el-button>
      </el-form>
      <div class="tip">院方账号仅可查看本院设备、告警与工单</div>
    </div>

    <el-dialog
      v-model="pwdVisible"
      title="首次登录，请修改密码"
      width="420px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <el-alert title="为了账号安全，系统初始密码必须先修改后才能进入院方端" type="warning" :closable="false" show-icon style="margin-bottom: 16px" />
      <el-form :model="pwdForm" label-width="88px">
        <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 6 位" /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="pwdForm.confirmPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="savingPwd" @click="savePassword">修改并进入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from '@/request'
import { store } from '@/store'

const router = useRouter()
const loading = ref(false)
const savingPwd = ref(false)
const pwdVisible = ref(false)
const form = reactive({ username: '', password: '', uuid: '', code: '' })
const captcha = reactive({ enabled: false, uuid: '', img: '' })
const platform = reactive({ name: '医院洁净设备运维协同平台', logo: '', maintenance: false, message: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

async function loadCaptcha() {
  try {
    const res = await request.get('/auth/captcha')
    captcha.enabled = !!res.data?.enabled
    captcha.uuid = res.data?.uuid || ''
    captcha.img = res.data?.img || ''
    form.uuid = captcha.uuid
    form.code = ''
  } catch (e) {
    captcha.enabled = false
  }
}

onMounted(loadCaptcha)

onMounted(async () => {
  try {
    const res = await request.get('/public/features')
    platform.name = res.data?.platformName || '医院洁净设备运维协同平台'
    platform.logo = res.data?.platformLogo || ''
    platform.maintenance = !!res.data?.maintenanceEnabled
    platform.message = res.data?.maintenanceMessage || ''
  } catch (e) { /* ignore */ }
})

async function submit() {
  if (captcha.enabled && !form.code) {
    ElMessage.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    const res = await request.post('/auth/login', form)
    store.token = res.data.token
    localStorage.setItem('portal_token', res.data.token)
    const info = await request.get('/auth/info')
    if (info.data.user?.userType !== '2') {
      localStorage.removeItem('portal_token')
      store.token = ''
      ElMessage.error('请使用院方账号登录')
      return
    }
    if (res.data.mustChangePassword) {
      pwdForm.oldPassword = form.password
      pwdForm.newPassword = ''
      pwdForm.confirmPassword = ''
      pwdVisible.value = true
      return
    }
    await finishLogin()
  } finally {
    loading.value = false
  }
}

async function savePassword() {
  if (!pwdForm.oldPassword) { ElMessage.warning('请填写原密码'); return }
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) { ElMessage.warning('新密码至少 6 位'); return }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) { ElMessage.warning('两次输入的密码不一致'); return }
  savingPwd.value = true
  try {
    await request.put('/auth/password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码已修改')
    pwdVisible.value = false
    await finishLogin()
  } finally {
    savingPwd.value = false
  }
}

async function finishLogin() {
  store.profile = (await request.get('/portal/profile')).data
  router.replace('/dashboard')
}
</script>

<style scoped>
.captcha-row { display: flex; align-items: center; gap: 10px; width: 100%; }
.captcha-row :deep(.el-input) { flex: 1; }
.captcha-img { width: 110px; height: 40px; border-radius: 6px; border: 1px solid #e2e8f0; cursor: pointer; }
.login { height: 100vh; display: flex; align-items: center; justify-content: center; background: radial-gradient(800px 400px at 20% 10%, #0f766e, transparent 60%), linear-gradient(135deg, #0b2a2a, #0d3b38 60%, #115e59); }
.panel { width: 400px; background: #fff; border-radius: 16px; padding: 32px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.head { display: flex; gap: 12px; align-items: center; margin-bottom: 26px; }
.mark { width: 42px; height: 42px; border-radius: 12px; background: linear-gradient(135deg, #0f766e, #14b8a6); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; overflow: hidden; }
.mark img { width: 100%; height: 100%; object-fit: contain; background: #fff; }
.head b { display: block; font-size: 16px; } .head span { font-size: 11px; color: #8494a8; }
.btn { width: 100%; height: 44px; margin-top: 6px; }
.tip { margin-top: 22px; font-size: 11px; color: #94a3b8; line-height: 1.8; }
</style>
