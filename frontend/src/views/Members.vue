<template>
  <div>
    <div class="toolbar">
      <el-input v-model="kw" placeholder="搜索姓名/部门" clearable style="width:220px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <span class="spacer" />
      <el-button type="success" @click="openCreate">新增成员</el-button>
      <el-upload :show-file-list="false" :before-upload="importMembers" accept=".xlsx,.xls">
        <el-button>导入成员清单</el-button>
      </el-upload>
      <el-button @click="downloadTpl">下载模板</el-button>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="username" label="登录名" />
      <el-table-column prop="department" label="部门" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button text :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="editing ? '编辑成员' : '新增成员'" width="440px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="登录名"><el-input v-model="form.username" :disabled="editing" placeholder="字母+数字，最长26位" /></el-form-item>
        <el-form-item label="部门"><el-input v-model="form.department" /></el-form-item>
        <el-form-item v-if="!editing" label="初始密码">
          <el-input v-model="form.initPassword" placeholder="留空则使用默认 Abc_123456" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'

const rows = ref([])
const kw = ref('')
const visible = ref(false)
const editing = ref(false)
const saving = ref(false)
const form = ref({ name: '', username: '', department: '', initPassword: '' })

onMounted(load)

async function load() {
  rows.value = await http.get('/api/members', { params: { name: kw.value, department: kw.value } }) || []
}

function openCreate() {
  editing.value = false
  form.value = { name: '', username: '', department: '', initPassword: '' }
  visible.value = true
}
function openEdit(row) {
  editing.value = true
  editId = row.id
  form.value = { name: row.name, username: row.username, department: row.department, initPassword: '' }
  visible.value = true
}
async function save() {
  if (!form.value.name || !form.value.username) return ElMessage.warning('姓名与登录名必填')
  saving.value = true
  try {
    if (editing.value) await http.put('/api/members/' + editId, form.value)
    else await http.post('/api/members', form.value)
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
let editId = null

async function toggleStatus(row) {
  await ElMessageBox.confirm(row.status === 1 ? '确认禁用该成员？' : '确认启用该成员？', '提示', { type: 'warning' })
  await http.post('/api/members/' + row.id + '/status', { status: row.status === 1 ? 0 : 1 })
  await load()
}

async function importMembers(file) {
  const fd = new FormData()
  fd.append('file', file)
  const res = await http.post('/api/import/members', fd)
  if (res.success) ElMessage.success(`导入成功，共 ${res.count} 条`)
  else ElMessage.error('导入失败：' + JSON.stringify(res.errors))
  await load()
  return false
}

function downloadTpl() {
  const header = '姓名,登录名,部门,初始密码'
  const sample = '张三,zhangsan,研发部,Abc_123456'
  const blob = new Blob([header + '\n' + sample], { type: 'text/csv' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '成员清单模板.csv'
  a.click()
}
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.spacer { flex: 1; }
</style>
