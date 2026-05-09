<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { patchEquipmentStatus } from '@/api/equipment'
import { getEquipmentPage, getLibraries, type LibraryItem } from '@/api/catalog'
import { ElMessage } from 'element-plus'
import DataResultPanel from '@/components/DataResultPanel.vue'
import { equipmentEntityRows } from '@/utils/labels'

const libraries = ref<LibraryItem[]>([])
const filterLib = ref<number | undefined>(undefined)
const tableRows = ref<Record<string, unknown>[]>([])
const tableLoading = ref(false)
const page = reactive({ page: 1, size: 15 })
const total = ref(0)

const form = reactive({
  id: 1,
  status: 'NORMAL',
})
const lastRaw = ref<unknown | null>(null)
const summaryRows = ref<{ label: string; value: string }[]>([])

async function loadLibs() {
  try {
    libraries.value = await getLibraries()
  } catch {
    libraries.value = []
  }
}

async function loadTable() {
  tableLoading.value = true
  try {
    const data = (await getEquipmentPage({
      libraryId: filterLib.value,
      page: page.page,
      size: page.size,
    })) as Record<string, unknown>
    const rec = data.records
    tableRows.value = Array.isArray(rec) ? (rec as Record<string, unknown>[]) : []
    total.value = Number(data.total ?? 0)
  } catch {
    tableRows.value = []
  } finally {
    tableLoading.value = false
  }
}

function pickRow(row: Record<string, unknown>) {
  form.id = Number(row.id)
  ElMessage.info('已填入设备 ID: ' + form.id)
}

onMounted(async () => {
  await loadLibs()
  await loadTable()
})

async function submit() {
  lastRaw.value = null
  summaryRows.value = []
  try {
    const data = await patchEquipmentStatus(form.id, form.status)
    lastRaw.value = data
    summaryRows.value = equipmentEntityRows(data)
    ElMessage.success('设备状态已更新')
    await loadTable()
  } catch {
    /* */
  }
}
</script>

<template>
  <el-card shadow="never">
    <template #header>设备状态维护</template>
    <p class="hint">在列表中点选一行可填入下方「设备编号」；修改目标状态后保存。</p>
    <el-form inline style="margin-bottom: 12px">
      <el-form-item label="筛选馆">
        <el-select v-model="filterLib" clearable placeholder="全部" style="width: 200px" @change="() => { page.page = 1; loadTable() }">
          <el-option v-for="lib in libraries" :key="lib.id" :label="lib.name" :value="lib.id" />
        </el-select>
      </el-form-item>
      <el-button type="primary" plain :loading="tableLoading" @click="loadTable">刷新列表</el-button>
    </el-form>
    <el-table :data="tableRows" border size="small" max-height="360" empty-text="暂无设备">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="名称" min-width="120" show-overflow-tooltip />
      <el-table-column prop="libraryId" label="馆ID" width="80" />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="status" label="状态" width="90" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="pickRow(row as Record<string, unknown>)">选用</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="total > page.size"
      layout="prev, pager, next"
      :total="total"
      :page-size="page.size"
      :current-page="page.page"
      style="margin-top: 8px"
      @current-change="
        (p: number) => {
          page.page = p
          loadTable()
        }
      "
    />

    <p class="sec-title">更新状态</p>
    <el-form inline>
      <el-form-item label="设备编号" required>
        <el-input-number v-model="form.id" :min="1" />
      </el-form-item>
      <el-form-item label="目标状态" required>
        <el-select v-model="form.status" style="width: 140px">
          <el-option value="NORMAL" label="正常" />
          <el-option value="FAULT" label="故障" />
          <el-option value="MAINTAIN" label="维护" />
          <el-option value="DISABLED" label="停用" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="submit">保存</el-button>
    </el-form>
    <DataResultPanel :rows="summaryRows" :raw="lastRaw" />
  </el-card>
</template>

<style scoped>
.hint {
  margin: 0 0 16px;
  font-size: 14px;
  color: #606266;
}
.sec-title {
  margin: 16px 0 8px;
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
</style>
