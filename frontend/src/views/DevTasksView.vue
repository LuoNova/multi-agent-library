<script setup lang="ts">
import { ref } from 'vue'
import {
  triggerReservationExpire,
  triggerTransferDelayCheck,
  triggerInventoryBalance,
  triggerReservationWarning,
  triggerSeatTempLeave,
} from '@/api/task'
import { ElMessage } from 'element-plus'
import DataResultPanel from '@/components/DataResultPanel.vue'

const lastMsg = ref('')
const summaryRows = ref<{ label: string; value: string }[]>([])

async function run(label: string, fn: () => Promise<string>) {
  lastMsg.value = ''
  summaryRows.value = []
  try {
    const msg = await fn()
    lastMsg.value = msg
    summaryRows.value = [{ label: '执行结果', value: msg }]
    ElMessage.success(label + ' 已执行')
  } catch {
    /* */
  }
}
</script>

<template>
  <el-alert title="以下操作会真实触发后端定时任务逻辑，请在了解影响后再点击。" type="warning" show-icon :closable="false" style="margin-bottom: 16px" />
  <el-space wrap>
    <el-button @click="run('预约超期释放', triggerReservationExpire)">预约超期释放</el-button>
    <el-button @click="run('调拨延迟检查', triggerTransferDelayCheck)">调拨延迟检查</el-button>
    <el-button @click="run('库存平衡', triggerInventoryBalance)">库存平衡任务</el-button>
    <el-button @click="run('预约提醒', triggerReservationWarning)">预约提醒</el-button>
    <el-button @click="run('座位暂离处理', triggerSeatTempLeave)">座位暂离处理</el-button>
  </el-space>
  <DataResultPanel v-if="lastMsg" :rows="summaryRows" :raw="{ message: lastMsg }" />
</template>
