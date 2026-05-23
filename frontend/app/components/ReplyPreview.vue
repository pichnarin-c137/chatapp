<script setup lang="ts">
import type { ReplyPreview } from '~/types/conversation'

const props = defineProps<{
  reply: ReplyPreview
  dismissable?: boolean
  /** "inline" renders the small quote-block style used inside a bubble.
      "input" renders the strip above the textarea with a close button. */
  variant?: 'inline' | 'input'
}>()

const emit = defineEmits<{
  (e: 'cancel'): void
}>()

const HIGHLIGHT_CLASS = 'ring-2 ring-amber-400'

function scrollToOriginal() {
  if (props.variant === 'input') return
  if (!props.reply?.id) return
  const el = document.getElementById(`msg-${props.reply.id}`)
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  el.classList.add(...HIGHLIGHT_CLASS.split(' '))
  window.setTimeout(() => el.classList.remove(...HIGHLIGHT_CLASS.split(' ')), 1500)
}
</script>

<template>
  <div
    v-if="variant === 'input'"
    class="flex items-start gap-2 px-3 py-2 border-l-2 border-indigo-400 bg-slate-900/60 rounded-md mb-2"
  >
    <div class="flex-1 min-w-0">
      <div class="text-[10px] font-semibold text-indigo-300 uppercase tracking-wider mb-0.5">
        Replying to @{{ reply.senderUsername }}
      </div>
      <div class="text-xs text-slate-400 truncate">
        {{ reply.deleted ? 'message deleted' : reply.body || '(empty)' }}
      </div>
    </div>
    <button
      v-if="dismissable"
      class="text-slate-500 hover:text-slate-200 text-xs px-1"
      aria-label="Cancel reply"
      @click="emit('cancel')"
    >
      ✕
    </button>
  </div>

  <button
    v-else
    type="button"
    class="block w-full text-left px-2 py-1 mb-1 border-l-2 border-slate-500 bg-slate-900/40 rounded text-[11px] leading-tight hover:bg-slate-900/70 hover:border-indigo-400 transition cursor-pointer"
    aria-label="Jump to original message"
    @click.stop="scrollToOriginal"
  >
    <div class="font-semibold text-slate-300 truncate">@{{ reply.senderUsername }}</div>
    <div class="text-slate-400 truncate">
      {{ reply.deleted ? 'message deleted' : reply.body || '(empty)' }}
    </div>
  </button>
</template>
