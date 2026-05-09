<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  postTransferComplete,
  postTransferCompleteBatch,
  getTransferProgress,
  getBatchTransferProgress,
  getAllTransfers,
  getSuggestionList,
  getSuggestionDetail,
  postApproveSuggestion,
  postRejectSuggestion,
  postTriggerBalance,
} from '@/api/transfer'
import { ElMessage } from 'element-plus'
import DataResultPanel from '@/components/DataResultPanel.vue'
import { transferResponseRows } from '@/utils/labels'

const completeForm = reactive({ transferId: 1 })
const batchForm = reactive({ orderId: 1 })
const progressId = ref(1)
const batchOrderId = ref(1)
const allPage = reactive({ status: '', page: 1, size: 10 })
const suggestionAudit = reactive({ id: 1, approverId: 1, reason: '' })

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
</script>

<template>
  <el-tabs type="border-card">
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
    <el-tab-pane label="全部调拨">
      <el-form inline>
        <el-form-item label="状态">
          <el-input v-model="allPage.status" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="页码">
          <el-input-number v-model="allPage.page" :min="1" />
        </el-form-item>
        <el-form-item label="每页条数">
          <el-input-number v-model="allPage.size" :min="1" />
        </el-form-item>
        <el-button type="primary" @click="run('已查询全部调拨', () => getAllTransfers(allPage))">查询</el-button>
      </el-form>
    </el-tab-pane>
    <el-tab-pane label="调拨建议">
      <el-space direction="vertical" alignment="start" style="width: 100%">
        <el-button @click="run('已刷新建议列表', () => getSuggestionList())">刷新待处理建议</el-button>
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
        <el-button type="primary" plain @click="run('已触发库存分析', () => postTriggerBalance())">
          手动触发库存平衡分析
        </el-button>
      </el-space>
    </el-tab-pane>
  </el-tabs>
  <DataResultPanel :rows="summaryRows" :raw="lastRaw" />
</template>

<style scoped>
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
