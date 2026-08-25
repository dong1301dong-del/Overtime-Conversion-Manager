<template>
  <div>
    <el-tabs v-model="tab">
      <el-tab-pane label="部门维度" name="dept">
        <el-row :gutter="16">
          <el-col :span="8" v-for="d in deptRows" :key="d.department">
            <el-card shadow="hover" class="dept-card">
              <div class="d-name">{{ d.department }}</div>
              <div class="d-row"><span>成员数</span><b>{{ d.memberCount }}</b></div>
              <div class="d-row"><span>产生(h)</span><b>{{ d.compTotal }}</b></div>
              <div class="d-row"><span>其他调休(h)</span><b>{{ d.adjustTotal }}</b></div>
              <div class="d-row"><span>使用(h)</span><b>{{ d.usageTotal }}</b></div>
              <div class="d-row remain"><span>剩余(h)</span><b :class="{ danger: d.remaining < 0 }">{{ d.remaining }}</b></div>
              <div class="d-row" v-if="d.overdraftCount"><span>透支人数</span><b class="danger">{{ d.overdraftCount }}</b></div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="全员余额" name="all">
        <el-table :data="allRows" border stripe>
          <el-table-column prop="name" label="姓名" />
          <el-table-column prop="department" label="部门" />
          <el-table-column prop="compTotal" label="产生(h)" />
          <el-table-column prop="adjustTotal" label="其他(h)" />
          <el-table-column prop="usageTotal" label="使用(h)" />
          <el-table-column label="剩余(h)">
            <template #default="{ row }">
              <span :class="{ danger: row.remaining < 0 }">{{ row.remaining }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.remaining < 0" type="danger" size="small">透支</el-tag>
              <el-tag v-else type="success" size="small">正常</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../api'

const tab = ref('dept')
const deptRows = ref([])
const allRows = ref([])

onMounted(load)
async function load() {
  deptRows.value = await http.get('/api/balance/department') || []
  allRows.value = await http.get('/api/balance/all') || []
}
</script>

<style scoped>
.dept-card .d-name { font-weight: 700; font-size: 16px; margin-bottom: 10px; }
.dept-card .d-row { display: flex; justify-content: space-between; padding: 4px 0; color: #555; font-size: 14px; }
.dept-card .d-row.remain { border-top: 1px dashed #eee; margin-top: 6px; padding-top: 8px; }
.danger { color: #f56c6c; }
</style>
