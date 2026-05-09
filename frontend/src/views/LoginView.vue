<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore, type PortalMode } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)

const portal = computed((): PortalMode => (route.name === 'loginAdmin' ? 'admin' : 'user'))

const subtitle = computed(() =>
  portal.value === 'admin' ? '管理后台 · 请登录后继续操作' : '读者服务 · 请登录后继续操作',
)

async function onSubmit() {
  loading.value = true
  try {
    auth.login(username.value, password.value, portal.value)
    ElMessage.success('登录成功')
    const raw = route.query.redirect as string | undefined
    const home = portal.value === 'admin' ? '/a/home' : '/u/home'
    const target = safeRedirect(raw, portal.value, home)
    await router.replace(target)
  } finally {
    loading.value = false
  }
}

function safeRedirect(redirect: string | undefined, mode: PortalMode, fallback: string): string {
  if (!redirect || !redirect.startsWith('/')) return fallback
  if (mode === 'user' && redirect.startsWith('/u')) return redirect
  if (mode === 'admin' && redirect.startsWith('/a')) return redirect
  return fallback
}
</script>

<template>
  <div class="login-page">
    <div class="panel">
      <h1 class="title">图书馆管理系统</h1>
      <p class="subtitle">{{ subtitle }}</p>
      <el-form @submit.prevent="onSubmit" class="form" label-position="top">
        <el-form-item label="账号" required>
          <el-input v-model="username" placeholder="请输入账号" size="large" clearable />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-button type="primary" :loading="loading" native-type="submit" size="large" class="submit">
          登录
        </el-button>
        <el-button text class="back" @click="router.push({ name: 'portal' })">← 返回选择端别</el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100%;
  height: 100%;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #eef1f6;
}

.panel {
  width: 100%;
  max-width: 400px;
  padding: 40px 36px 36px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  border: 1px solid #e4e7ed;
}

.title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  text-align: center;
}

.subtitle {
  margin: 0 0 28px;
  font-size: 14px;
  color: #909399;
  text-align: center;
}

.form {
  margin-top: 8px;
}

.submit {
  width: 100%;
  margin-top: 8px;
}

.back {
  width: 100%;
  margin-top: 12px;
  color: #909399;
}
</style>
