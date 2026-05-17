<script setup lang="ts">
import { formatTime } from '~/utils/time'

interface BubbleMessage {
  body: string | null
  sentAt: string
  senderUsername: string
}

const props = defineProps<{
  message: BubbleMessage
  isOwn: boolean
  showSender: boolean
  timezone: string
}>()

const time = computed(() => formatTime(props.message.sentAt, props.timezone))
const menuOpen = ref(false)
const longPressTimer = ref<number | null>(null)
const onWindowClick = () => closeMenu()

function openMenu() {
  menuOpen.value = true
}

function closeMenu() {
  menuOpen.value = false
}

function onContextMenu(e: MouseEvent) {
  e.preventDefault()
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

function onActionClick() {
  closeMenu()
}

onMounted(() => {
  window.addEventListener('click', onWindowClick)
})

onBeforeUnmount(() => {
  window.removeEventListener('click', onWindowClick)
})
</script>

<template>
  <div :class="['flex w-full', isOwn ? 'justify-end' : 'justify-start']">
    <div :class="['max-w-[78%] flex flex-col gap-0.5', isOwn ? 'items-end' : 'items-start']">
      <span v-if="showSender && !isOwn" class="text-xs text-slate-400 px-1">
        {{ message.senderUsername }}
      </span>
      <div class="relative">
        <div :class="[
          'px-3.5 py-2 rounded-2xl text-sm whitespace-pre-wrap break-words',
          isOwn
            ? 'bg-indigo-500 text-white rounded-br-md'
            : 'bg-slate-800 text-slate-100 rounded-bl-md',
        ]" @contextmenu="onContextMenu" @pointerdown="startLongPress" @pointerup="clearLongPress"
          @pointerleave="clearLongPress" @pointercancel="clearLongPress">
          {{ message.body }}
        </div>

        <div v-if="menuOpen" :class="[
          'absolute z-20 mt-2 min-w-40 rounded-lg border border-slate-800 bg-slate-900 shadow-xl text-xs overflow-hidden',
          isOwn ? 'right-0' : 'left-0',
        ]" @click.stop>
          <button class="w-full text-left px-3 py-2 hover:bg-slate-800" @click="onActionClick">
            Reply
          </button>
          <button class="w-full text-left px-3 py-2 hover:bg-slate-800" @click="onActionClick">
            Pin
          </button>
          <button v-if="isOwn" class="w-full text-left px-3 py-2 hover:bg-slate-800 text-rose-300"
            @click="onActionClick">
            Delete
          </button>
          <button class="w-full text-left px-3 py-2 hover:bg-slate-800" @click="onActionClick">
            Forward
          </button>
        </div>
      </div>
      <span class="text-[10px] text-slate-500 px-1 font-mono">{{ time }}</span>
    </div>
  </div>
</template>
