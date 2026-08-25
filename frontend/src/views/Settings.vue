<template>
  <div>
    <el-tabs v-model="tab">
      <el-tab-pane v-if="role === 'ADMIN'" label="账号管理" name="accounts">
        <div class="toolbar">
          <el-button type="success" @click="openCreate">新建账号</el-button>
        </div>
        <el-table :data="accounts" border stripe>
          <el-table-column prop="username" label="登录名" />
          <el-table-column prop="role" label="角色" width="100" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button text type="warning" @click="resetPwd(row)">重置密码</el-button>
              <el-button text :type="row.status === 1 ? 'warning' : 'success'" @click="toggle(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="节假日日历" name="holiday">
        <div class="toolbar">
          <el-date-picker v-model="holidayYear" type="year" value-format="yyyy" @change="loadHolidays" />
          <el-button type="primary" @click="loadHolidays">查询</el-button>
          <el-spacer="spacer" />
          <el-button v-if="role === 'ADMIN'" type="success" @click="openHoliday">新增/修正</el-button>
          <el-button v-if="role === 'ADMIN'" @click="seedYear">播种该年</el-button>
        </div>
        <el-table :data="holidays" border stripe>
          <el-table-column prop="holidayDate" label="日期" width="130" />
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="kind" label="类型" width="100">
            <template #default="{ row }">{{ row.kind === 'HOLIDAY' ? '法定节假日' : '调休工作日' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90" v-if="role === 'ADMIN'">
            <template #default="{ row }"><el-button text type="danger" @click="delHoliday(row)">删除</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="数据备份" name="backup">
        <div class="toolbar">
          <el-button type="primary" :loading="backing" @click="doBackup">立即备份</el-button>
          <span class="hint">每周一自动备份，保留最近 {{ retention }} 份</span>
        </div>
        <el-table :data="backups" border stripe>
          <el-table-column prop="filename" label="文件名" />
          <el-table-column prop="size" label="大小(字节)" width="130" />
          <el-table-column prop="note" label="方式" width="120" />
          <el-table-column prop="createdAt" label="时间" width="180" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane v-if="role === 'ADMIN'" label="系统参数" name="config">
        <el-table :data="configs" border stripe>
          <el-table-column prop="confKey" label="参数键" width="220" />
          <el-table-column prop="confValue" label="值" />
          <el-table-column prop="description" label="说明" />
          <el-table-column label="操作" width="90">
            <template #default="{ row }"><el-button text type="primary" @click="editConfig(row)">修改</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 账号创建 -->
    <el-dialog v-model="accVisible" title="新建账号" width="420px">
      <el-form :model="accForm" label-width="80px">
        <el-form-item label="登录名"><el-input v-model="accForm.username" placeholder="字母+数字" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="accForm.role" style="width:100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="录入员" value="CLERK" />
            <el-option label="只读" value="READONLY" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始密码"><el-input v-model="accForm.password" placeholder="8-20位，含大写+小写+数字+特殊字符" /></el-form-item>
        <el-form-item label="首次改密"><el-switch v-model="accForm.mustChangePwd" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="accVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveAcc">保存</el-button></template>
    </el-dialog>

    <!-- 节假日 -->
    <el-dialog v-model="holVisible" title="新增/修正节假日" width="420px">
      <el-form :model="holForm" label-width="80px">
        <el-form-item label="日期"><el-date-picker v-model="holForm.holidayDate" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="holForm.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="holForm.kind" style="width:100%">
            <el-option label="法定节假日" value="HOLIDAY" />
            <el-option label="调休工作日" value="WORKDAY" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="holVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveHoliday">保存</el-button></template>
    </el-dialog>

    <!-- 参数修改 -->
    <el-dialog v-model="cfgVisible" title="修改参数" width="420px">
      <el-form :model="cfgForm" label-width="80px">
        <el-form-item label="键"><el-input v-model="cfgForm.confKey" disabled /></el-form-item>
        <el-form-item label="值"><el-input v-model="cfgForm.confValue" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="cfgVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveCfg">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'
import { auth } from '../store'

const role = auth.user?.role
const tab = ref('accounts')
const retention = ref(10)

const accounts = ref([])
const accVisible = ref(false)
const saving = ref(false)
const accForm = ref({ username: '', role: 'CLERK', password: '', mustChangePwd: true })

const holidayYear = ref(String(new Date().getFullYear()))
const holidays = ref([])
const holVisible = ref(false)
const holForm = ref({ holidayDate: '', name: '', kind: 'HOLIDAY' })

const backups = ref([])
const backing = ref(false)

const configs = ref([])
const cfgVisible = ref(false)
const cfgForm = ref({ confKey: '', confValue: '' })

onMounted(async () => {
  if (role === 'ADMIN') {
    accounts.value = await http.get('/api/accounts') || []
    configs.value = await http.get('/api/config') || []
    const cfg = configs.value.find(c => c.confKey === 'backup_retention')
    if (cfg) retention.value = cfg.confValue
  }
  await loadHolidays()
  if (role === 'ADMIN' || role === 'CLERK') {
    backups.value = await http.get('/api/config/backups') || []
  }
})

async function loadHolidays() {
  holidays.value = await http.get('/api/config/holidays', { params: { year: holidayYear.value } }) || []
}

function openCreate() {
  accForm.value = { username: '', role: 'CLERK', password: '', mustChangePwd: true }
  accVisible.value = true
}
async function saveAcc() {
  if (!accForm.value.username || !accForm.value.password) return ElMessage.warning('登录名与密码必填')
  saving.value = true
  try {
    await http.post('/api/accounts', accForm.value)
    accVisible.value = false
    accounts.value = await http.get('/api/accounts') || []
  } finally {
    saving.value = false
  }
}
async function toggle(row) {
  await http.post('/api/accounts/' + row.id + '/status', { status: row.status === 1 ? 0 : 1 })
  accounts.value = await http.get('/api/accounts') || []
}
async function resetPwd(row) {
  const pwd = await ElMessageBox.prompt('请输入新密码（8-20位，含大写+小写+数字+特殊字符）', '重置密码', { inputPattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,20}$/ })
  await http.post('/api/accounts/' + row.id + '/reset-password', { newPassword: pwd.value })
  ElMessage.success('密码已重置，对方下次需重新登录')
}

function openHoliday() { holForm.value = { holidayDate: '', name: '', kind: 'HOLIDAY' }; holVisible.value = true }
async function saveHoliday() {
  if (!holForm.value.holidayDate) return ElMessage.warning('请选择日期')
  saving.value = true
  try {
    await http.post('/api/config/holidays', holForm.value)
    holVisible.value = false
    await loadHolidays()
  } finally {
    saving.value = false
  }
}
async function delHoliday(row) {
  await ElMessageBox.confirm('确认删除该日历项？', '提示', { type: 'warning' })
  await http.delete('/api/config/holidays/' + row.id)
  await loadHolidays()
}
async function seedYear() {
  await http.post('/api/config/holidays/seed?year=' + holidayYear.value)
  await loadHolidays()
}

async function doBackup() {
  backing.value = true
  try {
    await http.post('/api/config/backup')
    backups.value = await http.get('/api/config/backups') || []
    ElMessage.success('备份完成')
  } finally {
    backing.value = false
  }
}

function editConfig(row) {
  cfgForm.value = { confKey: row.confKey, confValue: row.confValue }
  cfgVisible.value = true
}
async function saveCfg() {
  saving.value = true
  try {
    await http.post('/api/config', { key: cfgForm.value.confKey, value: cfgForm.value.confValue })
    cfgVisible.value = false
    configs.value = await http.get('/api/config') || []
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.spacer { flex: 1; }
.hint { color: #999; font-size: 13px; }
</style>
