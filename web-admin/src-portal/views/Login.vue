<template>
  <div class="login">
    <div class="panel">
      <div class="head">
        <div class="mark">医</div>
        <div><b>医院洁净设备运维协同平台</b><span>院方端 · 仅限本院数据</span></div>
      </div>
      <el-form :model="form" size="large" @keyup.enter="submit">
        <el-form-item><el-input v-model="form.username" placeholder="院方账号" :prefix-icon="User" /></el-form-item>
        <el-form-item><el-input v-model="form.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" /></el-form-item>
        <el-button type="primary" class="btn" :loading="loading" @click="submit">登 录</el-button>
      </el-form>
      <div class="tip">演示账号：hospital1 / 123456<br />院方账号仅可查看本院设备、告警与工单</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from '@/request'
import { store } from '@/store'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'hospital1', password: '123456' })

async function submit() {
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
    store.profile = (await request.get('/portal/profile')).data
    router.replace('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login { height: 100vh; display: flex; align-items: center; justify-content: center; background: radial-gradient(800px 400px at 20% 10%, #0f766e, transparent 60%), linear-gradient(135deg, #0b2a2a, #0d3b38 60%, #115e59); }
.panel { width: 400px; background: #fff; border-radius: 16px; padding: 32px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.head { display: flex; gap: 12px; align-items: center; margin-bottom: 26px; }
.mark { width: 42px; height: 42px; border-radius: 12px; background: linear-gradient(135deg, #0f766e, #14b8a6); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; }
.head b { display: block; font-size: 16px; } .head span { font-size: 11px; color: #8494a8; }
.btn { width: 100%; height: 44px; margin-top: 6px; }
.tip { margin-top: 22px; font-size: 11px; color: #94a3b8; line-height: 1.8; }
</style>
