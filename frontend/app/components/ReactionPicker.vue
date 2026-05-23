<script setup lang="ts">
const emit = defineEmits<{
  (e: 'pick', emoji: string): void
  (e: 'close'): void
}>()

const QUICK = ['👍', '❤️', '😂', '🎉', '😮', '😢', '🙏', '🔥']

// Hand-picked grid of popular emojis. Kept in-house so we don't pull a
// 200KB picker library for v1.
const GRID = [
  '😀','😃','😄','😁','😆','😅','🤣','😂','🙂','🙃','😉','😊','😇','🥰','😍','🤩',
  '😘','😗','😚','😙','🥲','😋','😛','😜','🤪','😝','🤑','🤗','🤭','🤫','🤔','🤐',
  '🤨','😐','😑','😶','😏','😒','🙄','😬','😮','😯','😲','😳','🥺','😦','😧','😨',
  '😰','😥','😢','😭','😱','😖','😣','😞','😓','😩','😫','🥱','😤','😡','😠','🤬',
  '😈','👿','💀','💩','🤡','👹','👻','💯','🔥','✨','⭐','🌟','💥','💫','💢','💦',
  '👍','👎','👌','✌️','🤞','🤟','🤘','👏','🙌','🙏','💪','🫶','❤️','🧡','💛','💚',
]

function pick(emoji: string) {
  emit('pick', emoji)
  emit('close')
}
</script>

<template>
  <div
    class="w-72 rounded-xl border border-slate-700 bg-slate-900 shadow-2xl p-2 text-sm"
    @click.stop
  >
    <div class="grid grid-cols-8 gap-1">
      <button
        v-for="emoji in QUICK"
        :key="`q-${emoji}`"
        type="button"
        class="h-8 w-8 rounded hover:bg-slate-800 text-base leading-none flex items-center justify-center"
        @click="pick(emoji)"
      >
        <span class="text-lg">{{ emoji }}</span>
      </button>
    </div>
    <div class="my-1.5 border-t border-slate-800"></div>
    <div class="grid grid-cols-8 gap-1 max-h-48 overflow-y-auto pr-1">
      <button
        v-for="emoji in GRID"
        :key="`g-${emoji}`"
        type="button"
        class="h-8 w-8 rounded hover:bg-slate-800 text-base leading-none flex items-center justify-center"
        @click="pick(emoji)"
      >
        <span class="text-lg">{{ emoji }}</span>
      </button>
    </div>
  </div>
</template>
