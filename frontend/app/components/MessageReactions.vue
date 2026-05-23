<script setup lang="ts">
import { useChat } from '~/composables/useChat'
import { useReactionStore } from '~/stores/reactions'
import ReactionPicker from './ReactionPicker.vue'

const props = defineProps<{
  messageId: string
  align?: 'start' | 'end'
}>()

const reactionStore = useReactionStore()
const { toggleReaction } = useChat()

const buckets = computed(() => reactionStore.reactionsFor(props.messageId))

const pickerOpen = ref(false)
const btnRef = ref<HTMLElement | null>(null)
const pickerRef = ref<HTMLElement | null>(null)
const pickerStyle = ref({ left: '0px', top: '0px' })

function onPick(emoji: string) {
  toggleReaction(props.messageId, emoji)
}

function onToggle(emoji: string) {
  toggleReaction(props.messageId, emoji)
}

function closeOnEsc(e: KeyboardEvent) {
  if (e.key === 'Escape') pickerOpen.value = false
}

function closeOnOutsideClick(e: MouseEvent) {
  const picker = pickerRef.value
  const btn = btnRef.value
  const target = e.target as Node | null
  if (!target) {
    pickerOpen.value = false
    return
  }
  if (picker && picker.contains(target)) return
  if (btn && btn.contains(target)) return
  pickerOpen.value = false
}

function computePickerPosition() {
  if (!import.meta.client) return
  const btn = btnRef.value
  const picker = pickerRef.value
  if (!btn || !picker) return
  const rect = btn.getBoundingClientRect()
  const pr = picker.getBoundingClientRect()
  const margin = 8

  // try to place above the button
  let top = rect.top - pr.height - margin
  // if not enough space above, place below
  if (top < margin) top = rect.bottom + margin

  // prefer aligning start (left) unless it would overflow
  let left = rect.left
  if (left + pr.width > window.innerWidth - margin) {
    left = window.innerWidth - pr.width - margin
  }
  if (left < margin) left = margin

  pickerStyle.value = { left: `${Math.round(left)}px`, top: `${Math.round(top)}px` }
}

watch(pickerOpen, async (v) => {
  if (!import.meta.client) return
  if (v) {
    window.addEventListener('keydown', closeOnEsc)
    await nextTick()
    computePickerPosition()
    // recompute on resize/scroll while open
    window.addEventListener('resize', computePickerPosition)
    window.addEventListener('scroll', computePickerPosition, true)
    window.addEventListener('click', closeOnOutsideClick)
  } else {
    window.removeEventListener('keydown', closeOnEsc)
    window.removeEventListener('resize', computePickerPosition)
    window.removeEventListener('scroll', computePickerPosition, true)
    window.removeEventListener('click', closeOnOutsideClick)
  }
})

onBeforeUnmount(() => {
  if (import.meta.client) {
    window.removeEventListener('keydown', closeOnEsc)
    window.removeEventListener('resize', computePickerPosition)
    window.removeEventListener('scroll', computePickerPosition, true)
    window.removeEventListener('click', closeOnOutsideClick)
  }
})

function openPickerProgrammatic() {
  pickerOpen.value = true
  // compute position after it renders
  nextTick().then(computePickerPosition).catch(() => { })
}

defineExpose({
  openPicker: openPickerProgrammatic,
})
</script>

<template>
  <div class="mt-1 flex flex-wrap items-center gap-1.5">
    <button v-for="b in buckets" :key="b.emoji" type="button"
      class="inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs transition" :class="b.mine
        ? 'border-indigo-400/70 bg-indigo-500/15 text-indigo-100'
        : 'border-slate-700 bg-slate-800/60 text-slate-200 hover:bg-slate-800'"
      :title="b.userIds.length === 1 ? '1 person reacted' : `${b.count} people reacted`"
      @click.stop="onToggle(b.emoji)">
      <span class="text-sm leading-none">{{ b.emoji }}</span>
      <span class="font-mono text-[10px] tabular-nums">{{ b.count }}</span>
    </button>

    <div class="relative">
      <button type="button" :class="[
        'inline-flex items-center justify-center rounded-full border px-2 py-0.5 text-xs transition',
        buckets.length === 0
          ? 'border-slate-800 bg-transparent text-slate-600 opacity-0 group-hover:opacity-100 focus:opacity-100 hover:bg-slate-800 hover:text-slate-200'
          : 'border-slate-700 bg-slate-800/40 text-slate-400 hover:bg-slate-800 hover:text-slate-100',
      ]" title="Add reaction" @click.stop="pickerOpen = !pickerOpen" ref="btnRef">
        +
      </button>
      <div v-if="pickerOpen" class="fixed z-50" :style="pickerStyle" ref="pickerRef" @click.stop>
        <ReactionPicker @pick="onPick" @close="pickerOpen = false" />
      </div>
    </div>
  </div>
</template>
