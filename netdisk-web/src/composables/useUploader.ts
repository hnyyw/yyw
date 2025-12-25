import axios from 'axios'
import { completeUpload, initUpload, precheck } from '@/api/upload'

export type UploadStatus = 'idle' | 'hashing' | 'prechecking' | 'uploading' | 'completing' | 'done' | 'error' | 'aborted'

export interface UploadProgress {
  uploadedBytes: number
  totalBytes: number
  percent: number
}

export interface UploadTask {
  id: string
  file: File
  parentId: number
  name: string
  sha256Hex?: string
  uploadId?: string
  partSize?: number
  totalParts?: number
  etags: Map<number, string>
  status: UploadStatus
  progress: UploadProgress
  error?: string
  abortController?: AbortController
}

function toHex(buf: ArrayBuffer) {
  const bytes = new Uint8Array(buf)
  const hex: string[] = []
  for (let i = 0; i < bytes.length; i++) {
    hex.push(bytes[i].toString(16).padStart(2, '0'))
  }
  return hex.join('')
}

async function sha256(file: File, onChunk?: (done: number, total: number) => void) {
  // 简版：一次性读取（大文件会吃内存）；生产建议 WebWorker + 流式增量哈希（如 spark-md5/wasm sha256）
  const buf = await file.arrayBuffer()
  onChunk?.(buf.byteLength, buf.byteLength)
  const digest = await crypto.subtle.digest('SHA-256', buf)
  return toHex(digest)
}

async function uploadPart(url: string, blob: Blob, signal: AbortSignal) {
  const resp = await axios.put(url, blob, {
    headers: { 'Content-Type': 'application/octet-stream' },
    signal,
    // S3/MinIO 会在响应头返回 ETag
    validateStatus: (s) => s >= 200 && s < 300,
  })
  const etag = resp.headers['etag'] as string | undefined
  return (etag || '').replaceAll('"', '')
}

export function useUploader() {
  const createTask = (file: File, parentId: number): UploadTask => ({
    id: `t_${crypto.randomUUID()}`,
    file,
    parentId,
    name: file.name,
    etags: new Map(),
    status: 'idle',
    progress: { uploadedBytes: 0, totalBytes: file.size, percent: 0 },
  })

  const start = async (task: UploadTask, opts?: { concurrency?: number; conflictStrategy?: 'fail' | 'auto_rename' }) => {
    const concurrency = opts?.concurrency ?? 4
    const conflictStrategy = opts?.conflictStrategy ?? 'fail'
    const aborter = new AbortController()
    task.abortController = aborter

    try {
      task.status = 'hashing'
      task.sha256Hex = await sha256(task.file)

      task.status = 'prechecking'
      const pre = await precheck({
        parentId: task.parentId,
        name: task.name,
        sha256Hex: task.sha256Hex,
        sizeBytes: task.file.size,
        conflictStrategy,
      })
      if (!pre.need_upload) {
        task.status = 'done'
        task.progress = { uploadedBytes: task.file.size, totalBytes: task.file.size, percent: 100 }
        return
      }

      task.status = 'uploading'
      const init = await initUpload({
        parentId: task.parentId,
        name: task.name,
        sha256Hex: task.sha256Hex,
        sizeBytes: task.file.size,
        partSize: pre.part_size ?? undefined,
        conflictStrategy,
      })
      if (!init.need_upload) {
        task.status = 'done'
        task.progress = { uploadedBytes: task.file.size, totalBytes: task.file.size, percent: 100 }
        return
      }

      task.uploadId = init.upload_id!
      task.partSize = init.part_size!
      task.totalParts = init.total_parts!

      const parts = init.parts ?? []
      const total = task.file.size
      let uploadedBytes = 0

      // 简单并发池
      const queue = parts.map((p) => p.part_number)
      const running: Promise<void>[] = []

      const runOne = async () => {
        const partNumber = queue.shift()
        if (!partNumber) return
        const p = parts.find((x) => x.part_number === partNumber)!
        const start = (partNumber - 1) * task.partSize!
        const end = Math.min(start + task.partSize!, total)
        const chunk = task.file.slice(start, end)

        // 失败重试（2 次）
        let lastErr: any
        for (let attempt = 0; attempt < 3; attempt++) {
          try {
            const etag = await uploadPart(p.url, chunk, aborter.signal)
            task.etags.set(partNumber, etag)
            uploadedBytes += chunk.size
            task.progress = {
              uploadedBytes,
              totalBytes: total,
              percent: Math.floor((uploadedBytes / total) * 100),
            }
            return
          } catch (e) {
            lastErr = e
            await new Promise((r) => setTimeout(r, 300 * (attempt + 1)))
          }
        }
        throw lastErr
      }

      for (let i = 0; i < concurrency; i++) {
        const worker = (async () => {
          while (queue.length > 0) {
            await runOne()
          }
        })()
        running.push(worker)
      }
      await Promise.all(running)

      task.status = 'completing'
      const partsForComplete = Array.from(task.etags.entries())
        .sort((a, b) => a[0] - b[0])
        .map(([partNumber, etag]) => ({ partNumber, etag }))
      await completeUpload({
        uploadId: task.uploadId,
        sha256Hex: task.sha256Hex,
        parts: partsForComplete,
      })

      task.status = 'done'
      task.progress = { uploadedBytes: total, totalBytes: total, percent: 100 }
    } catch (e: any) {
      if (aborter.signal.aborted) {
        task.status = 'aborted'
        return
      }
      task.status = 'error'
      task.error = e?.message || String(e)
      throw e
    }
  }

  const abort = (task: UploadTask) => {
    task.abortController?.abort()
    task.status = 'aborted'
  }

  return { createTask, start, abort }
}

