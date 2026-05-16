<script setup lang="ts">
definePageMeta({ middleware: ['auth'], layout: 'chat' })
</script>

<template>
  <div class="h-full flex items-center justify-center bg-slate-950">
    <div class="text-center px-6 max-w-sm">
      <div class="mx-auto size-16 rounded-2xl bg-gradient-to-br from-sky-400 to-indigo-500 flex items-center justify-center shadow-lg shadow-indigo-500/20">
        <svg xmlns="http://www.w3.org/2000/svg" class="size-8 text-white" viewBox="0 0 24 24" fill="currentColor">
          <path d="M21.5 4.5 2.7 12.2c-.9.4-.9 1.6 0 2l3.8 1.5L18 8.5c.4-.2.7.3.4.6L9 18.4v3.1c0 .8.9 1.1 1.4.5l2.7-3 5.3 2.1c.7.3 1.4-.1 1.6-.8L22.5 5.6c.2-.7-.4-1.4-1-1.1z"/>
        </svg>
      </div>
      <h1 class="mt-4 text-base font-semibold text-slate-200">Select a chat to start messaging</h1>
      <p class="mt-1.5 text-xs text-slate-500">
        Pick the Lobby from the sidebar, choose a direct message, or search for a user with
        <span class="text-indigo-300">@username</span>.
      </p>
    </div>
  </div>
</template>
