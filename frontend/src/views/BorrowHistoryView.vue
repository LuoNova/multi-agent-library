<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getBorrowHistory, type BorrowHistoryRow } from '@/api/catalog'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()

const query = reactive({
  status: '' as string,
  page: 1,
  size: 10,
})

const total = ref(0)
const loading = ref(false)
const rows = ref<BorrowHistoryRow[]>([])
const drawerOpen = ref(false)
const current = ref<BorrowHistoryRow | null>(null)

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'BORROWING', label: '借阅中' },
  { value: 'RETURNED', label: '已归还' },
  { value: 'RESERVED', label: '已预留（待取书）' },
  { value: 'TRANSFERRING', label: '调拨在途' },
  { value: 'CANCELLED', label: '已取消' },
]

async function load() {
  loading.value = true
  try {
    const data = (await getBorrowHistory({
      userId: auth.userId,
      status: query.status || undefined,
      page: query.page,
      size: query.size,
    })) as Record<string, unknown>
    total.value = Number(data.total ?? 0)
    const rec = data.records
    rows.value = Array.isArray(rec) ? (rec as BorrowHistoryRow[]) : []
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function openDetail(row: BorrowHistoryRow) {
  current.value = row
  drawerOpen.value = true
}

onMounted(() => {
  load().catch(() => ElMessage.error('加载失败'))
})
</script>

<template>
  <el-card shadow="never">
    <template #header>我的借阅记录</template>
    <p class="hint">
      展示当前登录用户（ID: <strong>{{ auth.userId }}</strong>）在系统中的<strong>借阅流水</strong>：含在借、已还、预留、调拨在途等状态。若您原指「故障工单处理记录」，请使用管理端故障模块或后续扩展读者「我的工单」接口。
    </p>
    <el-form inline>
      <el-form-item label="状态">
        <el-select v-model="query.status" style="width: 200px" @change="() => { query.page = 1; load() }">
          <el-option v-for="o in statusOptions" :key="o.value || 'all'" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="每页">
        <el-input-number v-model="query.size" :min="5" :max="50" @change="() => { query.page = 1; load() }" />
      </el-form-item>
      <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
    </el-form>

    <el-table :data="rows" border size="small" style="margin-top: 8px" empty-text="暂无记录">
      <el-table-column prop="borrowId" label="记录ID" width="90" />
      <el-table-column prop="bookTitle" label="书名" min-width="140" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column prop="borrowTime" label="借阅时间" width="170" />
      <el-table-column prop="dueTime" label="应还时间" width="170" />
      <el-table-column prop="returnTime" label="归还时间" width="170" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row as BorrowHistoryRow)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="query.size"
      :current-page="query.page"
      style="margin-top: 12px"
      @current-change="
        (p: number) => {
          query.page = p
          load()
        }
      "
    />

    <el-drawer v-model="drawerOpen" title="借阅详情" size="420px">
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="借阅记录ID">{{ current.borrowId }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ current.userId }}</el-descriptions-item>
          <el-descriptions-item label="副本ID">{{ current.copyId }}</el-descriptions-item>
          <el-descriptions-item label="书目ID">{{ current.biblioId ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="书名">{{ current.bookTitle }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ current.status }}</el-descriptions-item>
          <el-descriptions-item label="借阅时间">{{ current.borrowTime ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="应还时间">{{ current.dueTime ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="归还时间">{{ current.returnTime ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="实际取书时间">{{ current.actualPickupTime ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="取书馆">
            {{ current.pickupLibraryName ?? '—' }}（ID: {{ current.pickupLibraryId ?? '—' }}）
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </el-card>
</template>

<style scoped>
.hint {
  margin: 0 0 16px;
  font-size: 14px;
  color: #606266;
  line-height: 1.55;
}
</style>
