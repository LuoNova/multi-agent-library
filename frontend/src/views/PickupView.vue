<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { postConfirmPickup, type PickupConfirmResponse } from '@/api/borrow'
import DataResultPanel from '@/components/DataResultPanel.vue'
import { pickupResultRows } from '@/utils/labels'

const auth = useAuthStore()
const form = reactive({
  borrowId: 1,
  userId: auth.userId,
})
const loading = ref(false)
const result = ref<PickupConfirmResponse | null>(null)
const summaryRows = ref<{ label: string; value: string }[]>([])

async function submit() {
  loading.value = true
  result.value = null
  summaryRows.value = []
  try {
    const data = await postConfirmPickup({
      borrowId: Number(form.borrowId),
      userId: Number(form.userId),
    })
    result.value = data
    summaryRows.value = pickupResultRows(data)
  } catch {
    /* */
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card shadow="never">
    <template #header>到馆取书确认</template>
    <p class="hint">图书到馆并完成预留/调拨后，读者到馆确认取书。</p>
    <el-form :model="form" label-width="120px" style="max-width: 560px">
      <el-form-item label="借阅记录编号" required>
        <el-input-number v-model="form.borrowId" :min="1" style="width: 100%" controls-position="right" />
      </el-form-item>
      <el-form-item label="用户编号" required>
        <el-input-number v-model="form.userId" :min="1" style="width: 100%" controls-position="right" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">确认取书</el-button>
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
