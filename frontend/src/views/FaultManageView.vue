<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getFaultDetail, patchFaultStatus, postFaultHealthQuery } from '@/api/fault'
import { getLibraries, getAdminFaultTicketsPage, type AdminFaultTicketRow, type LibraryItem } from '@/api/catalog'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataResultPanel from '@/components/DataResultPanel.vue'
import {
  faultReportRows,
  faultTypeZh,
  severityZh,
  faultStatusZh,
  resourceTypeZh,
  pagedDataSummary,
  healthResultRows,
} from '@/utils/labels'

const libraries = ref<LibraryItem[]>([])
const listQ = reactive({
  libraryId: undefined as number | undefined,
  status: '' as string,
  faultType: '' as string,
  page: 1,
  size: 10,
})

const detailId = ref(1)
const patch = reactive({
  id: 1,
  status: 'IN_PROGRESS',
  assignee: '',
  adminRemark: '',
})

const healthJson = ref(
  JSON.stringify(
    {
      resources: [
        { resourceType: 'LIBRARY', resourceId: 1 },
        { resourceType: 'SEAT', resourceId: 1001 },
      ],
    },
    null,
    2,
  ),
)

const lastRaw = ref<unknown | null>(null)
const summaryRows = ref<{ label: string; value: string }[]>([])

const listRecords = ref<AdminFaultTicketRow[]>([])
const listTotal = ref(0)
const listLoading = ref(false)

/** 管理端仅展示三种业务状态（已受理/已关闭在前端隐藏，库内仍可能存在） */
const VISIBLE_FAULT_STATUSES = ['REPORTED', 'IN_PROGRESS', 'RESTORED'] as const
const FAULT_TYPES = ['seat_broken', 'power_failure', 'env_issue', 'network_fault', 'other'] as const

/** 列表/确认框展示：ACCEPTED→处理中、CLOSED→已恢复 */
function faultStatusManageDisplay(s?: string): string {
  if (s === 'ACCEPTED') return faultStatusZh('IN_PROGRESS')
  if (s === 'CLOSED') return faultStatusZh('RESTORED')
  return faultStatusZh(s)
}

/** 详情/ PATCH 回显面板：状态列用简化展示 */
function faultReportRowsManage(data: Record<string, unknown>) {
  const rows = faultReportRows(data)
  return rows.map((r) =>
    r.label === '状态' ? { ...r, value: faultStatusManageDisplay(String(data.status ?? '')) } : r,
  )
}

/** 填入「更新状态」表单时，把隐藏态映射为可选项 */
function mapToEditableStatus(s?: string): string {
  if (s === 'ACCEPTED') return 'IN_PROGRESS'
  if (s === 'CLOSED') return 'RESTORED'
  if (s && (VISIBLE_FAULT_STATUSES as readonly string[]).includes(s)) return s
  return 'IN_PROGRESS'
}

const healthTableRows = computed(() => {
  if (!lastRaw.value || typeof lastRaw.value !== 'object') return []
  const r = (lastRaw.value as { results?: unknown[] }).results
  if (!Array.isArray(r)) return []
  return r.map((item: unknown) => {
    const x = item as Record<string, unknown>
    return {
      type: resourceTypeZh(String(x.resourceType)),
      id: x.resourceId,
      available: x.available === false ? '不可用' : '可用',
      summary: x.faultSummary ?? '—',
    }
  })
})

function setPanel(data: unknown, rows: { label: string; value: string }[]) {
  lastRaw.value = data
  summaryRows.value = rows
}

async function loadLibs() {
  try {
    libraries.value = await getLibraries()
  } catch {
    libraries.value = []
  }
}

async function loadList() {
  lastRaw.value = null
  summaryRows.value = []
  listLoading.value = true
  try {
    const params: Record<string, string | number | undefined> = {
      page: listQ.page,
      size: listQ.size,
    }
    if (listQ.libraryId != null) params.libraryId = listQ.libraryId
    if (listQ.status) params.status = listQ.status
    if (listQ.faultType) params.faultType = listQ.faultType
    const data = (await getAdminFaultTicketsPage(params)) as Record<string, unknown>
    setPanel(data, pagedDataSummary(data))
    listTotal.value = Number(data.total ?? 0)
    const rec = data.records
    listRecords.value = Array.isArray(rec) ? (rec as AdminFaultTicketRow[]) : []
  } catch {
    listRecords.value = []
  } finally {
    listLoading.value = false
  }
}

function pickTicket(row: AdminFaultTicketRow) {
  detailId.value = row.id
  patch.id = row.id
  patch.status = mapToEditableStatus(row.status)
  ElMessage.success(`已填入工单编号 ${row.id}（详情 / 更新状态）`)
}

/** 列表行内一键 PATCH，无需切到「更新状态」Tab */
async function quickStatus(row: AdminFaultTicketRow, next: string, confirmTitle: string) {
  try {
    await ElMessageBox.confirm(
      `将工单 <strong>#${row.id}</strong>「${faultStatusManageDisplay(row.status)}」→「<strong>${faultStatusManageDisplay(next)}</strong>」？`,
      confirmTitle,
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        dangerouslyUseHTMLString: true,
      },
    )
  } catch {
    return
  }
  try {
    const data = await patchFaultStatus(row.id, { status: next })
    setPanel(data, faultReportRowsManage(data as unknown as Record<string, unknown>))
    ElMessage.success('状态已更新')
    await loadList()
  } catch {
    /* */
  }
}

function canQuickToInProgress(s?: string) {
  return s === 'REPORTED'
}

function canQuickToRestored(s?: string) {
  return s !== 'RESTORED' && s !== 'CLOSED'
}

async function loadDetail() {
  try {
    const data = await getFaultDetail(detailId.value)
    setPanel(data, faultReportRowsManage(data as unknown as Record<string, unknown>))
  } catch {
    /* */
  }
}

async function doPatch() {
  try {
    const body: { status: string; assignee?: string; adminRemark?: string } = {
      status: patch.status,
    }
    if (patch.assignee) body.assignee = patch.assignee
    if (patch.adminRemark) body.adminRemark = patch.adminRemark
    const data = await patchFaultStatus(patch.id, body)
    setPanel(data, faultReportRowsManage(data as unknown as Record<string, unknown>))
    ElMessage.success('状态已更新')
    await loadList()
  } catch {
    /* */
  }
}

async function doHealth() {
  try {
    const parsed = JSON.parse(healthJson.value) as { resources: { resourceType: string; resourceId: number }[] }
    const data = await postFaultHealthQuery(parsed.resources)
    setPanel(data, healthResultRows(data))
    ElMessage.success('健康查询完成')
  } catch (e) {
    ElMessage.error('请求体 JSON 无效或请求失败: ' + (e as Error).message)
  }
}

onMounted(async () => {
  await loadLibs()
  await loadList()
})
</script>

<template>
  <el-tabs type="border-card">
    <el-tab-pane label="工单列表">
      <p class="hint">
        列表数据来自 <code>GET /api/catalog/fault-tickets</code>（含 <strong>libraryName</strong>）。可在表格右侧<strong>直接变更状态</strong>（确认后调用
        <code>PATCH /api/fault/{id}/status</code>）；需填负责人/备注时仍用下方「更新状态」页签。本页将流程简化为<strong>已报修
        / 处理中 / 已恢复</strong>三种可见状态（历史数据中的已受理、已关闭在界面上分别并入处理中、已恢复展示，且不再提供对应快捷按钮）。
      </p>
      <el-form inline>
        <el-form-item label="图书馆">
          <el-select
            v-model="listQ.libraryId"
            clearable
            placeholder="全部"
            filterable
            style="width: 200px"
            @change="() => { listQ.page = 1; loadList() }"
          >
            <el-option v-for="lib in libraries" :key="lib.id" :label="lib.name" :value="lib.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="listQ.status" clearable placeholder="全部" style="width: 160px" @change="() => { listQ.page = 1; loadList() }">
            <el-option v-for="s in VISIBLE_FAULT_STATUSES" :key="s" :value="s" :label="faultStatusZh(s)" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="listQ.faultType" clearable placeholder="全部" style="width: 140px" @change="() => { listQ.page = 1; loadList() }">
            <el-option v-for="t in FAULT_TYPES" :key="t" :value="t" :label="faultTypeZh(t)" />
          </el-select>
        </el-form-item>
        <el-form-item label="每页">
          <el-input-number v-model="listQ.size" :min="5" :max="100" @change="() => { listQ.page = 1; loadList() }" />
        </el-form-item>
        <el-button type="primary" :loading="listLoading" @click="() => { listQ.page = 1; loadList() }">查询</el-button>
      </el-form>
      <el-table
        :data="listRecords"
        border
        size="small"
        v-loading="listLoading"
        style="width: 100%; margin-top: 12px"
        max-height="360"
        empty-text="暂无工单"
      >
        <el-table-column prop="id" label="工单" width="72" />
        <el-table-column prop="libraryName" label="图书馆" width="120" show-overflow-tooltip />
        <el-table-column label="标题" min-width="140" prop="title" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">{{ faultStatusManageDisplay(String((row as AdminFaultTicketRow).status)) }}</template>
        </el-table-column>
        <el-table-column label="程度" width="80">
          <template #default="{ row }">{{ severityZh(String((row as AdminFaultTicketRow).severity)) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ faultTypeZh(String((row as AdminFaultTicketRow).faultType)) }}</template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="快捷状态" min-width="280" fixed="right">
          <template #default="{ row }">
            <span class="row-actions">
              <el-button
                v-if="canQuickToInProgress((row as AdminFaultTicketRow).status)"
                link
                type="primary"
                @click="quickStatus(row as AdminFaultTicketRow, 'IN_PROGRESS', '开始处理')"
              >
                处理中
              </el-button>
              <el-button
                v-if="canQuickToRestored((row as AdminFaultTicketRow).status)"
                link
                type="success"
                @click="quickStatus(row as AdminFaultTicketRow, 'RESTORED', '标记已恢复')"
              >
                已恢复
              </el-button>
              <el-button link @click="pickTicket(row as AdminFaultTicketRow)">填入编号</el-button>
            </span>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="listTotal > 0"
        layout="total, prev, pager, next"
        :total="listTotal"
        :page-size="listQ.size"
        :current-page="listQ.page"
        style="margin-top: 12px"
        @current-change="
          (p: number) => {
            listQ.page = p
            loadList()
          }
        "
      />
    </el-tab-pane>
    <el-tab-pane label="工单详情">
      <el-input-number v-model="detailId" :min="1" />
      <el-button style="margin-left: 8px" @click="loadDetail">按编号查询</el-button>
    </el-tab-pane>
    <el-tab-pane label="更新状态">
      <el-form label-width="100px" style="max-width: 480px">
        <el-form-item label="工单编号" required>
          <el-input-number v-model="patch.id" :min="1" />
        </el-form-item>
        <el-form-item label="新状态" required>
          <el-select v-model="patch.status" style="width: 100%">
            <el-option v-for="s in VISIBLE_FAULT_STATUSES" :key="s" :value="s" :label="faultStatusZh(s)" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="patch.assignee" placeholder="可选" />
        </el-form-item>
        <el-form-item label="运维备注">
          <el-input v-model="patch.adminRemark" placeholder="可选" />
        </el-form-item>
        <el-button type="primary" @click="doPatch">保存</el-button>
      </el-form>
    </el-tab-pane>
    <el-tab-pane label="资源健康查询">
      <p class="hint">请求体为 JSON，须包含 <code>resources</code> 数组。下方为编辑区。</p>
      <el-input v-model="healthJson" type="textarea" :rows="10" class="mono" />
      <el-button type="primary" style="margin-top: 8px" @click="doHealth">执行查询</el-button>
      <el-table
        v-if="healthTableRows.length"
        :data="healthTableRows"
        border
        size="small"
        style="width: 100%; margin-top: 16px"
      >
        <el-table-column prop="type" label="资源类型" width="120" />
        <el-table-column prop="id" label="资源编号" width="100" />
        <el-table-column prop="available" label="是否可用" width="100" />
        <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
      </el-table>
    </el-tab-pane>
  </el-tabs>
  <DataResultPanel :rows="summaryRows" :raw="lastRaw" />
</template>

<style scoped>
.hint {
  margin: 0 0 12px;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}
.mono :deep(textarea) {
  font-family: ui-monospace, monospace;
  font-size: 13px;
}
.row-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px 6px;
}
</style>
