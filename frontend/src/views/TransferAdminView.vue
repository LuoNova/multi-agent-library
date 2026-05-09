<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  postTransferComplete,
  postTransferCompleteBatch,
  getTransferProgress,
  getBatchTransferProgress,
  getSuggestionList,
  getSuggestionDetail,
  postApproveSuggestion,
  postRejectSuggestion,
  postTriggerBalance,
} from '@/api/transfer'
import { getAdminTransfersPage, type AdminTransferRow } from '@/api/catalog'
import { ElMessage } from 'element-plus'
import DataResultPanel from '@/components/DataResultPanel.vue'
import { transferResponseRows } from '@/utils/labels'

const completeForm = reactive({ transferId: 1 })
const batchForm = reactive({ orderId: 1 })
const progressId = ref(1)
const batchOrderId = ref(1)
const suggestionAudit = reactive({ id: 1, approverId: 1, reason: '' })
const suggestionRows = ref<Record<string, unknown>[]>([])

const lastRaw = ref<unknown | null>(null)
const summaryRows = ref<{ label: string; value: string }[]>([])

const transferLoading = ref(false)
const transferRows = ref<AdminTransferRow[]>([])
const transferTotal = ref(0)
const transferQ = reactive({ status: '', page: 1, size: 10 })

function setResult(data: unknown) {
  lastRaw.value = data
  summaryRows.value = transferResponseRows(data)
}

async function run(title: string, fn: () => Promise<unknown>) {
  lastRaw.value = null
  summaryRows.value = []
  try {
    const data = await fn()
    setResult(data)
    ElMessage.success(title)
    await loadTransferTable()
  } catch {
    /* */
  }
}

async function loadTransferTable() {
  transferLoading.value = true
  try {
    const data = (await getAdminTransfersPage({
      status: transferQ.status || undefined,
      page: transferQ.page,
      size: transferQ.size,
    })) as Record<string, unknown>
    transferTotal.value = Number(data.total ?? 0)
    const rec = data.records
    transferRows.value = Array.isArray(rec) ? (rec as AdminTransferRow[]) : []
  } catch {
    transferRows.value = []
  } finally {
    transferLoading.value = false
  }
}

function pickForComplete(row: AdminTransferRow) {
  completeForm.transferId = row.transferId
  ElMessage.success('已填入「确认到货」调拨记录编号')
}

function pickForProgress(row: AdminTransferRow) {
  progressId.value = row.transferId
  ElMessage.success('已填入「单条进度」调拨记录编号')
}

function pickForBatchProgress(row: AdminTransferRow) {
  const oid = row.orderId
  if (oid == null || Number(oid) <= 0) {
    ElMessage.warning('该调拨行无调拨单编号，无法填批量进度')
    return
  }
  batchOrderId.value = Number(oid)
  ElMessage.success('已填入「批量进度」调拨单编号')
}

function pickForBatchComplete(row: AdminTransferRow) {
  const oid = row.orderId
  if (oid == null || Number(oid) <= 0) {
    ElMessage.warning('该调拨行无调拨单编号，无法填批量到货')
    return
  }
  batchForm.orderId = Number(oid)
  ElMessage.success('已填入「批量确认到货」调拨单编号')
}

async function doApproveSuggestion() {
  await run('审批已通过', () =>
    postApproveSuggestion(suggestionAudit.id, suggestionAudit.approverId),
  )
}

async function doRejectSuggestion() {
  await run('已拒绝', () =>
    postRejectSuggestion(suggestionAudit.id, suggestionAudit.approverId, suggestionAudit.reason || undefined),
  )
}

async function refreshSuggestions() {
  try {
    const list = (await getSuggestionList()) as unknown[]
    suggestionRows.value = Array.isArray(list) ? (list as Record<string, unknown>[]) : []
    lastRaw.value = list
    summaryRows.value = [{ label: '待处理建议条数', value: String(suggestionRows.value.length) }]
    ElMessage.success('已刷新建议列表')
  } catch {
    suggestionRows.value = []
  }
}

function pickSuggestion(row: Record<string, unknown>) {
  suggestionAudit.id = Number(row.id)
}

onMounted(() => {
  loadTransferTable()
})
</script>

<template>
  <div class="admin-transfer">
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head">
          <span>调拨记录（只读）</span>
          <el-button type="primary" link @click="loadTransferTable">刷新列表</el-button>
        </div>
      </template>
      <p class="hint">
        下方表格来自 <code>GET /api/catalog/transfers</code>，含馆名与书名。点行内按钮可把
        <strong>调拨记录编号</strong>、<strong>调拨单编号</strong>填入本页各操作区，减少手输。
      </p>
      <el-form inline>
        <el-form-item label="状态">
          <el-input
            v-model="transferQ.status"
            clearable
            placeholder="如 PENDING、IN_TRANSIT"
            style="width: 160px"
            @keyup.enter="
              () => {
                transferQ.page = 1
                loadTransferTable()
              }
            "
          />
        </el-form-item>
        <el-form-item label="每页">
          <el-input-number v-model="transferQ.size" :min="5" :max="100" @change="() => { transferQ.page = 1; loadTransferTable() }" />
        </el-form-item>
        <el-button type="primary" :loading="transferLoading" @click="() => { transferQ.page = 1; loadTransferTable() }">查询</el-button>
      </el-form>

      <el-table :data="transferRows" border size="small" v-loading="transferLoading" empty-text="暂无调拨记录" style="margin-top: 8px">
        <el-table-column prop="transferId" label="调拨记录" width="96" />
        <el-table-column prop="bookTitle" label="书名" min-width="120" show-overflow-tooltip />
        <el-table-column label="源馆 → 目标馆" min-width="200">
          <template #default="{ row }">
            {{ (row as AdminTransferRow).fromLibraryName }} → {{ (row as AdminTransferRow).toLibraryName }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="orderId" label="调拨单" width="88" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="pickForComplete(row as AdminTransferRow)">填到货</el-button>
            <el-button link type="primary" @click="pickForProgress(row as AdminTransferRow)">填进度</el-button>
            <el-button link type="warning" @click="pickForBatchComplete(row as AdminTransferRow)">填批量到货</el-button>
            <el-button link @click="pickForBatchProgress(row as AdminTransferRow)">填批量进度</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="transferTotal > 0"
        layout="total, prev, pager, next"
        :total="transferTotal"
        :page-size="transferQ.size"
        :current-page="transferQ.page"
        style="margin-top: 12px"
        @current-change="
          (p: number) => {
            transferQ.page = p
            loadTransferTable()
          }
        "
      />
    </el-card>

    <el-tabs type="border-card" class="tabs-below">
      <el-tab-pane label="确认到货（单条）">
        <el-form inline>
          <el-form-item label="调拨记录编号" required>
            <el-input-number v-model="completeForm.transferId" :min="1" />
          </el-form-item>
          <el-button type="primary" @click="run('已提交单条到货', () => postTransferComplete(completeForm.transferId))">
            确认图书已到馆
          </el-button>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="批量确认到货">
        <el-form inline>
          <el-form-item label="调拨单编号" required>
            <el-input-number v-model="batchForm.orderId" :min="1" />
          </el-form-item>
          <el-button type="primary" @click="run('已提交批量到货', () => postTransferCompleteBatch(batchForm.orderId))">
            批量确认到货
          </el-button>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="进度查询（运维）">
        <el-space wrap align="center">
          <span>调拨记录</span>
          <el-input-number v-model="progressId" :min="1" />
          <el-button @click="run('已查询单条进度', () => getTransferProgress(progressId))">查询</el-button>
          <el-divider direction="vertical" />
          <span>调拨单</span>
          <el-input-number v-model="batchOrderId" :min="1" />
          <el-button @click="run('已查询批量进度', () => getBatchTransferProgress(batchOrderId))">查询</el-button>
        </el-space>
      </el-tab-pane>
      <el-tab-pane label="调拨建议">
        <el-space direction="vertical" alignment="start" style="width: 100%">
          <el-button type="primary" plain @click="refreshSuggestions">刷新待处理建议</el-button>
          <el-table v-if="suggestionRows.length" :data="suggestionRows" border size="small" max-height="280" style="width: 100%">
            <el-table-column prop="id" label="建议ID" width="90" />
            <el-table-column prop="fromLibraryId" label="源馆" width="80" />
            <el-table-column prop="toLibraryId" label="目标馆" width="80" />
            <el-table-column prop="biblioId" label="书目ID" width="90" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="pickSuggestion(row as Record<string, unknown>)">填入编号</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-form label-width="100px" class="suggestion-audit-form">
            <el-form-item label="建议编号" required>
              <div class="suggestion-id-row">
                <el-input-number v-model="suggestionAudit.id" :min="1" controls-position="right" />
                <el-button @click="run('已查询建议详情', () => getSuggestionDetail(suggestionAudit.id))">查看详情</el-button>
              </div>
            </el-form-item>
            <el-form-item label="审批人编号" required>
              <el-input-number v-model="suggestionAudit.approverId" :min="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="拒绝原因">
              <el-input v-model="suggestionAudit.reason" placeholder="拒绝时选填" clearable style="max-width: 360px" />
            </el-form-item>
          </el-form>
          <el-space class="suggestion-actions">
            <el-button type="success" @click="doApproveSuggestion">审批通过</el-button>
            <el-button type="warning" @click="doRejectSuggestion">拒绝建议</el-button>
          </el-space>
          <el-button type="primary" plain @click="run('已触发库存分析', () => postTriggerBalance())">手动触发库存平衡分析</el-button>
        </el-space>
      </el-tab-pane>
    </el-tabs>
    <DataResultPanel :rows="summaryRows" :raw="lastRaw" />
  </div>
</template>

<style scoped>
.admin-transfer {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.hint {
  margin: 0 0 12px;
  font-size: 14px;
  color: #606266;
  line-height: 1.55;
}
.tabs-below {
  margin-top: 0;
}
.suggestion-audit-form {
  max-width: 520px;
}
.suggestion-actions {
  margin-top: 8px;
  margin-left: 100px;
}
.suggestion-id-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
