import { http } from './http'

export interface PrecheckResp {
  need_upload: boolean
  entry_id?: number | null
  part_size?: number | null
}

export interface InitUploadResp {
  need_upload: boolean
  entry_id?: number | null
  upload_id?: string
  part_size?: number
  total_parts?: number
  expire_at?: string
  parts?: { part_number: number; url: string }[]
}

export interface UploadSessionResp {
  upload_id: string
  target_parent_id: number
  target_name: string
  sha256_hex: string
  size_bytes: number
  part_size: number
  total_parts: number
  status: string
  uploaded_parts: { part_number: number; etag: string }[]
}

export async function precheck(params: {
  parentId: number
  name: string
  sha256Hex: string
  sizeBytes: number
  conflictStrategy?: 'fail' | 'auto_rename'
}) {
  const resp = await http.post('/uploads/precheck', {
    parentId: params.parentId,
    name: params.name,
    sha256Hex: params.sha256Hex,
    sizeBytes: params.sizeBytes,
    conflictStrategy: params.conflictStrategy ?? 'fail',
  })
  return resp.data.data as PrecheckResp
}

export async function initUpload(params: {
  parentId: number
  name: string
  sha256Hex: string
  sizeBytes: number
  partSize?: number
  conflictStrategy?: 'fail' | 'auto_rename'
}) {
  const resp = await http.post('/uploads/init', {
    parentId: params.parentId,
    name: params.name,
    sha256Hex: params.sha256Hex,
    sizeBytes: params.sizeBytes,
    partSize: params.partSize,
    conflictStrategy: params.conflictStrategy ?? 'fail',
  })
  return resp.data.data as InitUploadResp
}

export async function getUploadSession(uploadId: string) {
  const resp = await http.get(`/uploads/${uploadId}`)
  return resp.data.data as UploadSessionResp
}

export async function completeUpload(params: {
  uploadId: string
  sha256Hex: string
  parts: { partNumber: number; etag: string }[]
}) {
  const resp = await http.post('/uploads/complete', {
    uploadId: params.uploadId,
    sha256Hex: params.sha256Hex,
    parts: params.parts.map((p) => ({ partNumber: p.partNumber, etag: p.etag })),
  })
  return resp.data.data as { entry_id: number; blob_id: number }
}

export async function abortUpload(uploadId: string) {
  await http.post(`/uploads/${uploadId}/abort`)
}

