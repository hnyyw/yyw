import { http } from './http'

export async function getDownloadLink(entryId: number) {
  const resp = await http.post(`/files/${entryId}/download-link`)
  return resp.data.data as { url: string; expire_in: number }
}

