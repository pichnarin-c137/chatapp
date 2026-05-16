<script setup lang="ts">
interface PaneMessage {
  id: string
  senderId: string
  senderUsername: string
  content: string
  sentAt: string
}

const props = defineProps<{
  title: string
  subtitle?: string
  avatar?: string
  messages: PaneMessage[]
  currentUserId: string | null | undefined
  timezone: string
  placeholder?: string
  disabled?: boolean
  showBack?: boolean
  backTo?: string
}>()

const emit = defineEmits<{
  (e: 'send', content: string): void
}>()

const text = ref('')
const listRef = ref<HTMLElement | null>(null)

async function scrollToBottom() {
  await nextTick()
  if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
}

onMounted(scrollToBottom)
watch(() => props.messages, scrollToBottom, { deep: false, flush: 'post' })

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    submit()
  }
}

function submit() {
  const v = text.value.trim()
  if (!v) return
  emit('send', v)
  text.value = ''
}

function initials(name: string) {
  return name.slice(0, 2).toUpperCase()
}
</script>

<template>
  <div class="flex flex-col h-full bg-slate-950">
    <header class="border-b border-slate-800 px-4 py-3 flex items-center gap-3">
      <NuxtLink
        v-if="showBack && backTo"
        :to="backTo"
        class="md:hidden text-slate-400 hover:text-slate-100 transition p-1.5 rounded-md hover:bg-slate-800"
        aria-label="Back"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>
      </NuxtLink>
      <div class="size-10 rounded-full bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center text-indigo-200 text-sm font-semibold shrink-0">
        {{ avatar ?? initials(title) }}
      </div>
      <div class="min-w-0">
        <div class="text-sm font-semibold leading-tight truncate">{{ title }}</div>
        <div v-if="subtitle" class="text-xs text-slate-400 truncate">{{ subtitle }}</div>
      </div>
    </header>

    <main
      ref="listRef"
      class="flex-1 overflow-y-auto px-4 sm:px-6 py-4 space-y-3"
    >
      <div
        v-if="messages.length === 0"
        class="h-full flex items-center justify-center text-sm text-slate-500"
      >
        No messages yet — say hi.
      </div>

      <MessageBubble
        v-for="(m, i) in messages"
        :key="m.id"
        :message="m"
        :is-own="m.senderId === currentUserId"
        :show-sender="i === 0 || messages[i - 1]?.senderId !== m.senderId"
        :timezone="timezone"
      />
    </main>

    <footer class="border-t border-slate-800 px-4 sm:px-6 py-3">
      <div class="flex items-end gap-2 bg-slate-900/60 border border-slate-800 rounded-2xl px-3 py-2 focus-within:border-indigo-500/60 transition">
        <textarea
          v-model="text"
          rows="1"
          :placeholder="placeholder ?? 'Message'"
          class="flex-1 resize-none bg-transparent outline-none text-sm placeholder:text-slate-500 max-h-32"
          @keydown="onKeydown"
        ></textarea>
        <button
          class="text-sm font-medium rounded-lg bg-indigo-500 hover:bg-indigo-400 disabled:opacity-40 disabled:cursor-not-allowed text-white px-3 py-1.5 transition"
          :disabled="disabled || !text.trim()"
          @click="submit"
        >
          Send
        </button>
      </div>
      <p class="mt-1.5 text-[10px] text-slate-600 px-3">
        Enter to send, Shift+Enter for new line
      </p>
    </footer>
  </div>
</template>
