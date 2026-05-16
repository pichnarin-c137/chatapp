<script setup lang="ts">
definePageMeta({ middleware: ['guest'] })

const { login } = useAuth()
const email = ref('')
const password = ref('')
const error = ref<string | null>(null)
const loading = ref(false)

async function onSubmit() {
  error.value = null
  loading.value = true
  try {
    await login({ email: email.value, password: password.value })
    await navigateTo('/app')
  } catch (e: any) {
    error.value = e?.data?.message || e?.statusMessage || 'Login failed'
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
        <h1 class="text-2xl font-semibold">Welcome back</h1>
        <p class="text-sm text-slate-400 mt-1">Sign in to continue.</p>
      </div>

      <div class="space-y-3">
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
            autocomplete="current-password"
            class="mt-1 w-full rounded-lg bg-slate-950 border border-slate-800 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
          >
        </label>
      </div>

      <p v-if="error" class="text-sm text-rose-400">{{ error }}</p>

      <button
        type="submit"
        :disabled="loading"
        class="w-full rounded-lg bg-indigo-500 hover:bg-indigo-400 disabled:opacity-50 text-white text-sm font-medium py-2.5 transition"
      >
        {{ loading ? 'Signing in…' : 'Sign in' }}
      </button>

      <p class="text-center text-sm text-slate-400">
        No account?
        <NuxtLink to="/register" class="text-indigo-400 hover:text-indigo-300">Create one</NuxtLink>
      </p>
    </form>
  </main>
</template>
