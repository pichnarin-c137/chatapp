<script setup lang="ts">
import { formatTime } from '~/utils/time'

interface BubbleMessage {
  content: string
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
</script>

<template>
  <div :class="['flex w-full', isOwn ? 'justify-end' : 'justify-start']">
    <div :class="['max-w-[78%] flex flex-col gap-0.5', isOwn ? 'items-end' : 'items-start']">
      <span
        v-if="showSender && !isOwn"
        class="text-xs text-slate-400 px-1"
      >
        {{ message.senderUsername }}
      </span>
      <div
        :class="[
          'px-3.5 py-2 rounded-2xl text-sm whitespace-pre-wrap break-words',
          isOwn
            ? 'bg-indigo-500 text-white rounded-br-md'
            : 'bg-slate-800 text-slate-100 rounded-bl-md',
        ]"
      >
        {{ message.content }}
      </div>
      <span class="text-[10px] text-slate-500 px-1 font-mono">{{ time }}</span>
    </div>
  </div>
</template>
