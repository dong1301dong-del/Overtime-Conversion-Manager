<template>
  <div>
    <div class="toolbar">
      <el-select v-model="filterMember" filterable clearable placeholder="按成员筛选" style="width:200px" @change="load">
        <el-option v-for="m in members" :key="m.id" :label="m.name" :value="m.id" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <span class="spacer" />
      <el-button type="success" @click="openCreate">录入其他调休</el-button>
    </div>

    <el-table :data="rows" border stripe class="mt">
      <el-table-column prop="memberId" label="成员ID" width="90" />
      <el-table-column prop="date" label="日期" width="120" />
      <el-table-column prop="category" label="来源" width="120" />
      <el-table-column prop="hours" label="时长(h)" width="100" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }"><el-button text type="danger" @click="remove(row)">删除</el-button></template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="录入其他调休（奖励/福利）" width="440px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="成员">
          <el-select v-model="form.memberId" filterable style="width:100%">
            <el-option v-for="m in members" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="form.date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="来源类别">
          <el-select v-model="form.category" style="width:100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="时长(h)"><el-input-number v-model="form.hours" :min="0" :precision="2" :step="0.5" style="width:100%" /></el-form-item>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'

const categories = ['项目奖励', '领导奖励', '公司福利', '其他']
const rows = ref([])
const members = ref([])
const filterMember = ref(null)
const visible = ref(false)
const saving = ref(false)
const form = ref({ memberId: null, date: '', category: '项目奖励', hours: 0, remark: '' })

onMounted(async () => {
  members.value = await http.get('/api/members') || []
  await load()
})
async function load() {
  const params = {}
  if (filterMember.value) params.memberId = filterMember.value
  rows.value = await http.get('/api/adjustment', { params }) || []
}
function openCreate() {
  form.value = { memberId: null, date: '', category: '项目奖励', hours: 0, remark: '' }
  visible.value = true
}
async function save() {
  if (!form.value.memberId || !form.value.date) return ElMessage.warning('请选择成员与日期')
  saving.value = true
  try {
    await http.post('/api/adjustment', form.value)
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function remove(row) {
  await ElMessageBox.confirm('确认删除该记录？', '提示', { type: 'warning' })
  await http.delete('/api/adjustment/' + row.id)
  await load()
}
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 10px; }
.spacer { flex: 1; }
.mt { margin-top: 14px; }
</style>
