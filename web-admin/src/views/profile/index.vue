<template>
  <div class="app-container">
    <div class="page-card profile-page">
      <div class="page-title">
        <div>
          <h2>个人中心</h2>
          <div class="sub">账号信息与个人使用偏好</div>
        </div>
      </div>

      <el-alert
        v-if="forceMode"
        title="首次登录需要修改密码"
        description="新密码需 8-32 位，并同时包含字母和数字。修改完成后才能进入系统。"
        type="warning"
        :closable="false"
        show-icon
        class="force-alert"
      />

      <div class="profile-grid">
        <section class="panel">
          <div class="panel-head">
            <h3>账号信息</h3>
            <span class="panel-sub">由管理员维护</span>
          </div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="账号">{{ user?.username || '—' }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ user?.nickName || '—' }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ store.roleNames.join(' / ') || '—' }}</el-descriptions-item>
            <el-descriptions-item label="所属公司">{{ store.companyName || '平台' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ user?.phone || '—' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ user?.email || '—' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="panel">
          <div class="panel-head">
            <h3>联系方式验证</h3>
            <span class="panel-sub">验证后才能用手机号/邮箱登录</span>
          </div>
          <div v-for="c in contacts" :key="c.type" class="verify-row">
            <div class="verify-main">
              <div class="pref-label">{{ c.label }}</div>
              <el-input
                v-if="!c.verified"
                v-model="targets[c.type]"
                :placeholder="c.type === 'phone' ? '请输入手机号' : '请输入邮箱'"
                clearable
              />
              <div v-else class="pref-desc">{{ c.value }}</div>
            </div>
            <el-tag v-if="c.verified" type="success" size="small">已验证</el-tag>
            <el-tag v-else type="info" size="small">未验证</el-tag>
            <el-button v-if="!c.verified" size="small" @click="sendCode(c.type)">发送验证码</el-button>
            <div v-if="sent[c.type]" class="verify-input">
              <el-input v-model="codes[c.type]" maxlength="6" placeholder="6位验证码" />
              <el-button size="small" type="primary" @click="verifyCode(c.type)">验证</el-button>
            </div>
          </div>
        </section>

        <section v-if="store.engineerId" class="panel">
          <div class="panel-head">
            <h3>工作状态</h3>
            <span class="panel-sub">临时离岗开关</span>
          </div>
          <div class="duty-row">
            <div>
              <div class="pref-label">当前状态</div>
              <div class="pref-desc">
                {{ duty.onDuty ? '在岗，可接收派单' : '已离岗，不接收新派单' }}
              </div>
            </div>
            <el-switch
              :model-value="duty.onDuty"
              :disabled="duty.onDuty && !duty.canOffDuty"
              @change="toggleDuty"
            />
          </div>
          <div class="duty-meta">
            在手单：{{ duty.activeOrders }} 张
          </div>
          <div v-if="duty.onDuty && !duty.canOffDuty" class="duty-warn">
            还有在手单，需先改派后才能离岗
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <h3>列表偏好</h3>
            <span class="panel-sub">应用于所有列表页</span>
          </div>
          <div class="pref-row">
            <div>
              <div class="pref-label">默认每页条数</div>
              <div class="pref-desc">打开列表页时默认显示的记录数量</div>
            </div>
            <el-select v-model="pageSize" style="width: 140px">
              <el-option v-for="n in pageSizeOptions" :key="n" :label="n + ' 条 / 页'" :value="n" />
            </el-select>
          </div>
          <div class="panel-actions">
            <el-button type="primary" :loading="savingPref" @click="savePreference">保存偏好</el-button>
            <span class="hint">保存后重新打开列表页生效</span>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <h3>修改密码</h3>
            <span class="panel-sub">建议定期更换</span>
          </div>
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="88px" class="pwd-form">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingPwd" @click="savePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const store = useUserStore()
const route = useRoute()
const router = useRouter()
const forceMode = ref(store.mustChangePassword || route.query.force === '1')
const user = computed(() => store.user || {})
const contacts = computed(() => [
  { type: 'phone', label: '手机号', value: user.value.phone, verified: Number(user.value.phoneVerified) === 1 },
  { type: 'email', label: '邮箱', value: user.value.email, verified: Number(user.value.emailVerified) === 1 }
])
const sent = reactive({})
const codes = reactive({})
const targets = reactive({ phone: '', email: '' })
const pageSizeOptions = [10, 20, 50, 100]
const pageSize = ref(store.pageSize || 10)
const duty = reactive({ onDuty: true, activeOrders: 0, canOffDuty: true })
const savingPref = ref(false)
const savingPwd = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{
    validator: (rule, value, callback) => {
      if (!value) callback(new Error('请输入新密码'))
      else if (value.length < 8 || value.length > 32) callback(new Error('密码长度需为 8-32 位'))
      else if (!/[A-Za-z]/.test(value) || !/\d/.test(value)) callback(new Error('密码必须同时包含字母和数字'))
      else if (value === pwdForm.oldPassword) callback(new Error('新密码不能与原密码相同'))
      else callback()
    },
    trigger: 'blur'
  }],
  confirmPassword: [{
    validator: (rule, value, callback) => {
      if (!value) callback(new Error('请再次输入新密码'))
      else if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
      else callback()
    },
    trigger: 'blur'
  }]
}

async function loadDuty() {
  if (!store.engineerId) return
  try {
    const res = await request.get('/engineer/me/status')
    Object.assign(duty, res.data || {})
  } catch (e) { /* ignore */ }
}

async function toggleDuty(value) {
  try {
    await request.put('/engineer/me/on-duty', { onDuty: value })
    duty.onDuty = value
    ElMessage.success(value ? '已恢复在岗' : '已切换为离岗')
  } catch (e) {
    await loadDuty()
  }
}

function initTargets() {
  targets.phone = user.value.phone || ''
  targets.email = user.value.email || ''
}

async function sendCode(type) {
  if (!targets[type]) {
    ElMessage.warning(type === 'phone' ? '请输入手机号' : '请输入邮箱')
    return
  }
  try {
    const res = await request.post('/auth/verify/send', { type, target: targets[type] })
    sent[type] = true
    codes[type] = res.data?.debugCode || ''
    if (res.data?.debugCode) {
      ElMessage.success(`开发模式验证码：${res.data.debugCode}`)
    } else {
      ElMessage.success('验证码已发送')
    }
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
  }
}

async function verifyCode(type) {
  if (!codes[type]) { ElMessage.warning('请输入验证码'); return }
  try {
    await request.post('/auth/verify/confirm', { type, code: codes[type], target: targets[type] })
    ElMessage.success('验证成功')
    sent[type] = false
    await store.getInfo()
    initTargets()
  } catch (e) {
    ElMessage.error(e.message || '验证失败')
  }
}

onMounted(() => {
  initTargets()
  loadDuty()
})

async function savePreference() {
  savingPref.value = true
  try {
    await request.put('/auth/preferences', { pageSize: pageSize.value })
    store.pageSize = pageSize.value
    if (store.user) store.user.prefPageSize = pageSize.value
    ElMessage.success('偏好已保存')
  } finally {
    savingPref.value = false
  }
}

async function savePassword() {
  await pwdFormRef.value.validate()
  savingPwd.value = true
  try {
    await request.put('/auth/password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    const wasForced = forceMode.value
    store.mustChangePassword = false
    if (store.user) store.user.mustChangePwd = 0
    forceMode.value = false
    ElMessage.success('密码已修改')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    if (wasForced) router.replace('/dashboard')
  } finally {
    savingPwd.value = false
  }
}
</script>

<style scoped>
.profile-page { max-width: 1080px; }
.force-alert { margin-bottom: 16px; }
.profile-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 4px; }
.panel {
  background: #fff;
  border: 1px solid #e6ebf2;
  border-radius: 10px;
  padding: 18px 20px;
}
.panel:first-child { grid-row: span 2; }
.panel-head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 16px; }
.panel-head h3 { margin: 0; font-size: 15px; font-weight: 600; color: #1f2d3d; }
.panel-sub { font-size: 12px; color: #93a1b5; }
.pref-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 4px 0 8px; }
.pref-label { font-size: 13px; color: #33465c; font-weight: 500; }
.pref-desc { font-size: 12px; color: #8a99ab; margin-top: 3px; }
.panel-actions { display: flex; align-items: center; gap: 12px; margin-top: 10px; }
.duty-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 4px 0 8px; }
.duty-meta { font-size: 12px; color: #8a99ab; }
.duty-warn { margin-top: 8px; font-size: 12px; color: #e6a23c; }
.hint { font-size: 12px; color: #93a1b5; }
.verify-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; padding: 8px 0; border-bottom: 1px solid #f0f3f7; }
.verify-row:last-child { border-bottom: none; }
.verify-main { flex: 1; min-width: 180px; }
.verify-input { display: flex; gap: 6px; align-items: center; width: 100%; }
.verify-input .el-input { width: 150px; }
.pwd-form { max-width: 360px; }
@media (max-width: 900px) {
  .profile-grid { grid-template-columns: 1fr; }
  .panel:first-child { grid-row: auto; }
}
</style>
