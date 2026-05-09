<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { postFaultReport } from '@/api/fault'
import { getLibraries, getSeatAreas, getSeats, getEquipmentPage, type LibraryItem, type SeatAreaItem, type SeatItem } from '@/api/catalog'
import { ElMessage } from 'element-plus'
import DataResultPanel from '@/components/DataResultPanel.vue'
import { faultReportRows, faultTypeZh, severityZh, reportSourceZh } from '@/utils/labels'

const auth = useAuthStore()

const libraries = ref<LibraryItem[]>([])
const areas = ref<SeatAreaItem[]>([])
const seats = ref<SeatItem[]>([])

const equipLibId = ref<number | undefined>(undefined)
const equipRows = ref<Record<string, unknown>[]>([])
const equipPage = reactive({ page: 1, size: 10 })
const equipTotal = ref(0)
const equipLoading = ref(false)

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

async function loadLibraries() {
  try {
    libraries.value = await getLibraries()
  } catch {
    libraries.value = []
  }
}

async function loadAreas() {
  areas.value = []
  seats.value = []
  report.areaId = undefined
  report.seatId = undefined
  if (report.libraryId == null) return
  try {
    areas.value = await getSeatAreas(report.libraryId)
  } catch {
    areas.value = []
  }
}

async function loadSeats() {
  seats.value = []
  report.seatId = undefined
  if (report.areaId == null) return
  try {
    seats.value = await getSeats(report.areaId, 200)
  } catch {
    seats.value = []
  }
}

watch(
  () => report.libraryId,
  () => loadAreas(),
)

watch(
  () => report.areaId,
  () => loadSeats(),
)

async function loadEquipmentTable() {
  equipLoading.value = true
  try {
    const data = (await getEquipmentPage({
      libraryId: equipLibId.value,
      page: equipPage.page,
      size: equipPage.size,
    })) as Record<string, unknown>
    const rec = data.records
    equipRows.value = Array.isArray(rec) ? (rec as Record<string, unknown>[]) : []
    equipTotal.value = Number(data.total ?? 0)
  } catch {
    equipRows.value = []
  } finally {
    equipLoading.value = false
  }
}

function pickEquipment(row: Record<string, unknown>) {
  report.equipmentId = Number(row.id)
  ElMessage.success('已选择设备 ID: ' + report.equipmentId)
}

onMounted(async () => {
  await loadLibraries()
  await loadEquipmentTable()
})

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
    <p class="hint">至少填写一类报修对象（馆 / 区域 / 座位 / 设备）。馆、区域、座位请级联选择；设备可从下方列表点选。</p>
    <el-form label-width="120px" style="max-width: 640px">
      <el-form-item label="图书馆">
        <el-select v-model="report.libraryId" clearable placeholder="可选" style="width: 100%" filterable @change="loadAreas">
          <el-option v-for="lib in libraries" :key="lib.id" :label="lib.name" :value="lib.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="区域">
        <el-select v-model="report.areaId" clearable placeholder="先选馆" style="width: 100%" filterable :disabled="!report.libraryId">
          <el-option v-for="a in areas" :key="a.id" :label="`${a.name}（#${a.id}）`" :value="a.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="座位">
        <el-select v-model="report.seatId" clearable placeholder="先选区域" style="width: 100%" filterable :disabled="!report.areaId">
          <el-option v-for="s in seats" :key="s.id" :label="`${s.seatNo ?? '座'}（#${s.id}）`" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="设备">
        <span v-if="report.equipmentId">已选设备 ID：<strong>{{ report.equipmentId }}</strong></span>
        <span v-else class="muted">未选择（可在下表点「选用」）</span>
        <el-button v-if="report.equipmentId" link type="warning" @click="report.equipmentId = undefined">清除</el-button>
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

    <p class="sec-title">设备列表（点选作为报修对象）</p>
    <el-form inline style="margin-bottom: 8px">
      <el-form-item label="筛选馆">
        <el-select v-model="equipLibId" clearable placeholder="全部" style="width: 200px" @change="() => { equipPage.page = 1; loadEquipmentTable() }">
          <el-option v-for="lib in libraries" :key="lib.id" :label="lib.name" :value="lib.id" />
        </el-select>
      </el-form-item>
      <el-button type="primary" plain :loading="equipLoading" @click="loadEquipmentTable">刷新设备</el-button>
    </el-form>
    <el-table :data="equipRows" border size="small" max-height="280" empty-text="暂无设备">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" min-width="120" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="status" label="状态" width="90" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="pickEquipment(row as Record<string, unknown>)">选用</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="equipTotal > equipPage.size"
      layout="prev, pager, next"
      :total="equipTotal"
      :page-size="equipPage.size"
      :current-page="equipPage.page"
      style="margin-top: 8px"
      @current-change="
        (p: number) => {
          equipPage.page = p
          loadEquipmentTable()
        }
      "
    />

    <DataResultPanel style="margin-top: 16px" :rows="summaryRows" :raw="lastRaw" />
  </el-card>
</template>

<style scoped>
.hint {
  margin: 0 0 12px;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}
.muted {
  color: #909399;
  font-size: 13px;
}
.sec-title {
  margin: 20px 0 8px;
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
</style>
