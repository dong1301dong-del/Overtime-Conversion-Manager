<template>
  <div>
    <div class="toolbar">
      <el-select v-model="filterMember" filterable clearable placeholder="按成员筛选" style="width:200px" @change="load">
        <el-option v-for="m in members" :key="m.id" :label="m.name" :value="m.id" />
      </el-select>
      <el-date-picker v-model="filterMonth" type="month" value-format="YYYY-MM" placeholder="按月份" @change="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <span class="spacer" />
      <el-button type="success" @click="openCreate">录入使用</el-button>
      <el-upload :show-file-list="false" :before-upload="importUsage" accept=".xlsx,.xls">
        <el-button>导入使用记录</el-button>
      </el-upload>
      <el-button @click="downloadTpl">下载模板</el-button>
    </div>

    <el-table :data="rows" border stripe class="mt">
      <el-table-column prop="memberId" label="成员ID" width="90" />
      <el-table-column prop="useStart" label="开始日期" width="120" />
      <el-table-column prop="useEnd" label="结束日期" width="120" />
      <el-table-column label="模式" width="90">
        <template #default="{ row }">{{ row.mode === 1 ? '小时(A)' : '天数(B/C)' }}</template>
      </el-table-column>
      <el-table-column prop="days" label="天数" width="80" />
      <el-table-column prop="hours" label="调休时长(h)" width="120" />
      <el-table-column label="透支" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.isOverdraft === 1" type="danger" size="small">透支</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }"><el-button text type="danger" @click="remove(row)">删除</el-button></template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="录入调休使用" width="460px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="成员">
          <el-select v-model="form.memberId" filterable style="width:100%">
            <el-option v-for="m in members" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="模式">
          <el-radio-group v-model="form.modeText">
            <el-radio value="hour">小时(A)</el-radio>
            <el-radio value="day">天数(B/C)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="开始日期"><el-date-picker v-model="form.useStart" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="结束日期"><el-date-picker v-model="form.useEnd" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item :label="form.modeText === 'hour' ? '使用小时数' : '使用天数'">
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="form.modeText === 'hour' ? 0.5 : 0.5" style="width:100%" />
        </el-form-item>
        <el-alert v-if="preview" :closable="false" type="warning" :title="'预计消耗调休：' + preview + ' h'" />
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'

const rows = ref([])
const members = ref([])
const filterMember = ref(null)
const filterMonth = ref('')
const visible = ref(false)
const saving = ref(false)
const form = ref({ memberId: null, modeText: 'hour', useStart: '', useEnd: '', amount: 0, remark: '' })

const preview = computed(() => {
  if (!form.value.amount) return 0
  return form.value.modeText === 'hour' ? form.value.amount : (form.value.amount * 7.5).toFixed(2)
})

onMounted(async () => {
  members.value = await http.get('/api/members') || []
  await load()
})

async function load() {
  const params = {}
  if (filterMember.value) params.memberId = filterMember.value
  if (filterMonth.value) params.month = filterMonth.value
  rows.value = await http.get('/api/comp-usage', { params }) || []
}
function openCreate() {
  form.value = { memberId: null, modeText: 'hour', useStart: '', useEnd: '', amount: 0, remark: '' }
  visible.value = true
}
async function save() {
  if (!form.value.memberId || !form.value.useStart) return ElMessage.warning('请选择成员与开始日期')
  const payload = {
    memberId: form.value.memberId,
    useStart: form.value.useStart,
    useEnd: form.value.useEnd || form.value.useStart,
    mode: form.value.modeText === 'hour' ? 1 : 2,
    hoursOrDays: form.value.amount,
    remark: form.value.remark
  }
  saving.value = true
  try {
    await http.post('/api/comp-usage', payload)
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm('确认删除该使用记录？', '提示', { type: 'warning' })
  await http.delete('/api/comp-usage/' + row.id)
  await load()
}
async function importUsage(file) {
  const fd = new FormData(); fd.append('file', file)
  const res = await http.post('/api/import/comp-usage', fd)
  if (res.success) ElMessage.success(`导入成功，共 ${res.count} 条`)
  else ElMessage.error('导入失败：' + JSON.stringify(res.errors))
  await load()
  return false
}
function downloadTpl() {
  const header = '姓名,开始日期,结束日期,模式,时长,备注'
  const sample = '张三,2026-02-01,2026-02-01,天数,1,事假调休'
  const blob = new Blob([header + '\n' + sample], { type: 'text/csv' })
  const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = '调休使用记录模板.csv'; a.click()
}
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; }
.spacer { flex: 1; }
.mt { margin-top: 14px; }
</style>
