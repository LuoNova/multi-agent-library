<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { postConfirmPickup, type PickupConfirmResponse } from '@/api/borrow'
import { getPickupPending, type PickupPendingRow } from '@/api/catalog'
import { ElMessage } from 'element-plus'
import DataResultPanel from '@/components/DataResultPanel.vue'
import { pickupResultRows } from '@/utils/labels'

const auth = useAuthStore()

const rows = ref<PickupPendingRow[]>([])
const listLoading = ref(false)

const detailOpen = ref(false)
const detailRow = ref<PickupPendingRow | null>(null)

const loading = ref(false)
const result = ref<PickupConfirmResponse | null>(null)
const summaryRows = ref<{ label: string; value: string }[]>([])

async function loadList() {
  listLoading.value = true
  try {
    rows.value = await getPickupPending(auth.userId)
  } catch {
    rows.value = []
  } finally {
    listLoading.value = false
  }
}

onMounted(() => loadList())

function openDetail(row: PickupPendingRow) {
  detailRow.value = row
  detailOpen.value = true
}

async function confirmRow(row: PickupPendingRow) {
  loading.value = true
  result.value = null
  summaryRows.value = []
  try {
    const data = await postConfirmPickup({
      borrowId: row.borrowId,
      userId: row.userId,
    })
    result.value = data
    summaryRows.value = pickupResultRows(data)
    ElMessage.success('取书已确认')
    await loadList()
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
    <p class="hint">
      当前用户 ID：<strong>{{ auth.userId }}</strong>。下列为<strong>待取书</strong>（预留或调拨在途）记录；取书馆以列表为准。仅 <code>RESERVED</code> 状态可执行取书确认（与后端校验一致）。
    </p>
    <el-button type="primary" plain :loading="listLoading" style="margin-bottom: 12px" @click="loadList">刷新待取书</el-button>
    <el-table :data="rows" border size="small" empty-text="暂无待取书记录">
      <el-table-column prop="bookTitle" label="书名" min-width="160" show-overflow-tooltip />
      <el-table-column prop="borrowId" label="借阅记录ID" width="110" />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column prop="pickupLibraryName" label="取书馆" min-width="120" />
      <el-table-column prop="pickupDeadline" label="取书截止" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row as PickupPendingRow)">详情</el-button>
          <el-button
            link
            type="success"
            :disabled="(row as PickupPendingRow).status !== 'RESERVED'"
            :loading="loading"
            @click="confirmRow(row as PickupPendingRow)"
          >
            确认取书
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="detailOpen" title="待取书详情" size="400px">
      <template v-if="detailRow">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="借阅记录ID">{{ detailRow.borrowId }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ detailRow.userId }}</el-descriptions-item>
          <el-descriptions-item label="副本ID">{{ detailRow.copyId }}</el-descriptions-item>
          <el-descriptions-item label="书名">{{ detailRow.bookTitle }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detailRow.status }}</el-descriptions-item>
          <el-descriptions-item label="取书馆">{{ detailRow.pickupLibraryName }}（ID: {{ detailRow.pickupLibraryId ?? '—' }}）</el-descriptions-item>
          <el-descriptions-item label="截止时间">{{ detailRow.pickupDeadline ?? '—' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>

    <DataResultPanel style="margin-top: 16px" :rows="summaryRows" :raw="result" />
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
