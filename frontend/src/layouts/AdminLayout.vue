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
    <el-aside width="220px" class="aside admin-aside">
      <div class="brand">馆员 / 运维管理</div>
      <el-menu :default-active="active" router class="menu">
        <el-menu-item index="/a/home">管理概览</el-menu-item>
        <el-menu-item index="/a/transfer">调拨与库存</el-menu-item>
        <el-menu-item index="/a/fault">故障工单运维</el-menu-item>
        <el-menu-item index="/a/equipment">设备状态</el-menu-item>
        <el-menu-item index="/a/dev-tasks">定时任务（调试）</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container direction="vertical" class="main-column">
      <el-header class="header">
        <span class="title">{{ (route.meta.title as string) || '管理后台' }}</span>
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
  background: #1d1e1f;
  color: #fff;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.brand {
  padding: 20px 16px;
  font-weight: 600;
  font-size: 16px;
  border-bottom: 1px solid #333;
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
.admin-aside .el-menu-item {
  color: #cfd3dc;
}

.admin-aside .el-menu-item.is-active {
  background: #409eff33 !important;
  color: #fff;
}
</style>
