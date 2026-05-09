<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { postReturn } from '@/api/book'
import { getActiveLoans, getLibraries, type ActiveLoanRow, type LibraryItem } from '@/api/catalog'
import { ElMessage } from 'element-plus'
import DataResultPanel from '@/components/DataResultPanel.vue'
import { returnResultRows } from '@/utils/labels'

const auth = useAuthStore()

const libraries = ref<LibraryItem[]>([])
const loans = ref<ActiveLoanRow[]>([])
const loansLoading = ref(false)

const detailOpen = ref(false)
const detailRow = ref<ActiveLoanRow | null>(null)

const returnOpen = ref(false)
const returnForm = reactive({
  copyId: 0,
  userId: 0,
  returnLibraryId: null as number | null,
})

const loading = ref(false)
const result = ref<Record<string, unknown> | null>(null)
const summaryRows = ref<{ label: string; value: string }[]>([])

async function loadLibraries() {
  try {
    libraries.value = await getLibraries()
  } catch {
    /* */
  }
}

async function loadLoans() {
  loansLoading.value = true
  try {
    loans.value = await getActiveLoans(auth.userId)
  } catch {
    loans.value = []
  } finally {
    loansLoading.value = false
  }
}

onMounted(async () => {
  await loadLibraries()
  await loadLoans()
})

function openDetail(row: ActiveLoanRow) {
  detailRow.value = row
  detailOpen.value = true
}

function openReturn(row: ActiveLoanRow) {
  returnForm.copyId = row.copyId
  returnForm.userId = row.userId
  returnForm.returnLibraryId = row.suggestedReturnLibraryId ?? null
  returnOpen.value = true
}

async function doReturn() {
  if (returnForm.returnLibraryId == null) {
    ElMessage.warning('请选择还书馆')
    return
  }
  returnOpen.value = false
  loading.value = true
  result.value = null
  summaryRows.value = []
  try {
    const data = (await postReturn({
      copyId: returnForm.copyId,
      userId: returnForm.userId,
      returnLibraryId: returnForm.returnLibraryId,
    })) as Record<string, unknown>
    result.value = data
    summaryRows.value = returnResultRows(data)
    ElMessage.success('还书请求已处理')
    await loadLoans()
  } catch {
    /* */
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card shadow="never">
    <template #header>还书</template>
    <p class="hint">
      以下为当前登录用户（ID: <strong>{{ auth.userId }}</strong>）的<strong>在借图书</strong>。请选择「详情」查看字段，或「还书」在弹窗中选择还书馆后确认归还。
    </p>
    <el-button type="primary" plain :loading="loansLoading" style="margin-bottom: 12px" @click="loadLoans">刷新在借列表</el-button>
    <el-table :data="loans" border size="small" empty-text="暂无在借记录">
      <el-table-column prop="bookTitle" label="书名" min-width="160" show-overflow-tooltip />
      <el-table-column prop="copyId" label="副本ID" width="90" />
      <el-table-column prop="dueTime" label="应还时间" width="170" />
      <el-table-column prop="suggestedReturnLibraryName" label="默认还书馆" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row as ActiveLoanRow)">详情</el-button>
          <el-button link type="danger" @click="openReturn(row as ActiveLoanRow)">还书</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="detailOpen" title="借阅详情" size="400px">
      <template v-if="detailRow">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="借阅记录ID">{{ detailRow.borrowId }}</el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ detailRow.userId }}</el-descriptions-item>
          <el-descriptions-item label="副本ID">{{ detailRow.copyId }}</el-descriptions-item>
          <el-descriptions-item label="书目ID">{{ detailRow.biblioId ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="书名">{{ detailRow.bookTitle }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detailRow.status }}</el-descriptions-item>
          <el-descriptions-item label="应还时间">{{ detailRow.dueTime ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="建议还书馆">
            {{ detailRow.suggestedReturnLibraryName }}（ID: {{ detailRow.suggestedReturnLibraryId ?? '—' }}）
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>

    <el-dialog v-model="returnOpen" title="确认还书" width="420px" destroy-on-close>
      <p class="dlg-hint">副本 <strong>{{ returnForm.copyId }}</strong>，还书人用户 ID <strong>{{ returnForm.userId }}</strong></p>
      <el-form label-width="100px">
        <el-form-item label="还书馆" required>
          <el-select v-model="returnForm.returnLibraryId" placeholder="请选择还书馆" style="width: 100%" filterable>
            <el-option v-for="lib in libraries" :key="lib.id" :label="lib.name" :value="lib.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnOpen = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="doReturn">确认还书</el-button>
      </template>
    </el-dialog>

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
.dlg-hint {
  margin: 0 0 12px;
  font-size: 14px;
  color: #606266;
}
</style>
