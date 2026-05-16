<script setup lang="ts">
const auth = useAuthStore()

if (import.meta.client && auth.isAuthenticated) {
  navigateTo('/app', { replace: true })
}
</script>

<template>
  <div class="min-h-dvh bg-slate-950 text-slate-100 relative overflow-hidden">
    <div
      aria-hidden="true"
      class="pointer-events-none absolute inset-0 opacity-60"
      style="background:
        radial-gradient(ellipse 60% 50% at 20% 0%, rgba(56, 189, 248, 0.18), transparent 60%),
        radial-gradient(ellipse 50% 50% at 80% 30%, rgba(99, 102, 241, 0.22), transparent 60%),
        radial-gradient(ellipse 80% 40% at 50% 100%, rgba(99, 102, 241, 0.12), transparent 60%)"
    ></div>
    <div
      aria-hidden="true"
      class="pointer-events-none absolute inset-0 opacity-[0.025]"
      style="background-image:
        linear-gradient(rgba(255,255,255,0.5) 1px, transparent 1px),
        linear-gradient(90deg, rgba(255,255,255,0.5) 1px, transparent 1px);
      background-size: 32px 32px;"
    ></div>

    <header class="relative z-10 px-6 sm:px-10 py-5 flex items-center justify-between">
      <NuxtLink to="/" class="flex items-center gap-2.5">
        <div class="size-9 rounded-xl bg-gradient-to-br from-sky-400 to-indigo-500 flex items-center justify-center shadow-lg shadow-indigo-500/30">
          <svg xmlns="http://www.w3.org/2000/svg" class="size-4 text-white" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21.5 4.5 2.7 12.2c-.9.4-.9 1.6 0 2l3.8 1.5L18 8.5c.4-.2.7.3.4.6L9 18.4v3.1c0 .8.9 1.1 1.4.5l2.7-3 5.3 2.1c.7.3 1.4-.1 1.6-.8L22.5 5.6c.2-.7-.4-1.4-1-1.1z"/>
          </svg>
        </div>
        <span class="text-sm font-semibold tracking-tight">Chat</span>
      </NuxtLink>

      <nav class="flex items-center gap-1.5">
        <NuxtLink
          to="/login"
          class="text-xs sm:text-sm text-slate-300 hover:text-white transition px-3 py-2 rounded-lg hover:bg-slate-800/60"
        >
          Log in
        </NuxtLink>
        <NuxtLink
          to="/register"
          class="text-xs sm:text-sm font-medium text-white bg-indigo-500 hover:bg-indigo-400 transition px-3.5 py-2 rounded-lg shadow-sm shadow-indigo-500/30"
        >
          Sign up
        </NuxtLink>
      </nav>
    </header>

    <main class="relative z-10 px-6 sm:px-10 pt-12 sm:pt-20 pb-16 max-w-5xl mx-auto text-center">
      <div class="inline-flex items-center justify-center mb-7">
        <div class="size-20 rounded-3xl bg-gradient-to-br from-sky-400 to-indigo-500 flex items-center justify-center shadow-2xl shadow-indigo-500/30 ring-1 ring-white/10">
          <svg xmlns="http://www.w3.org/2000/svg" class="size-10 text-white" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21.5 4.5 2.7 12.2c-.9.4-.9 1.6 0 2l3.8 1.5L18 8.5c.4-.2.7.3.4.6L9 18.4v3.1c0 .8.9 1.1 1.4.5l2.7-3 5.3 2.1c.7.3 1.4-.1 1.6-.8L22.5 5.6c.2-.7-.4-1.4-1-1.1z"/>
          </svg>
        </div>
      </div>

      <div class="inline-flex items-center gap-1.5 mb-5 text-[10px] uppercase tracking-widest text-indigo-300 bg-indigo-500/10 border border-indigo-500/20 rounded-full px-3 py-1">
        <span class="size-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
        Real-time · No-fluff
      </div>

      <h1 class="text-4xl sm:text-6xl font-bold tracking-tight leading-[1.05] bg-gradient-to-b from-white to-slate-400 bg-clip-text text-transparent">
        A simpler way<br class="hidden sm:block" />
        to chat.
      </h1>

      <p class="mt-6 max-w-xl mx-auto text-sm sm:text-base text-slate-400 leading-relaxed">
        A public lobby, private messages by
        <span class="text-indigo-300 font-medium">@username</span>,
        and real-time delivery. Built lean, runs anywhere.
      </p>

      <div class="mt-9 flex flex-col sm:flex-row items-center justify-center gap-2.5">
        <NuxtLink
          to="/register"
          class="w-full sm:w-auto text-sm font-medium text-white bg-indigo-500 hover:bg-indigo-400 transition px-6 py-3 rounded-xl shadow-lg shadow-indigo-500/30 flex items-center justify-center gap-2"
        >
          Create an account
          <svg xmlns="http://www.w3.org/2000/svg" class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
        </NuxtLink>
        <NuxtLink
          to="/login"
          class="w-full sm:w-auto text-sm font-medium text-slate-200 bg-slate-800/60 hover:bg-slate-800 border border-slate-700 transition px-6 py-3 rounded-xl flex items-center justify-center"
        >
          I already have an account
        </NuxtLink>
      </div>
    </main>

    <section class="relative z-10 px-6 sm:px-10 pb-20 max-w-5xl mx-auto">
      <div class="grid sm:grid-cols-3 gap-3 sm:gap-4">
        <div class="rounded-2xl border border-slate-800 bg-slate-900/40 backdrop-blur-sm px-5 py-5">
          <div class="size-9 rounded-lg bg-emerald-500/15 border border-emerald-500/25 flex items-center justify-center mb-3.5">
            <svg xmlns="http://www.w3.org/2000/svg" class="size-4 text-emerald-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2 3 14h9l-1 8 10-12h-9l1-8z"/></svg>
          </div>
          <h3 class="text-sm font-semibold text-slate-100">Instant delivery</h3>
          <p class="mt-1 text-xs text-slate-400 leading-relaxed">
            WebSocket + STOMP. Messages land in milliseconds, not seconds.
          </p>
        </div>

        <div class="rounded-2xl border border-slate-800 bg-slate-900/40 backdrop-blur-sm px-5 py-5">
          <div class="size-9 rounded-lg bg-indigo-500/15 border border-indigo-500/25 flex items-center justify-center mb-3.5">
            <svg xmlns="http://www.w3.org/2000/svg" class="size-4 text-indigo-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
          </div>
          <h3 class="text-sm font-semibold text-slate-100">Find anyone</h3>
          <p class="mt-1 text-xs text-slate-400 leading-relaxed">
            Search by <span class="text-indigo-300">@username</span>. Click → DM. No phone numbers, no friction.
          </p>
        </div>

        <div class="rounded-2xl border border-slate-800 bg-slate-900/40 backdrop-blur-sm px-5 py-5">
          <div class="size-9 rounded-lg bg-sky-500/15 border border-sky-500/25 flex items-center justify-center mb-3.5">
            <svg xmlns="http://www.w3.org/2000/svg" class="size-4 text-sky-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
          </div>
          <h3 class="text-sm font-semibold text-slate-100">Your timezone</h3>
          <p class="mt-1 text-xs text-slate-400 leading-relaxed">
            Timestamps respect your timezone. Defaults to Asia/Phnom_Penh.
          </p>
        </div>
      </div>
    </section>

    <footer class="relative z-10 border-t border-slate-900 px-6 sm:px-10 py-6 text-center text-xs text-slate-500">
      Built lean. Spring Boot + Nuxt. Open the
      <NuxtLink to="/register" class="text-slate-300 hover:text-white">Lobby</NuxtLink>
      to see it live.
    </footer>
  </div>
</template>
