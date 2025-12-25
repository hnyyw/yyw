<template>
  <div style="max-width: 360px; margin: 80px auto; font-family: system-ui">
    <h2>登录</h2>
    <div style="display: grid; gap: 8px">
      <input v-model="email" placeholder="邮箱" />
      <input v-model="password" placeholder="密码" type="password" />
      <button @click="onLogin">登录</button>
      <button @click="onRegister">注册（测试用）</button>
      <div v-if="msg" style="color: #c00">{{ msg }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login, register } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const user = useUserStore()
const email = ref('test@example.com')
const password = ref('12345678')
const msg = ref('')

const onLogin = async () => {
  msg.value = ''
  try {
    const r = await login(email.value, password.value)
    user.setToken(r.access_token)
    await router.push('/drive')
  } catch (e: any) {
    msg.value = e?.message || String(e)
  }
}

const onRegister = async () => {
  msg.value = ''
  try {
    await register(email.value, password.value)
    msg.value = '注册成功，请点击登录'
  } catch (e: any) {
    msg.value = e?.message || String(e)
  }
}
</script>

