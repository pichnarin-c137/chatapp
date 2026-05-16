<script setup lang="ts">
import type { User } from '~/types/auth'
import { LOBBY_ID } from '~/composables/useChat'
import { formatRelative } from '~/utils/time'

const route = useRoute()
const authStore = useAuthStore()
const chatStore = useChatStore()
const dmStore = useDmStore()
const { logout } = useAuth()
const { connect, disconnect } = useStomp()
const { loadConversations, subscribeConversation, startWith } = useDm()
const { subscribeRoom, loadHistory } = useChat()
const { search } = useUserSearch()

const query = ref('')
const results = ref<User[]>([])
const searching = ref(false)
const starting = ref(false)
const showUserMenu = ref(false)

const isSearching = computed(() => query.value.trim().replace(/^@+/, '').length > 0)
const conversations = computed(() => dmStore.conversations)
const tz = computed(() => authStore.timezone)

const inChat = computed(() => {
  const p = route.path
  return p.startsWith('/app/') && p !== '/app/' && p !== '/app'
})

const activeKey = computed(() => {
  if (route.path === '/app/lobby') return 'lobby'
  const match = route.path.match(/^\/app\/dm\/(.+)$/)
  return match ? `dm:${match[1]}` : null
})

const connectionLabel = computed(() => {
  switch (chatStore.connection) {
    case 'connected': return 'Online'
    case 'connecting': return 'Connecting…'
    case 'reconnecting': return 'Reconnecting…'
    case 'error': return chatStore.lastError || 'Connection error'
    default: return 'Offline'
  }
})

const connectionColor = computed(() => {
  switch (chatStore.connection) {
    case 'connected': return 'bg-emerald-400'
    case 'connecting':
    case 'reconnecting': return 'bg-amber-400 animate-pulse'
    case 'error': return 'bg-rose-500'
    default: return 'bg-slate-500'
  }
})

const runSearch = useDebounceFn(async (raw: string) => {
  const q = raw.trim().replace(/^@+/, '')
  if (!q) {
    results.value = []
    return
  }
  searching.value = true
  try {
    results.value = await search(q)
  } catch (e) {
    console.error('search failed', e)
    results.value = []
  } finally {
    searching.value = false
  }
}, 250)

watch(query, (v) => runSearch(v))

onMounted(async () => {
  connect()
  subscribeRoom(LOBBY_ID)
  await loadHistory(LOBBY_ID).catch(() => {})
  try {
    const list = await loadConversations()
    for (const c of list) subscribeConversation(c.id)
  } catch (e) {
    console.error('failed to load DMs', e)
  }
})

async function openSearchResult(user: User) {
  if (starting.value) return
  starting.value = true
  try {
    const conv = await startWith(user.id)
    subscribeConversation(conv.id)
    query.value = ''
    results.value = []
    await navigateTo(`/app/dm/${conv.id}`)
  } catch (e) {
    console.error('startWith failed', e)
  } finally {
    starting.value = false
  }
}

async function onLogout() {
  disconnect()
  chatStore.reset()
  dmStore.reset()
  logout()
  await navigateTo('/login')
}

function initials(name: string) {
  return name.slice(0, 2).toUpperCase()
}

function preview(conv: { lastMessage: { senderId: string; content: string } | null }) {
  if (!conv.lastMessage) return 'No messages yet'
  const isOwn = conv.lastMessage.senderId === authStore.user?.id
  return (isOwn ? 'You: ' : '') + conv.lastMessage.content
}

function lastTime(conv: { lastMessageAt: string | null; createdAt: string }) {
  const stamp = conv.lastMessageAt ?? conv.createdAt
  return formatRelative(stamp, tz.value)
}
</script>

<template>
  <div class="h-dvh flex bg-slate-950 text-slate-100">
    <aside
      :class="[
        'flex-col w-full md:w-80 shrink-0 md:border-r border-slate-800',
        inChat ? 'hidden md:flex' : 'flex',
      ]"
    >
      <div class="px-4 py-3 border-b border-slate-800 flex items-center justify-between relative">
        <div class="flex items-center gap-2 min-w-0">
          <div class="size-8 rounded-full bg-gradient-to-br from-sky-400 to-indigo-500 flex items-center justify-center shrink-0">
            <svg xmlns="http://www.w3.org/2000/svg" class="size-4 text-white" viewBox="0 0 24 24" fill="currentColor"><path d="M21.5 4.5 2.7 12.2c-.9.4-.9 1.6 0 2l3.8 1.5L18 8.5c.4-.2.7.3.4.6L9 18.4v3.1c0 .8.9 1.1 1.4.5l2.7-3 5.3 2.1c.7.3 1.4-.1 1.6-.8L22.5 5.6c.2-.7-.4-1.4-1-1.1z"/></svg>
          </div>
          <span class="text-sm font-semibold">Chat</span>
        </div>
        <button
          class="text-xs text-slate-400 hover:text-slate-100 transition px-2 py-1.5 rounded-md hover:bg-slate-800 flex items-center gap-1.5"
          @click="showUserMenu = !showUserMenu"
        >
          <span class="size-6 rounded-full bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center text-[10px] font-semibold text-indigo-200">
            {{ authStore.user ? initials(authStore.user.username) : '··' }}
          </span>
          <span class="hidden sm:inline">@{{ authStore.user?.username }}</span>
          <svg xmlns="http://www.w3.org/2000/svg" class="size-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 9 6 6 6-6"/></svg>
        </button>
        <div
          v-if="showUserMenu"
          class="absolute right-4 top-full mt-1 z-30 w-56 rounded-lg border border-slate-800 bg-slate-900 shadow-xl text-xs"
          @click.stop
        >
          <div class="px-3 py-2.5 border-b border-slate-800 text-slate-400">
            <div class="text-slate-200 font-medium">@{{ authStore.user?.username }}</div>
            <div class="truncate font-mono text-[10px]">{{ authStore.user?.email }}</div>
            <div class="mt-1 font-mono text-[10px] text-slate-500">{{ tz }}</div>
          </div>
          <button
            class="w-full text-left px-3 py-2 hover:bg-slate-800 text-rose-300 hover:text-rose-200"
            @click="onLogout"
          >
            Sign out
          </button>
        </div>
      </div>

      <div class="px-3 py-2 border-b border-slate-800">
        <div class="flex items-center gap-2 bg-slate-900/60 border border-slate-800 rounded-xl px-3 py-2 focus-within:border-indigo-500/60 transition">
          <svg xmlns="http://www.w3.org/2000/svg" class="size-4 text-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
          <input
            v-model="query"
            type="text"
            autocomplete="off"
            placeholder="Search @username"
            class="flex-1 bg-transparent outline-none text-sm placeholder:text-slate-500"
          />
          <button
            v-if="query"
            class="text-slate-500 hover:text-slate-200 transition text-xs"
            @click="query = ''"
          >
            ✕
          </button>
        </div>
        <div class="mt-1.5 flex items-center gap-1.5 text-[10px] text-slate-500 px-1">
          <span :class="['size-1.5 rounded-full', connectionColor]"></span>
          {{ connectionLabel }}
        </div>
      </div>

      <div class="flex-1 overflow-y-auto">
        <section v-if="isSearching">
          <div v-if="searching" class="px-4 py-6 text-xs text-slate-500">Searching…</div>
          <div v-else-if="results.length === 0" class="px-4 py-6 text-xs text-slate-500">
            No users matching "{{ query }}"
          </div>
          <ul v-else class="divide-y divide-slate-900">
            <li
              v-for="u in results"
              :key="u.id"
              class="px-3 py-2.5 hover:bg-slate-900/60 cursor-pointer transition flex items-center gap-3"
              :class="starting && 'opacity-50 pointer-events-none'"
              @click="openSearchResult(u)"
            >
              <div class="size-10 rounded-full bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center text-sm font-semibold text-indigo-200 shrink-0">
                {{ initials(u.username) }}
              </div>
              <div class="min-w-0 flex-1">
                <div class="text-sm font-medium text-slate-100 truncate">@{{ u.username }}</div>
                <div class="text-xs text-slate-500 truncate">{{ u.email }}</div>
              </div>
            </li>
          </ul>
        </section>

        <section v-else>
          <NuxtLink
            to="/app/lobby"
            class="px-3 py-2.5 flex items-center gap-3 transition border-l-2"
            :class="activeKey === 'lobby'
              ? 'bg-indigo-500/10 border-indigo-400'
              : 'border-transparent hover:bg-slate-900/60'"
          >
            <div class="size-10 rounded-full bg-sky-500/20 border border-sky-500/30 flex items-center justify-center text-sky-200 text-sm font-semibold shrink-0">#</div>
            <div class="min-w-0 flex-1">
              <div class="text-sm font-medium text-slate-100 truncate">Lobby</div>
              <div class="text-xs text-slate-500 truncate">Public · everyone</div>
            </div>
          </NuxtLink>

          <div class="px-3 pt-3 pb-1.5 text-[10px] uppercase tracking-wider text-slate-500 font-semibold">
            Direct messages
          </div>

          <div v-if="conversations.length === 0" class="px-4 py-3 text-xs text-slate-500">
            No DMs yet. Search above to start one.
          </div>
          <ul v-else>
            <li v-for="c in conversations" :key="c.id">
              <NuxtLink
                :to="`/app/dm/${c.id}`"
                class="px-3 py-2.5 flex items-center gap-3 transition border-l-2"
                :class="activeKey === `dm:${c.id}`
                  ? 'bg-indigo-500/10 border-indigo-400'
                  : 'border-transparent hover:bg-slate-900/60'"
              >
                <div class="size-10 rounded-full bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center text-sm font-semibold text-indigo-200 shrink-0">
                  {{ initials(c.peer.username) }}
                </div>
                <div class="min-w-0 flex-1">
                  <div class="flex items-baseline justify-between gap-2">
                    <span class="text-sm font-medium text-slate-100 truncate">@{{ c.peer.username }}</span>
                    <span class="text-[10px] text-slate-500 font-mono shrink-0">{{ lastTime(c) }}</span>
                  </div>
                  <div class="text-xs text-slate-400 truncate">{{ preview(c) }}</div>
                </div>
              </NuxtLink>
            </li>
          </ul>
        </section>
      </div>
    </aside>

    <section
      :class="[
        'flex-1 min-w-0 flex-col',
        inChat ? 'flex' : 'hidden md:flex',
      ]"
    >
      <slot />
    </section>
  </div>
</template>
