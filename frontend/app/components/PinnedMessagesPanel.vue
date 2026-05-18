<script setup lang="ts">
import type { Pin } from '~/types/conversation'
import { formatTime } from '~/utils/time'
import { useChat } from '~/composables/useChat'

const props = defineProps<{
  conversationId: string
  pins: Pin[]
  timezone: string
  open: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const { unpin } = useChat()

async function onUnpin(messageId: string) {
  await unpin(props.conversationId, messageId)
}
</script>

<template>
  <Transition
    enter-active-class="transition duration-150"
    enter-from-class="opacity-0 translate-x-2"
    enter-to-class="opacity-100 translate-x-0"
    leave-active-class="transition duration-150"
    leave-from-class="opacity-100 translate-x-0"
    leave-to-class="opacity-0 translate-x-2"
  >
    <aside
      v-if="open"
      class="absolute top-0 right-0 z-30 w-80 max-w-full h-full bg-slate-900/95 backdrop-blur border-l border-slate-800 shadow-xl flex flex-col"
    >
      <header class="px-4 py-3 border-b border-slate-800 flex items-center justify-between">
        <div class="text-sm font-semibold">Pinned messages</div>
        <button
          class="text-slate-400 hover:text-slate-100 text-xs px-2 py-1 rounded hover:bg-slate-800"
          @click="emit('close')"
        >
          ✕
        </button>
      </header>

      <div class="flex-1 overflow-y-auto divide-y divide-slate-800">
        <div
          v-if="pins.length === 0"
          class="px-4 py-6 text-xs text-slate-500 text-center"
        >
          Nothing pinned yet.
        </div>

        <article
          v-for="p in pins"
          :key="p.messageId"
          class="px-4 py-3 group"
        >
          <header class="flex items-center justify-between mb-1.5">
            <span class="text-xs text-slate-400">
              @{{ p.message?.senderUsername ?? 'unknown' }}
              · <span class="font-mono">{{ formatTime(p.message?.sentAt ?? p.pinnedAt, timezone) }}</span>
            </span>
            <button
              class="text-[10px] uppercase text-slate-500 hover:text-rose-300 transition opacity-0 group-hover:opacity-100"
              @click="onUnpin(p.messageId)"
            >
              Unpin
            </button>
          </header>
          <div class="text-sm text-slate-100 whitespace-pre-wrap break-words">
            <span v-if="p.message?.deletedAt" class="italic text-slate-500">
              This message was deleted
            </span>
            <span v-else>{{ p.message?.body }}</span>
          </div>
        </article>
      </div>
    </aside>
  </Transition>
</template>
