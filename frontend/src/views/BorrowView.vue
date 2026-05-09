<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { postBorrowRequest, type BorrowResult } from '@/api/borrow'
import { getBiblios, getLibraries, type BiblioItem, type LibraryItem } from '@/api/catalog'
import { ElMessage } from 'element-plus'
import DataResultPanel from '@/components/DataResultPanel.vue'
import { borrowResultRows } from '@/utils/labels'

const auth = useAuthStore()

const libraries = ref<LibraryItem[]>([])
const biblioKeyword = ref('')
const biblioRows = ref<BiblioItem[]>([])
const biblioLoading = ref(false)

const form = reactive({
  biblioId: null as number | null,
  preferredLibraryId: null as number | null,
})

const selectedBiblioTitle = ref('')

const loading = ref(false)
const result = ref<BorrowResult | null>(null)
const summaryRows = ref<{ label: string; value: string }[]>([])

async function loadLibraries() {
  try {
    libraries.value = await getLibraries()
    if (libraries.value.length && form.preferredLibraryId == null) {
      form.preferredLibraryId = libraries.value[0].id
    }
  } catch {
    /* */
  }
}

async function searchBiblios() {
  biblioLoading.value = true
  try {
    biblioRows.value = await getBiblios(biblioKeyword.value || undefined, 30)
  } catch {
    biblioRows.value = []
  } finally {
    biblioLoading.value = false
  }
}

function pickBiblio(row: BiblioItem) {
  form.biblioId = row.id
  selectedBiblioTitle.value = row.title || `书目#${row.id}`
}

onMounted(async () => {
  await loadLibraries()
  await searchBiblios()
})

async function submit() {
  if (form.biblioId == null || form.preferredLibraryId == null) {
    ElMessage.warning('请选择书目与取书馆')
    return
  }
  loading.value = true
  result.value = null
  summaryRows.value = []
  try {
    const data = await postBorrowRequest({
      userId: Number(auth.userId),
      biblioId: Number(form.biblioId),
      preferredLibraryId: Number(form.preferredLibraryId),
    })
    result.value = data
    summaryRows.value = borrowResultRows(data)
    if (data.status === 'SUCCESS') ElMessage.success(data.message || '借书成功')
    else ElMessage.warning(data.message || '未完成借书')
  } catch {
    /* */
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card shadow="never">
    <template #header>借书申请</template>
    <p class="hint">
      当前登录用户 ID：<strong>{{ auth.userId }}</strong>（与借书请求中的用户一致）。请先检索并<strong>选择书目</strong>，再选择<strong>取书馆</strong>后提交。
    </p>

    <p class="sec-title">选择书目</p>
    <el-space wrap style="margin-bottom: 12px">
      <el-input v-model="biblioKeyword" placeholder="书名 / ISBN / 作者关键词" clearable style="width: 260px" @keyup.enter="searchBiblios" />
      <el-button type="primary" :loading="biblioLoading" @click="searchBiblios">检索</el-button>
    </el-space>
    <el-table :data="biblioRows" border size="small" max-height="280" empty-text="无结果，请调整关键词后检索">
      <el-table-column prop="id" label="书目ID" width="90" />
      <el-table-column prop="title" label="书名" min-width="140" show-overflow-tooltip />
      <el-table-column prop="author" label="作者" width="100" show-overflow-tooltip />
      <el-table-column prop="isbn" label="ISBN" width="130" show-overflow-tooltip />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="pickBiblio(row as BiblioItem)">选为借书</el-button>
        </template>
      </el-table-column>
    </el-table>
    <p v-if="form.biblioId != null" class="picked">已选书目：<strong>{{ selectedBiblioTitle }}</strong>（ID: {{ form.biblioId }}）</p>

    <p class="sec-title">取书馆与提交</p>
    <el-form label-width="120px" style="max-width: 560px">
      <el-form-item label="取书馆" required>
        <el-select v-model="form.preferredLibraryId" placeholder="请选择" style="width: 100%" filterable>
          <el-option v-for="lib in libraries" :key="lib.id" :label="lib.name" :value="lib.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" :disabled="form.biblioId == null" @click="submit">提交申请</el-button>
      </el-form-item>
    </el-form>
    <DataResultPanel :rows="summaryRows" :raw="result" />
  </el-card>
</template>

<style scoped>
.hint {
  margin: 0 0 16px;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}
.picked {
  margin: 12px 0 0;
  font-size: 14px;
  color: #409eff;
}
.sec-title {
  margin: 16px 0 8px;
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
</style>
