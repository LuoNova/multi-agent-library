<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const cards = [
  { title: '借书', desc: '发起借书 / 跨馆协商', path: '/u/borrow' },
  { title: '还书', desc: '归还副本', path: '/u/return' },
  { title: '取书', desc: '到馆确认取书', path: '/u/pickup' },
  { title: '借阅记录', desc: '历史借阅流水与状态', path: '/u/borrow-history' },
  { title: '我的调拨', desc: '调拨进度与我的调拨列表', path: '/u/transfer' },
  { title: '座位', desc: '可用查询与预约', path: '/u/seat' },
  { title: '故障报修', desc: '提交工单', path: '/u/fault' },
]
</script>

<template>
  <div>
    <h1 class="page-heading">读者服务</h1>
    <el-row :gutter="16" class="card-row">
      <el-col v-for="c in cards" :key="c.path" :span="8" style="margin-bottom: 16px">
        <el-card shadow="hover" class="card" @click="router.push(c.path)">
          <h3>{{ c.title }}</h3>
          <p class="desc">{{ c.desc }}</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page-heading {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.card-row {
  margin-top: 4px;
}
.card {
  cursor: pointer;
  min-height: 100px;
}
.card:hover {
  border-color: #409eff;
}
.desc {
  color: #909399;
  font-size: 14px;
  margin: 8px 0 0;
}
h3 {
  margin: 0;
  font-size: 16px;
}
</style>
