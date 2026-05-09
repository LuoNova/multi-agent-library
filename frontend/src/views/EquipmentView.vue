<script setup lang="ts">
import { reactive, ref } from 'vue'
import { patchEquipmentStatus } from '@/api/equipment'
import { ElMessage } from 'element-plus'
import DataResultPanel from '@/components/DataResultPanel.vue'
import { equipmentEntityRows } from '@/utils/labels'

const form = reactive({
  id: 1,
  status: 'NORMAL',
})
const lastRaw = ref<unknown | null>(null)
const summaryRows = ref<{ label: string; value: string }[]>([])

async function submit() {
  lastRaw.value = null
  summaryRows.value = []
  try {
    const data = await patchEquipmentStatus(form.id, form.status)
    lastRaw.value = data
    summaryRows.value = equipmentEntityRows(data)
    ElMessage.success('设备状态已更新')
  } catch {
    /* */
  }
}
</script>

<template>
  <el-card shadow="never">
    <template #header>设备状态维护</template>
    <p class="hint">用于运维修改设备状态；与故障工单联动规则由后端执行。</p>
    <el-form inline>
      <el-form-item label="设备编号" required>
        <el-input-number v-model="form.id" :min="1" />
      </el-form-item>
      <el-form-item label="目标状态" required>
        <el-select v-model="form.status" style="width: 140px">
          <el-option value="NORMAL" label="正常" />
          <el-option value="FAULT" label="故障" />
          <el-option value="MAINTAIN" label="维护" />
          <el-option value="DISABLED" label="停用" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="submit">保存</el-button>
    </el-form>
    <DataResultPanel :rows="summaryRows" :raw="lastRaw" />
  </el-card>
</template>

<style scoped>
.hint {
  margin: 0 0 16px;
  font-size: 14px;
  color: #606266;
}
</style>
