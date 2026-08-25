<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="c in cards" :key="c.label">
        <el-card shadow="hover" class="stat-card">
          <div class="num" :class="{ danger: c.danger }">{{ c.value }}</div>
          <div class="label">{{ c.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="mt" shadow="never">
      <template #header>
        <span>历史月份（点击查看该月核算）</span>
      </template>
      <el-tag v-for="m in months" :key="m" class="month" type="primary" effect="plain" @click="openMonth(m)">{{ m }}</el-tag>
      <el-empty v-if="!months.length" description="暂无核算数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api'

const router = useRouter()
const data = ref({})
const months = ref([])
const cards = ref([])

onMounted(async () => {
  data.value = await http.get('/api/dashboard') || {}
  months.value = data.value.months || []
  cards.value = [
    { label: '成员总数', value: data.value.totalMembers ?? 0 },
    { label: '累计产生调休(h)', value: data.value.totalCompHours ?? 0 },
    { label: '累计使用调休(h)', value: data.value.totalUsageHours ?? 0 },
    { label: '当前剩余(h)', value: data.value.totalRemaining ?? 0, danger: (data.value.totalRemaining ?? 0) < 0 },
    { label: '透支人数', value: data.value.overdraftCount ?? 0, danger: (data.value.overdraftCount ?? 0) > 0 }
  ]
})

function openMonth(m) {
  router.push({ path: '/overtime', query: { month: m } })
}
</script>

<style scoped>
.stat-card .num { font-size: 26px; font-weight: 700; color: #1f2a44; }
.stat-card .num.danger { color: #f56c6c; }
.stat-card .label { color: #888; font-size: 13px; margin-top: 4px; }
.mt { margin-top: 16px; }
.month { cursor: pointer; margin: 0 10px 10px 0; }
</style>
