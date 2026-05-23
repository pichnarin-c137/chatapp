<script setup lang="ts">
import { useAuthStore } from '~/stores/auth'
import { useTypingStore } from '~/stores/typing'

const props = defineProps<{
  conversationId: string
}>()

const typingStore = useTypingStore()
const auth = useAuthStore()

const typers = computed(() => typingStore.typersFor(props.conversationId, auth.user?.id ?? null))

const label = computed(() => {
  const t = typers.value
  if (t.length === 0) return ''
  if (t.length === 1) return `${t[0]!.username} is typing`
  if (t.length === 2) return `${t[0]!.username} and ${t[1]!.username} are typing`
  return `${t[0]!.username} and ${t.length - 1} others are typing`
})
</script>

<template>
  <div
    v-if="typers.length > 0"
    class="px-4 py-1 text-[11px] text-slate-400 flex items-center gap-2 transition"
  >
    <span class="flex items-center gap-0.5">
      <span class="size-1.5 rounded-full bg-slate-400 animate-bounce" style="animation-delay: 0ms"></span>
      <span class="size-1.5 rounded-full bg-slate-400 animate-bounce" style="animation-delay: 120ms"></span>
      <span class="size-1.5 rounded-full bg-slate-400 animate-bounce" style="animation-delay: 240ms"></span>
    </span>
    <span>{{ label }}…</span>
  </div>
</template>
