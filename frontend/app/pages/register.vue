<script setup lang="ts">
import { DEFAULT_TIMEZONE, detectBrowserTimezone } from '~/utils/time'

definePageMeta({ middleware: ['guest'] })

const { register } = useAuth()
const username = ref('')
const email = ref('')
const password = ref('')
const timezone = ref(DEFAULT_TIMEZONE)
const error = ref<string | null>(null)
const loading = ref(false)

const browserTz = detectBrowserTimezone()

async function onSubmit() {
  error.value = null
  loading.value = true
  try {
    await register({
      username: username.value,
      email: email.value,
      password: password.value,
      timezone: timezone.value,
    })
    await navigateTo('/app')
  } catch (e: any) {
    error.value = e?.data?.message || e?.statusMessage || 'Registration failed'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center p-6">
    <form
      class="w-full max-w-sm space-y-5 rounded-2xl border border-slate-800 bg-slate-900/50 p-8 shadow-xl"
      @submit.prevent="onSubmit"
    >
      <div>
        <h1 class="text-2xl font-semibold">Create account</h1>
        <p class="text-sm text-slate-400 mt-1">Get started with the chat.</p>
      </div>

      <div class="space-y-3">
        <label class="block">
          <span class="text-xs uppercase tracking-wide text-slate-400">Username</span>
          <input
            v-model="username"
            type="text"
            required
            minlength="3"
            maxlength="32"
            autocomplete="username"
            class="mt-1 w-full rounded-lg bg-slate-950 border border-slate-800 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
          >
        </label>
        <label class="block">
          <span class="text-xs uppercase tracking-wide text-slate-400">Email</span>
          <input
            v-model="email"
            type="email"
            required
            autocomplete="email"
            class="mt-1 w-full rounded-lg bg-slate-950 border border-slate-800 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
          >
        </label>
        <label class="block">
          <span class="text-xs uppercase tracking-wide text-slate-400">Password</span>
          <input
            v-model="password"
            type="password"
            required
            minlength="8"
            autocomplete="new-password"
            class="mt-1 w-full rounded-lg bg-slate-950 border border-slate-800 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
          >
        </label>
        <label class="block">
          <span class="text-xs uppercase tracking-wide text-slate-400">Timezone</span>
          <select
            v-model="timezone"
            class="mt-1 w-full rounded-lg bg-slate-950 border border-slate-800 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
          >
            <option value="Asia/Phnom_Penh">Asia/Phnom_Penh (default)</option>
            <option v-if="browserTz !== 'Asia/Phnom_Penh'" :value="browserTz">
              {{ browserTz }} (detected)
            </option>
            <option value="UTC">UTC</option>
            <option value="Asia/Bangkok">Asia/Bangkok</option>
            <option value="Asia/Singapore">Asia/Singapore</option>
            <option value="Asia/Tokyo">Asia/Tokyo</option>
            <option value="Europe/London">Europe/London</option>
            <option value="America/New_York">America/New_York</option>
          </select>
        </label>
      </div>

      <p v-if="error" class="text-sm text-rose-400">{{ error }}</p>

      <button
        type="submit"
        :disabled="loading"
        class="w-full rounded-lg bg-indigo-500 hover:bg-indigo-400 disabled:opacity-50 text-white text-sm font-medium py-2.5 transition"
      >
        {{ loading ? 'Creating…' : 'Create account' }}
      </button>

      <p class="text-center text-sm text-slate-400">
        Already have an account?
        <NuxtLink to="/login" class="text-indigo-400 hover:text-indigo-300">Sign in</NuxtLink>
      </p>
    </form>
  </main>
</template>
