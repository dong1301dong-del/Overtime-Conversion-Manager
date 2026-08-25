<template>
  <div class="login-wrap">
    <el-card class="login-card" shadow="always">
      <div class="brand">
        <div class="logo">LYNN</div>
        <h2>加班转调休记录工具</h2>
        <p class="sub">内网加班 / 调休核算与查询平台</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入登录名" @keyup.enter="doLogin" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" @keyup.enter="doLogin" />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="full" @click="doLogin">登录</el-button>
      </el-form>
      <p class="tip">默认管理员账号 admin / Admin@123456（首次登录需修改密码）</p>

      <el-dialog v-model="pwdVisible" title="修改密码" width="420px" :close-on-click-modal="false" :show-close="false">
        <el-form :model="pwdForm" :rules="pwdRules" ref="pwdRef" label-position="top">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="8-20位，含大写+小写+数字+特殊字符" />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirm">
            <el-input v-model="pwdForm.confirm" type="password" show-password />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button type="primary" :loading="pwdLoading" @click="doChange">确认修改</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import http from '../api'
import { auth } from '../store'

const form = reactive({ username: '', password: '' })
const formRef = ref()
const loading = ref(false)
const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const pwdRef = ref()

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,20}$/, message: '8-20位，含大写+小写+数字+特殊字符', trigger: 'blur' }
  ],
  confirm: [{ validator: (r, v, cb) => v === pwdForm.newPassword ? cb() : cb(new Error('两次输入不一致')), trigger: 'blur' }]
}

function goHome(role) {
  window.location.hash = role === 'EMPLOYEE' ? '#/employee' : '#/dashboard'
}

async function doLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await http.post('/api/auth/login', form)
    auth.token = data.token
    auth.user = data
    if (data.mustChangePwd) {
      pwdVisible.value = true
    } else {
      goHome(data.role)
    }
  } finally {
    loading.value = false
  }
}

async function doChange() {
  await pwdRef.value.validate()
  pwdLoading.value = true
  try {
    const data = await http.post('/api/auth/change-password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    auth.token = data.token
    auth.user = data
    pwdVisible.value = false
    goHome(data.role)
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2a44 0%, #3a5a9c 100%);
}
.login-card {
  width: 380px;
  border-radius: 12px;
}
.brand { text-align: center; margin-bottom: 18px; }
.logo {
  width: 56px; height: 56px; line-height: 56px; margin: 0 auto 10px;
  background: #3a5a9c; color: #fff; border-radius: 12px; font-weight: 700; letter-spacing: 1px;
}
.brand h2 { margin: 0; font-size: 20px; }
.sub { color: #888; font-size: 12px; margin: 6px 0 0; }
.full { width: 100%; }
.tip { color: #999; font-size: 12px; text-align: center; margin-top: 12px; }
</style>
