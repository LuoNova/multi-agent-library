<script setup lang="ts">
import { reactive, ref } from 'vue'
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
import { ElMessage } from 'element-plus'
import DataResultPanel from '@/components/DataResultPanel.vue'
import { seatAvailabilityRows, pagedDataSummary } from '@/utils/labels'

const auth = useAuthStore()

const avail = reactive({
  libraryId: 1,
  reservationDate: '',
  startTime: '09:00',
  endTime: '11:00',
  areaId: undefined as number | undefined,
  hasPower: undefined as boolean | undefined,
  autoAssign: true,
})

const reserve = reactive({
  userId: 0,
  seatId: 1002,
  libraryId: 1,
  reservationDate: '',
  startTime: '09:00',
  endTime: '11:00',
  borrowId: undefined as number | undefined,
  source: '',
})

const myList = reactive({ status: '', page: 1, size: 10 })
const act = reactive({ reservationId: 1, userId: 0 })

const lastRaw = ref<unknown | null>(null)
const summaryRows = ref<{ label: string; value: string }[]>([])

function todayStr() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function initDates() {
  if (!avail.reservationDate) avail.reservationDate = todayStr()
  if (!reserve.reservationDate) reserve.reservationDate = todayStr()
  reserve.userId = auth.userId
  act.userId = auth.userId
}
initDates()

function setPanel(data: unknown, rows: { label: string; value: string }[]) {
  lastRaw.value = data
  summaryRows.value = rows
}

async function runAvail() {
  lastRaw.value = null
  summaryRows.value = []
  try {
    const data = await getSeatAvailable({ ...avail })
    setPanel(data, seatAvailabilityRows(data))
    ElMessage.success('查询完成')
  } catch {
    /* */
  }
}

async function runReserve() {
  try {
    const data = await postSeatReservation({ ...reserve })
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
    ElMessage.success('已查询我的预约')
  } catch {
    /* */
  }
}

async function runAct(name: string, fn: () => Promise<unknown>) {
  try {
    await fn()
    setPanel({ ok: true, action: name }, [{ label: '操作', value: name }, { label: '结果', value: '成功（无返回体）' }])
    ElMessage.success(name)
  } catch {
    /* */
  }
}
</script>

<template>
  <el-tabs type="border-card" @tab-change="initDates">
    <el-tab-pane label="查询可用座位">
      <el-form label-width="120px" style="max-width: 640px">
        <el-form-item label="图书馆编号" required>
          <el-input-number v-model="avail.libraryId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="预约日期" required>
          <el-input v-model="avail.reservationDate" placeholder="yyyy-MM-dd" />
        </el-form-item>
        <el-form-item label="时段" required>
          <el-input v-model="avail.startTime" style="width: 100px" placeholder="开始" />
          <span class="mx">至</span>
          <el-input v-model="avail.endTime" style="width: 100px" placeholder="结束" />
        </el-form-item>
        <el-form-item label="区域编号">
          <el-input-number v-model="avail.areaId" :min="1" clearable controls-position="right" />
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
      <el-form label-width="120px" style="max-width: 640px">
        <el-form-item label="用户编号" required>
          <el-input-number v-model="reserve.userId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="座位编号" required>
          <el-input-number v-model="reserve.seatId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="图书馆编号" required>
          <el-input-number v-model="reserve.libraryId" :min="1" controls-position="right" />
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
    </el-tab-pane>
    <el-tab-pane label="取消 / 签到 / 暂离 / 结束">
      <el-form label-width="120px" style="max-width: 480px">
        <el-form-item label="预约记录编号" required>
          <el-input-number v-model="act.reservationId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="用户编号" required>
          <el-input-number v-model="act.userId" :min="1" controls-position="right" />
        </el-form-item>
        <el-space wrap>
          <el-button @click="runAct('取消预约', () => postCancelReservation(act.reservationId, act.userId))">取消预约</el-button>
          <el-button @click="runAct('签到', () => postCheckIn(act.reservationId, act.userId))">签到</el-button>
          <el-button @click="runAct('暂离', () => postTempLeave(act.reservationId, act.userId))">暂离</el-button>
          <el-button @click="runAct('结束暂离', () => postEndTempLeave(act.reservationId, act.userId))">结束暂离</el-button>
          <el-button type="danger" plain @click="runAct('结束使用', () => postFinishUse(act.reservationId, act.userId))">
            结束使用
          </el-button>
        </el-space>
      </el-form>
    </el-tab-pane>
  </el-tabs>
  <DataResultPanel :rows="summaryRows" :raw="lastRaw" />
</template>

<style scoped>
.mx {
  margin: 0 8px;
  color: #909399;
}
</style>
