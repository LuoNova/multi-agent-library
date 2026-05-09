<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { postFaultReport } from '@/api/fault'
import { ElMessage } from 'element-plus'
import DataResultPanel from '@/components/DataResultPanel.vue'
import { faultReportRows, faultTypeZh, severityZh, reportSourceZh } from '@/utils/labels'

const auth = useAuthStore()

const report = reactive({
  libraryId: undefined as number | undefined,
  areaId: undefined as number | undefined,
  seatId: undefined as number | undefined,
  equipmentId: undefined as number | undefined,
  faultType: 'other',
  severity: 'medium',
  title: '设备/座位异常',
  description: '',
  reportSource: 'USER',
  reportUserId: 0 as number | undefined,
})

const lastRaw = ref<unknown | null>(null)
const summaryRows = ref<{ label: string; value: string }[]>([])

function setPanel(data: unknown, rows: { label: string; value: string }[]) {
  lastRaw.value = data
  summaryRows.value = rows
}

async function submitReport() {
  lastRaw.value = null
  summaryRows.value = []
  try {
    report.reportUserId = auth.userId
    const body = {
      ...report,
      libraryId: report.libraryId || undefined,
      areaId: report.areaId || undefined,
      seatId: report.seatId || undefined,
      equipmentId: report.equipmentId || undefined,
    }
    const data = await postFaultReport(body)
    setPanel(data, faultReportRows(data as unknown as Record<string, unknown>))
    ElMessage.success('工单已创建')
  } catch {
    /* */
  }
}
</script>

<template>
  <el-card shadow="never">
    <template #header>提交报修</template>
    <p class="hint">至少填写一类报修对象（馆 / 区域 / 座位 / 设备）；须与数据库中层级一致。</p>
    <el-form label-width="120px" style="max-width: 600px">
      <el-form-item label="图书馆编号">
        <el-input-number v-model="report.libraryId" :min="1" clearable controls-position="right" />
      </el-form-item>
      <el-form-item label="区域编号">
        <el-input-number v-model="report.areaId" :min="1" clearable controls-position="right" />
      </el-form-item>
      <el-form-item label="座位编号">
        <el-input-number v-model="report.seatId" :min="1" clearable controls-position="right" />
      </el-form-item>
      <el-form-item label="设备编号">
        <el-input-number v-model="report.equipmentId" :min="1" clearable controls-position="right" />
      </el-form-item>
      <el-form-item label="故障类型" required>
        <el-select v-model="report.faultType" style="width: 100%">
          <el-option value="seat_broken" :label="faultTypeZh('seat_broken')" />
          <el-option value="power_failure" :label="faultTypeZh('power_failure')" />
          <el-option value="env_issue" :label="faultTypeZh('env_issue')" />
          <el-option value="network_fault" :label="faultTypeZh('network_fault')" />
          <el-option value="other" :label="faultTypeZh('other')" />
        </el-select>
      </el-form-item>
      <el-form-item label="严重程度" required>
        <el-select v-model="report.severity" style="width: 100%">
          <el-option value="low" :label="severityZh('low')" />
          <el-option value="medium" :label="severityZh('medium')" />
          <el-option value="high" :label="severityZh('high')" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题" required>
        <el-input v-model="report.title" />
      </el-form-item>
      <el-form-item label="补充说明">
        <el-input v-model="report.description" type="textarea" />
      </el-form-item>
      <el-form-item label="来源" required>
        <el-select v-model="report.reportSource" style="width: 100%">
          <el-option value="USER" :label="reportSourceZh('USER')" />
          <el-option value="MONITOR" :label="reportSourceZh('MONITOR')" />
          <el-option value="ADMIN" :label="reportSourceZh('ADMIN')" />
          <el-option value="SYSTEM" :label="reportSourceZh('SYSTEM')" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="submitReport">提交工单</el-button>
    </el-form>
    <DataResultPanel :rows="summaryRows" :raw="lastRaw" />
  </el-card>
</template>

<style scoped>
.hint {
  margin: 0 0 12px;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}
</style>
