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
    </el-tab-pane>
    <el-tab-pane label="我的调拨">
      <el-form inline>
        <el-form-item label="当前用户编号">
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
        <el-button
          type="primary"
          @click="
            run('已查询我的调拨', () =>
              getMyTransfers({
                userId: auth.userId,
                status: myPage.status || undefined,
                page: myPage.page,
                size: myPage.size,
              }),
            )
          "
        >
          查询
        </el-button>
      </el-form>
    </el-tab-pane>
  </el-tabs>
  <DataResultPanel :rows="summaryRows" :raw="lastRaw" />
</template>
