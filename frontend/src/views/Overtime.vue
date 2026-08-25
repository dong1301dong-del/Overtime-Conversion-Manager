<template>
  <div>
    <div class="toolbar">
      <el-date-picker v-model="month" type="month" value-format="YYYY-MM" placeholder="选择月份" @change="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <span class="spacer" />
      <el-button type="success" @click="openCreate">手动录入</el-button>
      <el-upload :show-file-list="false" :before-upload="importOvertime" accept=".xlsx,.xls">
        <el-button>导入月度核算</el-button>
      </el-upload>
      <el-button @click="downloadTpl">下载模板</el-button>
    </div>

    <el-alert v-if="agg" class="mt" type="info" :closable="false" show-icon>
      <template #title>
        本月共 {{ agg.count }} 条记录 ｜ 有效加班 {{ agg.totalValidHours }} h ｜ 折算产生调休 {{ agg.totalCompHours }} h
      </template>
    </el-alert>

    <el-table :data="rows" border stripe class="mt">
      <el-table-column prop="memberId" label="成员ID" width="90" />
      <el-table-column prop="overtimeDate" label="加班日期" width="120" />
      <el-table-column prop="weekday" label="星期" width="90" />
      <el-table-column prop="validPeriod" label="有效时段" width="110" />
      <el-table-column prop="type" label="类型" width="110" />
      <el-table-column prop="ratio" label="折算比例" width="90" />
      <el-table-column prop="validHours" label="有效时长(h)" width="110" />
      <el-table-column prop="compHours" label="产生调休(h)" width="120" />
      <el-table-column prop="punchTime" label="打卡时间" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="手动录入加班记录" width="460px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="成员">
          <el-select v-model="form.memberId" filterable placeholder="选择成员" style="width:100%">
            <el-option v-for="m in members" :key="m.id" :label="m.name + '(' + m.username + ')'" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="加班日期"><el-date-picker v-model="form.date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="有效时段"><el-input v-model="form.validPeriod" placeholder="如 18:00-21:00" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option label="工作日（1:0.5）" value="工作日" />
            <el-option label="周末（1:1）" value="周末" />
            <el-option label="法定节假日（1:1）" value="法定节假日" />
          </el-select>
        </el-form-item>
        <el-form-item label="有效时长(h)"><el-input-number v-model="form.validHours" :min="0" :precision="2" :step="0.5" style="width:100%" /></el-form-item>
        <el-form-item label="打卡时间"><el-input v-model="form.punchTime" placeholder="选填" /></el-form-item>
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
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'

const route = useRoute()
const now = new Date()
const month = ref(route.query.month || `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`)
const rows = ref([])
const agg = ref(null)
const members = ref([])
const visible = ref(false)
const saving = ref(false)
const form = ref({ memberId: null, date: '', validPeriod: '', type: '工作日', validHours: 0, punchTime: '', remark: '' })

onMounted(async () => {
  members.value = await http.get('/api/members') || []
  await load()
})

async function load() {
  if (!month.value) return
  const data = await http.get('/api/overtime/month', { params: { month: month.value } })
  rows.value = data?.records || []
  agg.value = { count: data?.count, totalValidHours: data?.totalValidHours, totalCompHours: data?.totalCompHours }
}

function openCreate() {
  form.value = { memberId: null, date: '', validPeriod: '', type: '工作日', validHours: 0, punchTime: '', remark: '' }
  visible.value = true
}
async function save() {
  if (!form.value.memberId || !form.value.date) return ElMessage.warning('请选择成员与日期')
  saving.value = true
  try {
    await http.post('/api/overtime', form.value)
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm('确认删除该加班记录？', '提示', { type: 'warning' })
  await http.delete('/api/overtime/' + row.id)
  await load()
}
async function importOvertime(file) {
  const fd = new FormData(); fd.append('file', file)
  const res = await http.post('/api/import/overtime', fd)
  if (res.success) ElMessage.success(`导入成功，共 ${res.count} 条`)
  else ElMessage.error('导入失败：' + JSON.stringify(res.errors))
  await load()
  return false
}
function downloadTpl() {
  const header = '姓名,加班日期,星期,有效时段,类型,折算比例,有效加班时长,产生调休时长,打卡时间,备注'
  const sample = '张三,2026-01-15,星期四,18:00-21:00,工作日,0.5,3,1.5,18:05-21:10,项目支持'
  const blob = new Blob([header + '\n' + sample], { type: 'text/csv' })
  const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = '月度加班转调休-详表模板.csv'; a.click()
}
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; }
.spacer { flex: 1; }
.mt { margin-top: 14px; }
</style>
