import { http } from './http'

export type EntryType = 'folder' | 'file'

export interface EntryVO {
  id: number
  parent_id: number | null
  type: EntryType
  name: string
  size_bytes: number
  is_deleted: boolean
  deleted_at: string | null
  created_at: string | null
  updated_at: string | null
}

export interface PageResult<T> {
  page: number
  page_size: number
  total: number
  items: T[]
}

export async function listFiles(params: {
  parent_id?: number | null
  page?: number
  page_size?: number
  sort?: 'name' | 'updated_at' | 'size_bytes'
  order?: 'asc' | 'desc'
  include_deleted?: 0 | 1
}) {
  const resp = await http.get('/files/list', { params })
  return resp.data.data as PageResult<EntryVO>
}

export async function mkdir(parent_id: number, name: string) {
  const resp = await http.post('/files/folders', { parentId: parent_id, name })
  return resp.data.data as { entry_id: number }
}

export async function rename(entryId: number, name: string) {
  await http.post(`/files/${entryId}/rename`, { name })
}

export async function move(entry_ids: number[], target_parent_id: number) {
  await http.post('/files/move', { entryIds: entry_ids, targetParentId: target_parent_id })
}

export async function removeToTrash(entry_ids: number[]) {
  await http.post('/files/delete', { entryIds: entry_ids })
}

export async function listTrash(page = 1, page_size = 50) {
  const resp = await http.get('/trash/list', { params: { page, page_size } })
  return resp.data.data as PageResult<EntryVO>
}

export async function restoreTrash(entry_ids: number[], strategy: 'auto_rename' | 'overwrite' | 'fail') {
  await http.post('/trash/restore', { entryIds: entry_ids, strategy })
}

