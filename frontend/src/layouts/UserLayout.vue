<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const active = computed(() => route.path)

function logout() {
  auth.logout()
  router.push({ name: 'portal' })
}
</script>

<template>
  <el-container class="layout-root">
    <el-aside width="220px" class="aside user-aside">
      <div class="brand">读者服务</div>
      <el-menu :default-active="active" router class="menu">
        <el-menu-item index="/u/home">服务首页</el-menu-item>
        <el-menu-item index="/u/borrow">借书申请</el-menu-item>
        <el-menu-item index="/u/return">还书</el-menu-item>
        <el-menu-item index="/u/pickup">取书确认</el-menu-item>
        <el-menu-item index="/u/borrow-history">我的借阅记录</el-menu-item>
        <el-menu-item index="/u/transfer">我的调拨与进度</el-menu-item>
        <el-menu-item index="/u/seat">座位预约</el-menu-item>
        <el-menu-item index="/u/fault">故障报修</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container direction="vertical" class="main-column">
      <el-header class="header">
        <span class="title">{{ (route.meta.title as string) || '读者服务' }}</span>
        <div class="right">
          <span class="user">{{ auth.displayName }}（用户ID: {{ auth.userId }}）</span>
          <el-button type="primary" link @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-root {
  height: 100vh;
  width: 100%;
  max-width: 100%;
}

.main-column {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.aside {
  min-height: 100vh;
  background: #1d3557;
  color: #fff;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.brand {
  padding: 20px 16px;
  font-weight: 600;
  font-size: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.menu {
  border-right: none;
  background: transparent;
  flex: 1;
}

.header {
  flex-shrink: 0;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
}

.title {
  font-size: 16px;
  font-weight: 500;
}

.right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user {
  color: #606266;
  font-size: 14px;
}

.main {
  flex: 1;
  min-height: 0;
  overflow: auto;
  background: #f5f7fa;
  padding: 16px 20px;
}
</style>

<style>
.user-aside .el-menu-item {
  color: #cfd3dc;
}

.user-aside .el-menu-item.is-active {
  background: rgba(168, 218, 220, 0.25) !important;
  color: #fff;
}
</style>
