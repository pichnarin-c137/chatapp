<script setup lang="ts">
import type { Conversation, Message } from '~/types/conversation'
import { useConversationStore } from '~/stores/conversation'
import { useChat } from '~/composables/useChat'

const props = defineProps<{
  message: Message | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'forwarded', created: Message[]): void
}>()

const store = useConversationStore()
const { forward } = useChat()

const selected = ref<Set<string>>(new Set())
const sending = ref(false)
const error = ref<string | null>(null)

const targets = computed<Conversation[]>(() => store.conversations)

watch(
  () => props.message?.id,
  () => {
    selected.value = new Set()
    error.value = null
  },
)

function toggle(id: string) {
  if (selected.value.has(id)) selected.value.delete(id)
  else selected.value.add(id)
  // trigger reactivity since Set mutation isn't auto-reactive
  selected.value = new Set(selected.value)
}

function label(c: Conversation) {
  if (c.type === 'DIRECT') return c.dmOther?.username ? `@${c.dmOther.username}` : 'Direct message'
  return c.name ?? 'Conversation'
}

async function onSubmit() {
  if (!props.message) return
  if (selected.value.size === 0) {
    error.value = 'Pick at least one conversation'
    return
  }
  sending.value = true
  error.value = null
  try {
    const created = await forward(props.message.id, [...selected.value])
    emit('forwarded', created)
    emit('close')
  } catch (e: any) {
    error.value = e?.data?.message || e?.message || 'Forward failed'
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="message"
      class="fixed inset-0 z-40 flex items-center justify-center bg-slate-950/60 p-4"
      @click.self="emit('close')"
    >
      <div class="w-full max-w-md rounded-xl border border-slate-800 bg-slate-900 shadow-2xl flex flex-col">
        <header class="px-4 py-3 border-b border-slate-800 flex items-center justify-between">
          <div class="text-sm font-semibold">Forward message</div>
          <button
            class="text-slate-400 hover:text-slate-100 text-xs px-2 py-1 rounded hover:bg-slate-800"
            @click="emit('close')"
          >
            ✕
          </button>
        </header>

        <div class="px-4 py-3 border-b border-slate-800 text-xs text-slate-400">
          <div class="border-l-2 border-slate-600 pl-2">
            <div class="font-semibold text-slate-300">@{{ message.senderUsername }}</div>
            <div class="truncate">{{ message.body }}</div>
          </div>
        </div>

        <div class="flex-1 max-h-72 overflow-y-auto divide-y divide-slate-800">
          <div v-if="targets.length === 0" class="px-4 py-6 text-xs text-slate-500 text-center">
            No conversations to forward to.
          </div>
          <button
            v-for="c in targets"
            :key="c.id"
            class="w-full flex items-center gap-3 px-4 py-2.5 hover:bg-slate-800/60 transition text-left"
            @click="toggle(c.id)"
          >
            <span
              :class="[
                'size-4 rounded border flex items-center justify-center text-[10px]',
                selected.has(c.id)
                  ? 'bg-indigo-500 border-indigo-500 text-white'
                  : 'border-slate-600',
              ]"
            >
              <span v-if="selected.has(c.id)">✓</span>
            </span>
            <span class="text-sm truncate">{{ label(c) }}</span>
          </button>
        </div>

        <footer class="px-4 py-3 border-t border-slate-800 flex items-center justify-between gap-2">
          <span class="text-[11px] text-rose-300">{{ error ?? '' }}</span>
          <div class="flex items-center gap-2">
            <span class="text-[11px] text-slate-500">{{ selected.size }} selected</span>
            <button
              class="text-xs px-3 py-1.5 rounded bg-indigo-500 hover:bg-indigo-400 disabled:opacity-40 disabled:cursor-not-allowed text-white font-medium"
              :disabled="sending || selected.size === 0"
              @click="onSubmit"
            >
              {{ sending ? 'Forwarding…' : 'Forward' }}
            </button>
          </div>
        </footer>
      </div>
    </div>
  </Teleport>
</template>
