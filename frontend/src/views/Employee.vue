<template>
  <div class="emp">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>我的信息</template>
          <p><b>姓名：</b>{{ info?.name }}</p>
          <p><b>部门：</b>{{ info?.department }}</p>
          <p><b>登录名：</b>{{ info?.username }}</p>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>调休余额</template>
          <el-row>
            <el-col :span="6" class="bal"><div class="num">{{ balance?.compTotal }}</div><div>产生(h)</div></el-col>
            <el-col :span="6" class="bal"><div class="num">{{ balance?.adjustTotal }}</div><div>其他(h)</div></el-col>
            <el-col :span="6" class="bal"><div class="num">{{ balance?.usageTotal }}</div><div>使用(h)</div></el-col>
            <el-col :span="6" class="bal"><div class="num" :class="{ danger: (balance?.remaining||0) < 0 }">{{ balance?.remaining }}</div><div>剩余(h)</div></el-col>
          </el-row>
          <el-alert v-if="balance?.overdraft" type="error" :closable="false" title="您的调休已透支，请关注" class="mt" />
        </el-card>
      </el-col>
    </el-row>

    <el-card class="mt" shadow="never">
      <template #header>
        <span>我的月度加班（{{ month }}）</span>
        <el-date-picker v-model="month" type="month" value-format="YYYY-MM" @change="load" style="float:right" />
      </template>
      <el-table :data="overtime" border stripe size="small">
        <el-table-column prop="overtimeDate" label="日期" width="120" />
        <el-table-column prop="type" label="类型" width="110" />
        <el-table-column prop="validHours" label="有效(h)" width="90" />
        <el-table-column prop="compHours" label="产生(h)" width="90" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-card>

    <el-card class="mt" shadow="never">
      <template #header>我的调休使用记录</template>
      <el-table :data="usage" border stripe size="small">
        <el-table-column prop="useStart" label="开始" width="120" />
        <el-table-column prop="useEnd" label="结束" width="120" />
        <el-table-column label="模式" width="90">
          <template #default="{ row }">{{ row.mode === 1 ? '小时' : '天数' }}</template>
        </el-table-column>
        <el-table-column prop="hours" label="时长(h)" width="90" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-card>

    <el-button class="mt" type="primary" @click="exportPdf">导出 / 打印（PDF）</el-button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../api'

const info = ref(null)
const balance = ref(null)
const overtime = ref([])
const usage = ref([])
const now = new Date()
const month = ref(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`)

onMounted(load)
async function load() {
  const data = await http.get('/api/employee/me', { params: { month: month.value } })
  info.value = data?.info
  balance.value = data?.balance
  overtime.value = data?.overtime || []
  usage.value = data?.usage || []
}
function exportPdf() {
  window.print()
}
</script>

<style scoped>
.bal { text-align: center; color: #666; }
.bal .num { font-size: 24px; font-weight: 700; color: #1f2a44; }
.bal .num.danger { color: #f56c6c; }
.mt { margin-top: 14px; }
.danger { color: #f56c6c; }
</style>
