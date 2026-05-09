<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const cards = [
  { title: '调拨与库存', desc: '到货确认、全量调拨、调拨建议审批、库存分析触发', path: '/a/transfer' },
  { title: '故障工单', desc: '列表、详情、状态更新、资源健康查询', path: '/a/fault' },
  { title: '设备状态', desc: '修改设备运行状态', path: '/a/equipment' },
  { title: '定时任务', desc: '手动触发后端定时任务（调试）', path: '/a/dev-tasks' },
]
</script>

<template>
  <div>
    <h1 class="page-heading">馆员 / 运维管理</h1>
    <el-alert
      title="以下功能对应管理类接口与运维场景，请勿与普通读者自助流程混用。"
      type="info"
      show-icon
      :closable="false"
      style="margin-top: 16px"
    />
    <el-row :gutter="16" style="margin-top: 20px">
      <el-col v-for="c in cards" :key="c.path" :span="6" style="margin-bottom: 16px">
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
.card {
  cursor: pointer;
  min-height: 120px;
}
.card:hover {
  border-color: #e6a23c;
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
