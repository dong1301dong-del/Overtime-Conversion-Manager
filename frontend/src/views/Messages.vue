<template>
  <div>
    <el-table :data="rows" border stripe>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isRead === 1 ? 'info' : 'danger'" size="small">{{ row.isRead === 1 ? '已读' : '未读' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="级别" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.level >= 2" type="warning" size="small">重要</el-tag>
          <span v-else>普通</span>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="120" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="createdAt" label="时间" width="170" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button v-if="row.isRead !== 1" text type="primary" @click="markRead(row)">标记已读</el-button>
          <el-button text type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'
import { auth } from '../store'

const rows = ref([])
const isAdmin = auth.user?.role === 'ADMIN' || auth.user?.role === 'CLERK'

onMounted(load)
async function load() {
  rows.value = await http.get('/api/messages') || []
}
async function markRead(row) {
  await http.post('/api/messages/' + row.id + '/read')
  await load()
}
async function remove(row) {
  if (!isAdmin) return ElMessage.warning('仅管理员/录入员可删除消息')
  await ElMessageBox.confirm('确认删除该消息？', '提示', { type: 'warning' })
  await http.delete('/api/messages/' + row.id)
  await load()
}
</script>
