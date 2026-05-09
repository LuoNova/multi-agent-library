<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { postReturn } from '@/api/book'
import DataResultPanel from '@/components/DataResultPanel.vue'
import { returnResultRows } from '@/utils/labels'

const auth = useAuthStore()
const form = reactive({
  copyId: 1,
  userId: auth.userId,
  returnLibraryId: 1,
})
const loading = ref(false)
const result = ref<Record<string, unknown> | null>(null)
const summaryRows = ref<{ label: string; value: string }[]>([])

async function submit() {
  loading.value = true
  result.value = null
  summaryRows.value = []
  try {
    const data = (await postReturn({
      copyId: Number(form.copyId),
      userId: Number(form.userId),
      returnLibraryId: Number(form.returnLibraryId),
    })) as Record<string, unknown>
    result.value = data
    summaryRows.value = returnResultRows(data)
  } catch {
    /* */
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card shadow="never">
    <template #header>还书</template>
    <p class="hint">归还指定副本，系统可触发预约队列、就地预留或跨馆调拨。</p>
    <el-form :model="form" label-width="120px" style="max-width: 560px">
      <el-form-item label="图书副本编号" required>
        <el-input-number v-model="form.copyId" :min="1" style="width: 100%" controls-position="right" />
      </el-form-item>
      <el-form-item label="用户编号" required>
        <el-input-number v-model="form.userId" :min="1" style="width: 100%" controls-position="right" />
      </el-form-item>
      <el-form-item label="还书馆编号" required>
        <el-input-number v-model="form.returnLibraryId" :min="1" style="width: 100%" controls-position="right" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">确认还书</el-button>
      </el-form-item>
    </el-form>
    <DataResultPanel :rows="summaryRows" :raw="result" />
  </el-card>
</template>

<style scoped>
.hint {
  margin: 0 0 16px;
  font-size: 14px;
  color: #606266;
}
</style>
