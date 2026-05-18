<script setup lang="ts">
import type { Message } from '~/types/conversation'
import { useConversationStore } from '~/stores/conversation'
import { useChat } from '~/composables/useChat'

const props = defineProps<{
  conversationId: string
  title: string
  subtitle?: string
  avatar?: string
  messages: Message[]
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

const store = useConversationStore()
const { cancelReply, loadPins } = useChat()

const text = ref('')
const listRef = ref<HTMLElement | null>(null)
const pinPanelOpen = ref(false)
const forwardTarget = ref<Message | null>(null)

const replyDraft = computed(() => store.replyDraftFor(props.conversationId))
const pins = computed(() => store.pinsFor(props.conversationId))
const latestPin = computed(() => pins.value[0] ?? null)

async function scrollToBottom() {
  await nextTick()
  if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
}

onMounted(async () => {
  scrollToBottom()
  // Load pins eagerly so the bubble's "Pin/Unpin" label is correct on first open
  // and the pinned banner shows without waiting for the panel to be opened.
  try {
    await loadPins(props.conversationId)
  } catch (e) {
    // Non-fatal — banner just stays empty.
    console.debug('loadPins failed', e)
  }
})

// Re-load pins on conversation switch.
watch(() => props.conversationId, async (id) => {
  try { await loadPins(id) } catch {}
})

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

async function togglePinPanel() {
  if (!pinPanelOpen.value) {
    try {
      await loadPins(props.conversationId)
    } catch (e) {
      console.error('loadPins failed', e)
    }
  }
  pinPanelOpen.value = !pinPanelOpen.value
}

function onForwardRequest(msg: Message) {
  forwardTarget.value = msg
}

function previewReplyToMessage() {
  return replyDraft.value
}

const _ = previewReplyToMessage  // satisfy linters; expression kept for clarity above

function jumpToPin() {
  if (!latestPin.value) return
  const el = document.getElementById(`msg-${latestPin.value.messageId}`)
  if (el && listRef.value) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}
</script>

<template>
  <div class="flex flex-col h-full bg-slate-950 relative">
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
      <div class="min-w-0 flex-1">
        <div class="text-sm font-semibold leading-tight truncate">{{ title }}</div>
        <div v-if="subtitle" class="text-xs text-slate-400 truncate">{{ subtitle }}</div>
      </div>
      <button
        class="text-slate-400 hover:text-slate-100 text-xs px-2 py-1.5 rounded hover:bg-slate-800 flex items-center gap-1"
        :aria-pressed="pinPanelOpen"
        aria-label="Pinned messages"
        @click="togglePinPanel"
      >
        <span class="text-base leading-none">📌</span>
        <span v-if="pins.length > 0" class="text-[10px]">{{ pins.length }}</span>
      </button>
    </header>

    <!-- Latest pin banner -->
    <button
      v-if="latestPin && !pinPanelOpen"
      class="w-full text-left px-4 py-2 border-b border-slate-800 bg-amber-500/5 hover:bg-amber-500/10 transition flex items-start gap-2"
      @click="jumpToPin"
    >
      <span class="text-amber-400 text-xs mt-0.5">📌</span>
      <div class="min-w-0 flex-1">
        <div class="text-[10px] uppercase tracking-wider text-amber-300/80 font-semibold">Pinned</div>
        <div class="text-xs text-slate-300 truncate">
          {{ latestPin.message?.deletedAt ? 'message deleted' : (latestPin.message?.body || '') }}
        </div>
      </div>
    </button>

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

      <div v-for="(m, i) in messages" :key="m.tempId ?? m.id" :id="`msg-${m.id}`">
        <MessageBubble
          :conversation-id="conversationId"
          :message="m"
          :is-own="m.senderId === currentUserId"
          :show-sender="i === 0 || messages[i - 1]?.senderId !== m.senderId"
          :timezone="timezone"
          @forward-request="onForwardRequest"
        />
      </div>
    </main>

    <footer class="border-t border-slate-800 px-4 sm:px-6 py-3">
      <ReplyPreview
        v-if="replyDraft"
        :reply="{
          id: replyDraft.id,
          senderId: replyDraft.senderId,
          senderUsername: replyDraft.senderUsername,
          body: replyDraft.body,
          deleted: !!replyDraft.deletedAt,
        }"
        variant="input"
        dismissable
        @cancel="cancelReply(conversationId)"
      />
      <div class="flex items-end gap-2 bg-slate-900/60 border border-slate-800 rounded-2xl px-3 py-2 focus-within:border-indigo-500/60 transition">
        <textarea
          v-model="text"
          rows="1"
          :placeholder="placeholder ?? 'Message'"
          class="flex-1 resize-none bg-transparent outline-none text-sm placeholder:text-slate-500 max-h-32"
          @keydown="onKeydown"
        />
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

    <PinnedMessagesPanel
      :conversation-id="conversationId"
      :pins="pins"
      :timezone="timezone"
      :open="pinPanelOpen"
      @close="pinPanelOpen = false"
    />

    <ForwardMessageModal
      :message="forwardTarget"
      @close="forwardTarget = null"
      @forwarded="forwardTarget = null"
    />
  </div>
</template>
