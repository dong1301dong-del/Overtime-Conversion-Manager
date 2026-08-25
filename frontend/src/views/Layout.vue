<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="aside-logo">LYNN 加班调休</div>
      <el-menu :default-active="activeMenu" router class="menu" background-color="#1f2a44" text-color="#c5cde0" active-text-color="#fff">
        <el-menu-item v-for="m in menus" :key="m.index" :index="m.index">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="crumb">{{ currentTitle }}</span>
        <div class="right">
          <el-tag v-if="user" :type="roleTag(user.role)" size="small">{{ roleText(user.role) }}</el-tag>
          <span class="who">{{ user?.name || user?.username }}</span>
          <el-button text @click="openPwd">修改密码</el-button>
          <el-button text type="danger" @click="doLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <el-dialog v-model="pwdVisible" title="修改密码" width="420px">
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
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="doChange">确认</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../api'
import { auth } from '../store'

const route = useRoute()
const router = useRouter()
const user = computed(() => auth.user)
const activeMenu = computed(() => '/' + (route.path.split('/')[1] || 'dashboard'))
const currentTitle = computed(() => {
  const map = {
    dashboard: '统计详表', members: '成员管理', overtime: '月度加班核算',
    'comp-usage': '调休使用记录', 'other-comp': '其他调休录入',
    balance: '剩余调休查询', employee: '我的调休', messages: '消息中心', settings: '系统设置'
  }
  return map[route.path.split('/')[1]] || '加班转调休记录工具'
})

const menus = computed(() => {
  const role = user.value?.role
  const base = [
    { index: '/dashboard', title: '统计详表', icon: 'DataLine' },
    { index: '/members', title: '成员管理', icon: 'UserFilled' },
    { index: '/overtime', title: '月度加班核算', icon: 'Calendar' },
    { index: '/comp-usage', title: '调休使用记录', icon: 'Timer' },
    { index: '/other-comp', title: '其他调休录入', icon: 'Gift' },
    { index: '/balance', title: '剩余调休查询', icon: 'Wallet' },
    { index: '/messages', title: '消息中心', icon: 'Bell' },
    { index: '/settings', title: '系统设置', icon: 'Setting' }
  ]
  if (role === 'EMPLOYEE') {
    return [
      { index: '/employee', title: '我的调休', icon: 'User' },
      { index: '/messages', title: '消息中心', icon: 'Bell' }
    ]
  }
  return base
})

function roleText(r) {
  return { ADMIN: '管理员', CLERK: '录入员', READONLY: '只读', EMPLOYEE: '员工自助' }[r] || r
}
function roleTag(r) {
  return { ADMIN: 'danger', CLERK: 'warning', READONLY: 'info', EMPLOYEE: 'success' }[r] || ''
}

function doLogout() {
  http.post('/api/auth/logout').catch(() => {})
  auth.clear()
  window.location.hash = '#/login'
}

const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const pwdRef = ref()
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,20}$/, message: '8-20位，含大写+小写+数字+特殊字符', trigger: 'blur' }
  ],
  confirm: [{ validator: (r, v, cb) => v === pwdForm.newPassword ? cb() : cb(new Error('两次输入不一致')), trigger: 'blur' }]
}
function openPwd() { pwdVisible.value = true }
async function doChange() {
  await pwdRef.value.validate()
  pwdLoading.value = true
  try {
    const data = await http.post('/api/auth/change-password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    auth.user = data
    pwdVisible.value = false
    pwdForm.oldPassword = pwdForm.newPassword = pwdForm.confirm = ''
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style scoped>
.layout { height: 100%; }
.aside { background: #1f2a44; }
.aside-logo { color: #fff; font-weight: 700; padding: 18px 16px; font-size: 16px; letter-spacing: 1px; }
.menu { border-right: none; }
.header { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #ebeef5; }
.crumb { font-weight: 600; }
.right { display: flex; align-items: center; gap: 10px; }
.who { font-size: 14px; color: #333; }
.main { background: #f5f7fa; padding: 18px; }
</style>
