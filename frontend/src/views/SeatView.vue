<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  getSeatAvailable,
  postSeatReservation,
  getMyReservations,
  postCancelReservation,
  postCheckIn,
  postTempLeave,
  postEndTempLeave,
  postFinishUse,
} from '@/api/seat'
import { getLibraries, getSeatAreas, getSeats, type LibraryItem, type SeatAreaItem, type SeatItem } from '@/api/catalog'
import { ElMessage } from 'element-plus'
import DataResultPanel from '@/components/DataResultPanel.vue'
import { seatAvailabilityRows, pagedDataSummary } from '@/utils/labels'

const auth = useAuthStore()

const libraries = ref<LibraryItem[]>([])
const areasAvail = ref<SeatAreaItem[]>([])
const areasReserve = ref<SeatAreaItem[]>([])
const seatsReserve = ref<SeatItem[]>([])

const avail = reactive({
  libraryId: null as number | null,
  reservationDate: '',
  startTime: '09:00',
  endTime: '11:00',
  areaId: undefined as number | undefined,
  hasPower: undefined as boolean | undefined,
  autoAssign: true,
})

const reserve = reactive({
  seatId: null as number | null,
  libraryId: null as number | null,
  reserveAreaId: null as number | null,
  reservationDate: '',
  startTime: '09:00',
  endTime: '11:00',
  borrowId: undefined as number | undefined,
  source: '',
})

const myList = reactive({ status: '', page: 1, size: 10 })
const myReservationRows = ref<Record<string, unknown>[]>([])

const lastRaw = ref<unknown | null>(null)
const summaryRows = ref<{ label: string; value: string }[]>([])

function todayStr() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

async function loadLibraries() {
  try {
    libraries.value = await getLibraries()
    const first = libraries.value[0]?.id
    if (first != null) {
      if (avail.libraryId == null) avail.libraryId = first
      if (reserve.libraryId == null) reserve.libraryId = first
    }
  } catch {
    /* */
  }
}

async function loadAreasForAvail() {
  areasAvail.value = []
  avail.areaId = undefined
  if (avail.libraryId == null) return
  try {
    areasAvail.value = await getSeatAreas(avail.libraryId)
  } catch {
    areasAvail.value = []
  }
}

async function loadAreasForReserve() {
  areasReserve.value = []
  seatsReserve.value = []
  reserve.reserveAreaId = null
  reserve.seatId = null
  if (reserve.libraryId == null) return
  try {
    areasReserve.value = await getSeatAreas(reserve.libraryId)
  } catch {
    areasReserve.value = []
  }
}

async function loadSeatsForReserve() {
  seatsReserve.value = []
  reserve.seatId = null
  if (reserve.reserveAreaId == null) return
  try {
    seatsReserve.value = await getSeats(reserve.reserveAreaId, 300)
  } catch {
    seatsReserve.value = []
  }
}

watch(
  () => avail.libraryId,
  () => {
    loadAreasForAvail()
  },
)

watch(
  () => reserve.libraryId,
  () => {
    loadAreasForReserve()
  },
)

watch(
  () => reserve.reserveAreaId,
  () => {
    loadSeatsForReserve()
  },
)

function initDates() {
  if (!avail.reservationDate) avail.reservationDate = todayStr()
  if (!reserve.reservationDate) reserve.reservationDate = todayStr()
}

onMounted(async () => {
  initDates()
  await loadLibraries()
  await loadAreasForAvail()
  await loadAreasForReserve()
})

function setPanel(data: unknown, rows: { label: string; value: string }[]) {
  lastRaw.value = data
  summaryRows.value = rows
}

async function runAvail() {
  if (avail.libraryId == null) {
    ElMessage.warning('请选择图书馆')
    return
  }
  lastRaw.value = null
  summaryRows.value = []
  try {
    const data = await getSeatAvailable({ ...avail, libraryId: avail.libraryId })
    setPanel(data, seatAvailabilityRows(data))
    ElMessage.success('查询完成')
  } catch {
    /* */
  }
}

async function runReserve() {
  if (reserve.libraryId == null || reserve.seatId == null) {
    ElMessage.warning('请选择馆、区域与座位')
    return
  }
  try {
    const data = await postSeatReservation({
      userId: auth.userId,
      seatId: reserve.seatId,
      libraryId: reserve.libraryId,
      reservationDate: reserve.reservationDate,
      startTime: reserve.startTime,
      endTime: reserve.endTime,
      borrowId: reserve.borrowId,
      source: reserve.source || undefined,
    })
    setPanel(data, [{ label: '预约记录编号', value: String((data as { reservationId?: number }).reservationId ?? '—') }])
    ElMessage.success('预约已创建')
  } catch {
    /* */
  }
}

async function runMyList() {
  try {
    const data = await getMyReservations({
      userId: auth.userId,
      status: myList.status || undefined,
      page: myList.page,
      size: myList.size,
    })
    setPanel(data, pagedDataSummary(data))
    const rec = (data as Record<string, unknown>).records
    myReservationRows.value = Array.isArray(rec) ? (rec as Record<string, unknown>[]) : []
    ElMessage.success('已查询我的预约')
  } catch {
    myReservationRows.value = []
  }
}

async function runAct(name: string, fn: () => Promise<unknown>) {
  try {
    await fn()
    setPanel({ ok: true, action: name }, [{ label: '操作', value: name }, { label: '结果', value: '成功（无返回体）' }])
    ElMessage.success(name)
    await runMyList()
  } catch {
    /* */
  }
}

function rid(row: Record<string, unknown>) {
  return Number(row.reservationId ?? row.id ?? 0)
}
</script>

<template>
  <el-tabs type="border-card" @tab-change="initDates">
    <el-tab-pane label="查询可用座位">
      <el-form label-width="120px" style="max-width: 640px">
        <el-form-item label="图书馆" required>
          <el-select v-model="avail.libraryId" placeholder="请选择" style="width: 100%" filterable @change="loadAreasForAvail">
            <el-option v-for="lib in libraries" :key="lib.id" :label="lib.name" :value="lib.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="区域（可选）">
          <el-select v-model="avail.areaId" clearable placeholder="不限区域" style="width: 100%">
            <el-option v-for="a in areasAvail" :key="a.id" :label="`${a.name}（#${a.id}）`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约日期" required>
          <el-input v-model="avail.reservationDate" placeholder="yyyy-MM-dd" />
        </el-form-item>
        <el-form-item label="时段" required>
          <el-input v-model="avail.startTime" style="width: 100px" placeholder="开始" />
          <span class="mx">至</span>
          <el-input v-model="avail.endTime" style="width: 100px" placeholder="结束" />
        </el-form-item>
        <el-form-item label="仅要有电源座位">
          <el-switch v-model="avail.hasPower" />
        </el-form-item>
        <el-form-item label="自动分配座位">
          <el-switch v-model="avail.autoAssign" />
        </el-form-item>
        <el-button type="primary" @click="runAvail">查询</el-button>
      </el-form>
    </el-tab-pane>
    <el-tab-pane label="创建预约">
      <p class="hint">用户固定为当前登录：<strong>{{ auth.userId }}</strong>。请选馆 → 区域 → 座位。</p>
      <el-form label-width="120px" style="max-width: 640px">
        <el-form-item label="图书馆" required>
          <el-select v-model="reserve.libraryId" placeholder="请选择" style="width: 100%" filterable @change="loadAreasForReserve">
            <el-option v-for="lib in libraries" :key="lib.id" :label="lib.name" :value="lib.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="区域" required>
          <el-select v-model="reserve.reserveAreaId" placeholder="请选择区域" style="width: 100%" filterable @change="loadSeatsForReserve">
            <el-option v-for="a in areasReserve" :key="a.id" :label="`${a.name}（#${a.id}）`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="座位" required>
          <el-select v-model="reserve.seatId" placeholder="请选择座位" style="width: 100%" filterable>
            <el-option v-for="s in seatsReserve" :key="s.id" :label="`${s.seatNo ?? '座'}（#${s.id}）`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约日期" required>
          <el-input v-model="reserve.reservationDate" />
        </el-form-item>
        <el-form-item label="时段" required>
          <el-input v-model="reserve.startTime" style="width: 100px" />
          <span class="mx">至</span>
          <el-input v-model="reserve.endTime" style="width: 100px" />
        </el-form-item>
        <el-form-item label="关联借阅编号">
          <el-input-number v-model="reserve.borrowId" :min="1" clearable controls-position="right" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="reserve.source" placeholder="可选" clearable />
        </el-form-item>
        <el-button type="primary" @click="runReserve">提交预约</el-button>
      </el-form>
    </el-tab-pane>
    <el-tab-pane label="我的预约">
      <el-form inline>
        <el-form-item label="状态">
          <el-input v-model="myList.status" placeholder="ACTIVE 等" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="页码">
          <el-input-number v-model="myList.page" :min="1" />
        </el-form-item>
        <el-form-item label="每页条数">
          <el-input-number v-model="myList.size" :min="1" />
        </el-form-item>
        <el-button type="primary" @click="runMyList">查询</el-button>
      </el-form>
      <el-table v-if="myReservationRows.length" :data="myReservationRows" border size="small" style="margin-top: 12px" max-height="360">
        <el-table-column label="预约ID" width="100">
          <template #default="{ row }">{{ rid(row as Record<string, unknown>) }}</template>
        </el-table-column>
        <el-table-column prop="seatId" label="座位ID" width="90" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="reservationDate" label="日期" width="120" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="runAct('取消预约', () => postCancelReservation(rid(row as Record<string, unknown>), auth.userId))"
            >
              取消
            </el-button>
            <el-button link type="success" @click="runAct('签到', () => postCheckIn(rid(row as Record<string, unknown>), auth.userId))">签到</el-button>
            <el-button link @click="runAct('暂离', () => postTempLeave(rid(row as Record<string, unknown>), auth.userId))">暂离</el-button>
            <el-button link @click="runAct('结束暂离', () => postEndTempLeave(rid(row as Record<string, unknown>), auth.userId))">结束暂离</el-button>
            <el-button link type="danger" @click="runAct('结束使用', () => postFinishUse(rid(row as Record<string, unknown>), auth.userId))">结束</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-tab-pane>
  </el-tabs>
  <DataResultPanel :rows="summaryRows" :raw="lastRaw" />
</template>

<style scoped>
.mx {
  margin: 0 8px;
  color: #909399;
}
.hint {
  margin: 0 0 12px;
  font-size: 14px;
  color: #606266;
}
</style>
