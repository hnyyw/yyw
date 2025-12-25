<template>
  <div style="max-width: 960px; margin: 24px auto; font-family: system-ui">
    <h2>分享访问</h2>
    <div style="display: flex; gap: 8px; align-items: center">
      <input v-model="code" placeholder="提取码（如果有）" />
      <button @click="onVerify">验证</button>
      <button @click="loadList" :disabled="!shareToken">刷新列表</button>
    </div>
    <div v-if="msg" style="margin-top: 12px; color: #c00">{{ msg }}</div>

    <div v-if="shareToken" style="margin-top: 16px">
      <h3>文件列表</h3>
      <ul>
        <li v-for="e in items" :key="e.id">
          {{ e.name }} ({{ e.type }}) {{ e.size_bytes }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const shareId = route.params.shareId as string
const code = ref('')
const msg = ref('')
const shareToken = ref('')
const items = ref<any[]>([])

const onVerify = async () => {
  msg.value = ''
  try {
    const resp = await axios.post(`/api/v1/shares/${shareId}/verify`, { code: code.value })
    shareToken.value = resp.data.data.share_token
    await loadList()
  } catch (e: any) {
    msg.value = e?.message || String(e)
  }
}

const loadList = async () => {
  msg.value = ''
  try {
    const resp = await axios.get(`/api/v1/shares/${shareId}/list`, {
      headers: { 'X-Share-Token': shareToken.value },
    })
    items.value = resp.data.data.items
  } catch (e: any) {
    msg.value = e?.message || String(e)
  }
}
</script>

