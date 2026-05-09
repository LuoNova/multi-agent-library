<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { postBorrowRequest, type BorrowResult } from '@/api/borrow'
import { ElMessage } from 'element-plus'
import DataResultPanel from '@/components/DataResultPanel.vue'
import { borrowResultRows } from '@/utils/labels'

const auth = useAuthStore()
const form = reactive({
  userId: auth.userId,
  biblioId: 1,
  preferredLibraryId: 1,
})
const loading = ref(false)
const result = ref<BorrowResult | null>(null)

const summaryRows = ref<{ label: string; value: string }[]>([])

async function submit() {
  loading.value = true
  result.value = null
  summaryRows.value = []
  try {
    const data = await postBorrowRequest({
      userId: Number(form.userId),
      biblioId: Number(form.biblioId),
      preferredLibraryId: Number(form.preferredLibraryId),
    })
    result.value = data
    summaryRows.value = borrowResultRows(data)
    if (data.status === 'SUCCESS') ElMessage.success(data.message || '借书成功')
    else ElMessage.warning(data.message || '未完成借书')
  } catch {
    /* */
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card shadow="never">
    <template #header>借书申请</template>
    <p class="hint">向目标馆发起借书，系统将自动协商本馆借阅、跨馆调拨或预约排队（约数秒内完成）。</p>
    <el-form :model="form" label-width="140px" style="max-width: 560px">
      <el-form-item label="用户编号" required>
        <el-input-number v-model="form.userId" :min="1" style="width: 100%" controls-position="right" />
      </el-form-item>
      <el-form-item label="书目编号" required>
        <el-input-number v-model="form.biblioId" :min="1" style="width: 100%" controls-position="right" />
      </el-form-item>
      <el-form-item label="取书图书馆编号" required>
        <el-input-number v-model="form.preferredLibraryId" :min="1" style="width: 100%" controls-position="right" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">提交申请</el-button>
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
  line-height: 1.5;
}
</style>
