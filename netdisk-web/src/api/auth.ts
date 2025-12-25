import { http } from './http'

export async function register(email: string, password: string) {
  const resp = await http.post('/auth/register', { email, password })
  return resp.data.data as { user_id: number }
}

export async function login(email: string, password: string) {
  const resp = await http.post('/auth/login', { email, password })
  return resp.data.data as { access_token: string; token_type: string; expire_in: number }
}

