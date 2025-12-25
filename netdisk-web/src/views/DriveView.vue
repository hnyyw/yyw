<template>
  <div style="max-width: 960px; margin: 24px auto; font-family: system-ui">
    <div style="display: flex; gap: 12px; align-items: center; justify-content: space-between">
      <h2>我的网盘</h2>
      <div style="display: flex; gap: 8px">
        <button @click="router.push('/login')">切换账号</button>
        <button @click="load">刷新</button>
      </div>
    </div>

    <div style="display: flex; gap: 8px; margin: 12px 0">
      <input v-model="newFolderName" placeholder="新建文件夹名" />
      <button @click="onMkdir">新建文件夹</button>
      <input type="file" @change="onPickFile" />
    </div>

    <div v-if="uploadTask" style="border: 1px solid #ddd; padding: 8px; margin: 12px 0">
      <div>上传：{{ uploadTask.name }}（{{ uploadTask.status }}）</div>
      <div>进度：{{ uploadTask.progress.percent }}% ({{ uploadTask.progress.uploadedBytes }}/{{ uploadTask.progress.totalBytes }})</div>
      <div v-if="uploadTask.error" style="color: #c00">错误：{{ uploadTask.error }}</div>
      <button v-if="uploadTask.status === 'uploading'" @click="abortUploadTask">取消上传</button>
    </div>

    <table width="100%" cellpadding="8" cellspacing="0" style="border-collapse: collapse">
      <thead>
        <tr style="text-align: left; border-bottom: 1px solid #ddd">
          <th>名称</th>
          <th>类型</th>
          <th>大小</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="e in items" :key="e.id" style="border-bottom: 1px solid #f0f0f0">
          <td>{{ e.name }}</td>
          <td>{{ e.type }}</td>
          <td>{{ e.size_bytes }}</td>
          <td style="display: flex; gap: 8px">
            <button v-if="e.type === 'file'" @click="onDownload(e.id)">下载</button>
            <button @click="onDelete(e.id)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="msg" style="margin-top: 12px; color: #c00">{{ msg }}</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { EntryVO } from '@/api/files'
import { listFiles, mkdir, removeToTrash } from '@/api/files'
import { getDownloadLink } from '@/api/download'
import { useUploader, type UploadTask } from '@/composables/useUploader'

const router = useRouter()
const items = ref<EntryVO[]>([])
const msg = ref('')
const newFolderName = ref('新建文件夹')
const parentId = 0

const { createTask, start, abort } = useUploader()
const uploadTask = ref<UploadTask | null>(null)

const load = async () => {
  msg.value = ''
  try {
    const r = await listFiles({ parent_id: parentId, page: 1, page_size: 50 })
    items.value = r.items
  } catch (e: any) {
    msg.value = e?.message || String(e)
    if ((e?.message || '').includes('未登录')) {
      router.push('/login')
    }
  }
}

const onMkdir = async () => {
  msg.value = ''
  try {
    await mkdir(parentId, newFolderName.value)
    await load()
  } catch (e: any) {
    msg.value = e?.message || String(e)
  }
}

const onPickFile = async (ev: Event) => {
  const input = ev.target as HTMLInputElement
  const f = input.files?.[0]
  if (!f) return
  uploadTask.value = createTask(f, parentId)
  try {
    await start(uploadTask.value, { concurrency: 4, conflictStrategy: 'auto_rename' })
    await load()
  } catch (e) {
    // 错误已写入 task
  } finally {
    input.value = ''
  }
}

const abortUploadTask = () => {
  if (uploadTask.value) abort(uploadTask.value)
}

const onDelete = async (entryId: number) => {
  msg.value = ''
  try {
    await removeToTrash([entryId])
    await load()
  } catch (e: any) {
    msg.value = e?.message || String(e)
  }
}

const onDownload = async (entryId: number) => {
  msg.value = ''
  try {
    const { url } = await getDownloadLink(entryId)
    window.location.href = url
  } catch (e: any) {
    msg.value = e?.message || String(e)
  }
}

onMounted(load)
</script>

