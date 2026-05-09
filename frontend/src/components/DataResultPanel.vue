<script setup lang="ts">
defineProps<{
  rows: { label: string; value: string }[]
  raw: unknown | null
}>()
</script>

<template>
  <div v-if="raw !== null" class="panel">
    <div v-if="rows.length" class="summary">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item v-for="r in rows" :key="r.label" :label="r.label">
          {{ r.value }}
        </el-descriptions-item>
      </el-descriptions>
    </div>
    <el-collapse class="collapse">
      <el-collapse-item title="原始数据（调试用）" name="raw">
        <pre class="json">{{ JSON.stringify(raw, null, 2) }}</pre>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<style scoped>
.panel {
  margin-top: 16px;
}
.summary {
  margin-bottom: 8px;
}
.collapse {
  border: none;
}
.json {
  margin: 0;
  padding: 12px;
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 6px;
  font-size: 12px;
  overflow: auto;
  max-height: 360px;
}
</style>
