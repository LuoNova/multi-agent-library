<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getTransferProgress, getBatchTransferProgress, getMyTransfers } from '@/api/transfer'
import { ElMessage } from 'element-plus'
import DataResultPanel from '@/components/DataResultPanel.vue'
import { transferResponseRows } from '@/utils/labels'

const auth = useAuthStore()

const progressId = ref(1)
const batchOrderId = ref(1)
const myPage = reactive({ status: '', page: 1, size: 10 })
const myRecords = ref<Record<string, unknown>[]>([])

const lastRaw = ref<unknown | null>(null)
const summaryRows = ref<{ label: string; value: string }[]>([])

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
  } catch {
    /* */
  }
}

async function loadMyList() {
  lastRaw.value = null
  summaryRows.value = []
  try {
    const data = (await getMyTransfers({
      userId: auth.userId,
      status: myPage.status || undefined,
      page: myPage.page,
      size: myPage.size,
    })) as Record<string, unknown>
    setResult(data)
    const rec = data.records
    myRecords.value = Array.isArray(rec) ? (rec as Record<string, unknown>[]) : []
    ElMessage.success('已查询我的调拨')
  } catch {
    myRecords.value = []
  }
}

function tid(row: Record<string, unknown>) {
  return Number(row.id ?? row.transferId ?? 0)
}

function oid(row: Record<string, unknown>) {
  return Number(row.orderId ?? row.batchOrderId ?? 0)
}
</script>

<template>
  <el-tabs type="border-card">
    <el-tab-pane label="进度查询">
      <el-space wrap align="center">
        <span>调拨记录</span>
        <el-input-number v-model="progressId" :min="1" />
        <el-button @click="run('已查询单条进度', () => getTransferProgress(progressId))">查询</el-button>
        <el-divider direction="vertical" />
        <span>调拨单</span>
        <el-input-number v-model="batchOrderId" :min="1" />
        <el-button @click="run('已查询批量进度', () => getBatchTransferProgress(batchOrderId))">查询</el-button>
      </el-space>
      <p class="hint">也可在「我的调拨」表格中点「查进度」自动填入调拨记录 ID。</p>
    </el-tab-pane>
    <el-tab-pane label="我的调拨">
      <el-form inline>
        <el-form-item label="当前用户">
          <span>{{ auth.userId }}</span>
        </el-form-item>
        <el-form-item label="状态筛选">
          <el-input v-model="myPage.status" placeholder="可选" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="页码">
          <el-input-number v-model="myPage.page" :min="1" />
        </el-form-item>
        <el-form-item label="每页条数">
          <el-input-number v-model="myPage.size" :min="1" />
        </el-form-item>
        <el-button type="primary" @click="loadMyList">查询</el-button>
      </el-form>
      <el-table v-if="myRecords.length" :data="myRecords" border size="small" style="margin-top: 12px" max-height="360">
        <el-table-column label="调拨ID" width="100">
          <template #default="{ row }">{{ tid(row as Record<string, unknown>) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="orderId" label="调拨单ID" width="100" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="
                () => {
                  progressId = tid(row as Record<string, unknown>)
                  run('已查询单条进度', () => getTransferProgress(progressId))
                }
              "
            >
              查进度
            </el-button>
            <el-button
              v-if="oid(row as Record<string, unknown>)"
              link
              type="success"
              @click="
                () => {
                  batchOrderId = oid(row as Record<string, unknown>)
                  run('已查询批量进度', () => getBatchTransferProgress(batchOrderId))
                }
              "
            >
              查批量进度
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-tab-pane>
  </el-tabs>
  <DataResultPanel :rows="summaryRows" :raw="lastRaw" />
</template>

<style scoped>
.hint {
  margin: 12px 0 0;
  font-size: 13px;
  color: #909399;
}
</style>
