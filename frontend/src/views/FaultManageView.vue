<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { getFaultList, getFaultDetail, patchFaultStatus, postFaultHealthQuery } from '@/api/fault'
import { ElMessage } from 'element-plus'
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

const listQ = reactive({
  libraryId: undefined as number | undefined,
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

const listRecords = ref<Record<string, unknown>[]>([])

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

async function loadList() {
  lastRaw.value = null
  summaryRows.value = []
  listRecords.value = []
  try {
    const params: Record<string, string | number | undefined> = {
      page: listQ.page,
      size: listQ.size,
    }
    if (listQ.libraryId != null) params.libraryId = listQ.libraryId
    const data = (await getFaultList(params)) as Record<string, unknown>
    setPanel(data, pagedDataSummary(data))
    const rec = data.records
    listRecords.value = Array.isArray(rec) ? (rec as Record<string, unknown>[]) : []
  } catch {
    /* */
  }
}

async function loadDetail() {
  listRecords.value = []
  try {
    const data = await getFaultDetail(detailId.value)
    setPanel(data, faultReportRows(data as unknown as Record<string, unknown>))
  } catch {
    /* */
  }
}

async function doPatch() {
  listRecords.value = []
  try {
    const body: { status: string; assignee?: string; adminRemark?: string } = {
      status: patch.status,
    }
    if (patch.assignee) body.assignee = patch.assignee
    if (patch.adminRemark) body.adminRemark = patch.adminRemark
    const data = await patchFaultStatus(patch.id, body)
    setPanel(data, faultReportRows(data as unknown as Record<string, unknown>))
    ElMessage.success('状态已更新')
  } catch {
    /* */
  }
}

async function doHealth() {
  listRecords.value = []
  try {
    const parsed = JSON.parse(healthJson.value) as { resources: { resourceType: string; resourceId: number }[] }
    const data = await postFaultHealthQuery(parsed.resources)
    setPanel(data, healthResultRows(data))
    ElMessage.success('健康查询完成')
  } catch (e) {
    ElMessage.error('请求体 JSON 无效或请求失败: ' + (e as Error).message)
  }
}
</script>

<template>
  <el-tabs type="border-card">
    <el-tab-pane label="工单列表">
      <el-form inline>
        <el-form-item label="图书馆编号">
          <el-input-number v-model="listQ.libraryId" :min="1" clearable />
        </el-form-item>
        <el-form-item label="页码">
          <el-input-number v-model="listQ.page" :min="1" />
        </el-form-item>
        <el-form-item label="每页条数">
          <el-input-number v-model="listQ.size" :min="1" />
        </el-form-item>
        <el-button type="primary" @click="loadList">查询列表</el-button>
      </el-form>
      <el-table
        v-if="listRecords.length"
        :data="listRecords"
        border
        size="small"
        style="width: 100%; margin-top: 12px"
        max-height="320"
      >
        <el-table-column prop="id" label="工单编号" width="90" />
        <el-table-column label="标题" min-width="140" prop="title" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">{{ faultStatusZh(String((row as Record<string, unknown>).status)) }}</template>
        </el-table-column>
        <el-table-column label="严重程度" width="90">
          <template #default="{ row }">{{ severityZh(String((row as Record<string, unknown>).severity)) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ faultTypeZh(String((row as Record<string, unknown>).faultType)) }}</template>
        </el-table-column>
      </el-table>
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
            <el-option v-for="s in ['REPORTED', 'ACCEPTED', 'IN_PROGRESS', 'RESTORED', 'CLOSED']" :key="s" :value="s" :label="faultStatusZh(s)" />
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
</style>
