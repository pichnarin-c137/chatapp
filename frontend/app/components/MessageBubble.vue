<script setup lang="ts">
import { formatTime } from '~/utils/time'
import type { MentionRange, Message } from '~/types/conversation'
import { useChat } from '~/composables/useChat'
import { useConversation } from '~/composables/useConversation'
import { useAuthStore } from '~/stores/auth'
import { useConversationStore } from '~/stores/conversation'
import MessageReactions from './MessageReactions.vue'

const props = defineProps<{
  conversationId: string
  message: Message
  isOwn: boolean
  showSender: boolean
  timezone: string
}>()

const emit = defineEmits<{
  (e: 'forward-request', msg: Message): void
}>()

const { editMessage, deleteForMe, deleteForEveryone, startReply, pin, unpin } = useChat()
const { getOrCreateDirect } = useConversation()
const store = useConversationStore()
const auth = useAuthStore()

const time = computed(() => formatTime(props.message.sentAt, props.timezone))
const isDeleted = computed(() => !!props.message.deletedAt)
const isEdited = computed(() => !!props.message.editedAt && !isDeleted.value)
const isSending = computed(() => props.message.state === 'sending')
const isPinned = computed(() =>
  store.pinsFor(props.conversationId).some((p) => p.messageId === props.message.id),
)
const reply = computed(() => props.message.replyTo ?? null)
const fwd = computed(() => props.message.forwardOf ?? null)
const mentions = computed<MentionRange[]>(() => props.message.mentions ?? [])
const mentionedSelf = computed(() => {
  const me = auth.user?.id
  if (!me) return false
  return mentions.value.some((m) => m.userId === me)
})

/**
 * Split the body into plain-text and mention segments so we can render
 * @-mentions with their own styling. Returns the segments in render order;
 * a one-pass scan over the (sorted) mention ranges is enough.
 */
type BodySegment = { kind: 'text'; text: string } | { kind: 'mention'; text: string; userId: string }

const bodySegments = computed<BodySegment[]>(() => {
  const body = props.message.body ?? ''
  const ranges = [...mentions.value].sort((a, b) => a.startIndex - b.startIndex)
  if (ranges.length === 0) return [{ kind: 'text', text: body }]
  const out: BodySegment[] = []
  let cursor = 0
  for (const r of ranges) {
    const start = Math.max(0, Math.min(r.startIndex, body.length))
    const end = Math.max(start, Math.min(r.endIndex, body.length))
    if (start > cursor) out.push({ kind: 'text', text: body.slice(cursor, start) })
    out.push({ kind: 'mention', text: body.slice(start, end), userId: r.userId })
    cursor = end
  }
  if (cursor < body.length) out.push({ kind: 'text', text: body.slice(cursor) })
  return out
})

const showSelfMentionPulse = ref(false)
onMounted(() => {
  if (mentionedSelf.value) {
    showSelfMentionPulse.value = true
    window.setTimeout(() => { showSelfMentionPulse.value = false }, 1500)
  }
})

const menuOpen = ref(false)
const menuX = ref(0)
const menuY = ref(0)
const longPressTimer = ref<number | null>(null)
const editing = ref(false)
const editText = ref('')
const editRef = ref<HTMLTextAreaElement | null>(null)
const reactionsRef = ref<InstanceType<typeof MessageReactions> | null>(null)

const onWindowClick = () => closeMenu()

function openMenu() {
  if (isDeleted.value) return
  menuOpen.value = true
}

function closeMenu() {
  menuOpen.value = false
}

function onContextMenu(e: MouseEvent) {
  e.preventDefault()
  // position the menu at the mouse cursor (fixed) so it doesn't force scrolling
  // and appears near the clicked message like Telegram
  menuX.value = e.clientX
  menuY.value = e.clientY
  openMenu()
}

function startLongPress() {
  clearLongPress()
  longPressTimer.value = window.setTimeout(() => {
    openMenu()
  }, 450)
}

function clearLongPress() {
  if (longPressTimer.value !== null) {
    window.clearTimeout(longPressTimer.value)
    longPressTimer.value = null
  }
}

function startEdit() {
  closeMenu()
  if (isSending.value || isDeleted.value) return
  editText.value = props.message.body ?? ''
  editing.value = true
  nextTick(() => editRef.value?.focus())
}

function cancelEdit() {
  editing.value = false
  editText.value = ''
}

function saveEdit() {
  const next = editText.value.trim()
  if (!next || next === (props.message.body ?? '')) {
    cancelEdit()
    return
  }
  editMessage(props.conversationId, props.message.id, next)
  cancelEdit()
}

function onEditKey(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    e.preventDefault()
    cancelEdit()
  } else if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    saveEdit()
  }
}

function onReply() {
  closeMenu()
  if (isSending.value || isDeleted.value) return
  startReply(props.conversationId, props.message)
}

function onReact() {
  closeMenu()
  if (isSending.value || isDeleted.value) return
  reactionsRef.value?.openPicker()
}

async function onPin() {
  closeMenu()
  try {
    if (isPinned.value) await unpin(props.conversationId, props.message.id)
    else await pin(props.message.id)
  } catch (e) {
    console.error('pin/unpin failed', e)
  }
}

function onForward() {
  closeMenu()
  if (isSending.value || isDeleted.value) return
  emit('forward-request', props.message)
}

function onDeleteForMe() {
  closeMenu()
  deleteForMe(props.conversationId, props.message.id)
}

function onDeleteForEveryone() {
  closeMenu()
  deleteForEveryone(props.conversationId, props.message.id)
}

async function openForwardSenderDm() {
  if (!fwd.value) return
  const otherId = fwd.value.originalSenderId
  if (!otherId || otherId === auth.user?.id) return
  try {
    const conv = await getOrCreateDirect(otherId)
    await navigateTo(`/app/dm/${conv.id}`)
  } catch (e) {
    console.error('openForwardSenderDm failed', e)
  }
}

onMounted(() => {
  window.addEventListener('click', onWindowClick)
})

onBeforeUnmount(() => {
  window.removeEventListener('click', onWindowClick)
})
</script>

<template>
  <div :class="['flex w-full group', isOwn ? 'justify-end' : 'justify-start']">
    <div :class="['max-w-[78%] flex flex-col gap-0.5', isOwn ? 'items-end' : 'items-start']">
      <span v-if="showSender && !isOwn" class="text-xs text-slate-400 px-1">
        {{ message.senderUsername }}
      </span>
      <div class="relative">
        <!-- Editing state: textarea swap-in -->
        <div v-if="editing" class="px-3.5 py-2 rounded-2xl text-sm bg-slate-900 border border-indigo-500/60">
          <textarea ref="editRef" v-model="editText" rows="2"
            class="w-full bg-transparent resize-none outline-none text-sm placeholder:text-slate-500"
            placeholder="Edit message" @keydown="onEditKey" />
          <div class="flex justify-end gap-2 mt-1.5">
            <button class="text-[11px] text-slate-400 hover:text-slate-200 px-2 py-1 rounded" @click="cancelEdit">
              Cancel
            </button>
            <button class="text-[11px] font-medium bg-indigo-500 hover:bg-indigo-400 text-white px-2 py-1 rounded"
              @click="saveEdit">
              Save
            </button>
          </div>
        </div>

        <!-- Deleted tombstone -->
        <div v-else-if="isDeleted" :class="[
          'relative px-3.5 py-2 pb-5 pr-12 rounded-2xl text-sm italic text-slate-500 border border-dashed border-slate-700 bg-slate-900/40',
          isOwn ? 'rounded-br-md' : 'rounded-bl-md',
        ]">
          This message was deleted
          <span class="absolute bottom-1 right-2 text-[10px] text-slate-500 font-mono flex items-center gap-1.5">
            {{ time }}
            <span v-if="isEdited" class="text-slate-600">· edited</span>
            <span v-if="isSending" class="text-slate-600">· sending…</span>
            <span v-if="isPinned" class="text-amber-400" aria-label="Pinned">📌</span>
          </span>
        </div>

        <!-- Normal bubble -->
        <div v-else :class="[
          'relative px-3.5 py-2 pb-5 pr-12 rounded-2xl text-sm whitespace-pre-wrap break-words transition-shadow',
          isOwn
            ? 'bg-indigo-500 text-white rounded-br-md'
            : 'bg-slate-800 text-slate-100 rounded-bl-md',
          isSending ? 'opacity-60' : '',
          showSelfMentionPulse ? 'ring-2 ring-amber-400' : '',
        ]" @contextmenu="onContextMenu" @pointerdown="startLongPress" @pointerup="clearLongPress"
          @pointerleave="clearLongPress" @pointercancel="clearLongPress">
          <div v-if="fwd" class="text-[10px] uppercase tracking-wider mb-1 opacity-75">
            ↪ Forwarded from
            <button type="button"
              class="underline decoration-dotted underline-offset-2 hover:opacity-100 disabled:no-underline disabled:cursor-default"
              :disabled="fwd.originalSenderId === auth.user?.id"
              :title="fwd.originalSenderId === auth.user?.id ? 'You' : 'Open DM with @' + fwd.originalSenderUsername"
              @click.stop="openForwardSenderDm">@{{ fwd.originalSenderUsername }}</button>
          </div>
          <ReplyPreview v-if="reply" :reply="reply" variant="inline" />
          <template v-for="(seg, i) in bodySegments" :key="i">
            <span v-if="seg.kind === 'mention'" :class="[
              'font-semibold',
              isOwn ? 'text-amber-200' : 'text-indigo-300',
              seg.userId === auth.user?.id ? 'underline decoration-dotted underline-offset-2' : '',
            ]">{{ seg.text }}</span>
            <template v-else>{{ seg.text }}</template>
          </template>
          <span
            :class="['absolute bottom-1 right-2 text-[10px] font-mono flex items-center gap-1.5', isOwn ? 'text-indigo-100/90' : 'text-slate-400']">
            {{ time }}
            <span v-if="isEdited" class="text-slate-600">· edited</span>
            <span v-if="isSending" class="text-slate-600">· sending…</span>
            <span v-if="isPinned" class="text-amber-400" aria-label="Pinned">📌</span>
          </span>
        </div>
        <MessageReactions v-if="!isDeleted && !isSending" ref="reactionsRef" :message-id="message.id" />


        <!-- Context menu -->
        <div v-if="menuOpen && !editing && !isDeleted"
          class="fixed z-50 min-w-44 rounded-lg border border-slate-800 bg-slate-900 shadow-xl text-xs overflow-hidden"
          :style="{ left: menuX + 'px', top: menuY + 'px' }" @click.stop>
          <button class="w-full text-left px-3 py-2 hover:bg-slate-800" @click="onReact">
            React
          </button>
          <button class="w-full text-left px-3 py-2 hover:bg-slate-800" @click="onReply">
            Reply
          </button>
          <button v-if="isOwn" class="w-full text-left px-3 py-2 hover:bg-slate-800" @click="startEdit">
            Edit
          </button>
          <button class="w-full text-left px-3 py-2 hover:bg-slate-800" @click="onPin">
            {{ isPinned ? 'Unpin' : 'Pin' }}
          </button>
          <button class="w-full text-left px-3 py-2 hover:bg-slate-800" @click="onForward">
            Forward
          </button>
          <div class="border-t border-slate-800" />
          <button class="w-full text-left px-3 py-2 hover:bg-slate-800 text-slate-300" @click="onDeleteForMe">
            Delete for me
          </button>
          <button v-if="isOwn" class="w-full text-left px-3 py-2 hover:bg-slate-800 text-rose-300"
            @click="onDeleteForEveryone">
            Delete for everyone
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
