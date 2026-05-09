<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="portal-page">
    <div class="portal">
      <h1 class="page-heading">系统入口</h1>
      <p class="lead">
        请先选择<strong>读者服务</strong>或<strong>馆员/运维管理</strong>，进入对应登录页后再使用功能。两端会话<strong>互不通用</strong>：在读者端登录后无法直接打开管理端页面，反之亦然；切换端别请先退出登录并重新选择。
      </p>
      <el-row :gutter="20" class="cards">
        <el-col :xs="24" :sm="12">
          <el-card shadow="hover" class="card user" @click="router.push({ name: 'loginUser' })">
            <h2>读者服务</h2>
            <p>借还书、取书、个人调拨进度、座位预约、提交故障报修等面向读者的自助功能。</p>
            <el-button type="primary">去登录</el-button>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-card shadow="hover" class="card admin" @click="router.push({ name: 'loginAdmin' })">
            <h2>馆员 / 运维管理</h2>
            <p>调拨到货确认、全量调拨与调拨建议审批、故障工单处理与健康查询、设备状态、定时任务调试等。</p>
            <el-button type="warning" plain>去登录</el-button>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped>
.portal-page {
  min-height: 100vh;
  background: #eef1f6;
  padding: 24px 20px 40px;
}
.portal {
  max-width: 900px;
  margin: 0 auto;
}
.page-heading {
  margin: 0 0 16px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}
.lead {
  margin: 16px 0 20px;
  font-size: 14px;
  color: #606266;
  line-height: 1.65;
}
.cards {
  margin-top: 8px;
}
.card {
  cursor: pointer;
  min-height: 200px;
  margin-bottom: 16px;
}
.card:hover {
  border-color: #409eff;
}
.card.admin:hover {
  border-color: #e6a23c;
}
.card h2 {
  margin: 0 0 12px;
  font-size: 18px;
}
.card p {
  margin: 0 0 16px;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  min-height: 4.5em;
}
</style>
