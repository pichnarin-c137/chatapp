# Real-Time Chat App — Full Plan (Spring Boot + Nuxt 3)

---

## Stack Overview

| Layer | Technology |
|---|---|
| Frontend | Nuxt 3 (SPA mode) + Vue 3 + TypeScript |
| Styling | Tailwind CSS + Nuxt UI |
| State | Pinia |
| Real-time | @stomp/stompjs + @vueuse/core |
| Backend | Spring Boot 3 + Java 21 |
| Real-time protocol | WebSocket + STOMP |
| Message broker | In-memory → RabbitMQ (when scaling) |
| Auth | JWT |
| Database | PostgreSQL |
| Cache / presence | Redis |
| File storage | Local disk (MVP) → Cloudflare R2 |
| Hosting (frontend) | Vercel / Cloudflare Pages |
| Hosting (backend) | Railway / VPS |

---

## Spring Initializr Setup

**Project:** Maven | Java | Spring Boot 3.x
**Packaging:** Jar | Java 21

### Select on Initializr UI

| Category | Dependency |
|---|---|
| Web | Spring Web |
| Messaging | WebSocket |
| Security | Spring Security |
| Data | Spring Data JPA |
| Data | Spring Data Redis |
| Database | PostgreSQL Driver |
| I/O | Validation |
| Developer Tools | Lombok |
| Developer Tools | Spring Boot DevTools |

### Add manually to pom.xml

```xml
<!-- JWT -->
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.12.3</version>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-impl</artifactId>
  <version>0.12.3</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-jackson</artifactId>
  <version>0.12.3</version>
  <scope>runtime</scope>
</dependency>
```

---

## Nuxt 3 Setup

```bash
npx nuxi@latest init chat-frontend
cd chat-frontend
```

### nuxt.config.ts

```ts
export default defineNuxtConfig({
  ssr: false,                    // SPA mode — no SSR needed for chat
  devtools: { enabled: true },
  modules: [
    '@nuxt/ui',                  // components + Tailwind built-in
    '@vueuse/nuxt',              // useWebSocket, useClipboard, useShare
    '@pinia/nuxt',               // global state
    '@vite-pwa/nuxt',            // installable PWA for mobile
  ],
  pwa: {
    manifest: {
      name: 'ChatApp',
      short_name: 'Chat',
      theme_color: '#0f172a',
      icons: [
        { src: '/icon-192.png', sizes: '192x192', type: 'image/png' },
        { src: '/icon-512.png', sizes: '512x512', type: 'image/png' },
      ],
    },
    workbox: {
      navigateFallback: '/',
    },
  },
  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_API_BASE || 'http://localhost:8080',
      wsBase:  process.env.NUXT_WS_BASE  || 'ws://localhost:8080/ws',
    },
  },
})
```

### npm packages to install

```bash
npm install @stomp/stompjs        # STOMP WebSocket client
npm install qrcode.vue            # QR code generation
npm install html5-qrcode          # Camera QR scanning
npm install date-fns              # Timestamp formatting
npm install vue-virtual-scroller  # Virtualized message list (performance)
```

---

## Frontend Project Structure

```
chat-frontend/
├── pages/
│   ├── index.vue                  # Redirect to /login or /app
│   ├── login.vue                  # Login page
│   ├── register.vue               # Register page
│   ├── app.vue                    # Main layout (sidebar + chat window)
│   ├── rooms/
│   │   └── [roomId].vue           # Group chat room
│   ├── dm/
│   │   └── [userId].vue           # Direct message thread
│   ├── invite/
│   │   └── [token].vue            # QR invite landing page
│   └── profile.vue                # User profile / settings
├── components/
│   ├── chat/
│   │   ├── MessageList.vue        # Virtualized message display
│   │   ├── MessageItem.vue        # Single message bubble
│   │   ├── MessageInput.vue       # Input box + send button
│   │   ├── TypingIndicator.vue    # "Alice is typing..."
│   │   └── ReplyPreview.vue       # Quoted reply preview
│   ├── sidebar/
│   │   ├── Sidebar.vue            # Room list + DM list
│   │   ├── RoomItem.vue           # Room row with unread badge
│   │   └── UserSearch.vue         # Search users to DM
│   ├── invite/
│   │   ├── InviteModal.vue        # QR + link + settings
│   │   ├── QRDisplay.vue          # Renders QR code
│   │   ├── QRScanner.vue          # Camera scanner
│   │   └── ShareButton.vue        # Web Share API button
│   └── ui/
│       ├── Avatar.vue             # User avatar with online dot
│       ├── UnreadBadge.vue        # Unread count pill
│       └── Toast.vue              # Notification toast
├── composables/
│   ├── useChat.ts                 # STOMP connection, send/receive
│   ├── usePresence.ts             # Online/offline status
│   ├── useTyping.ts               # Typing indicator logic
│   ├── useInvite.ts               # Generate / validate invite token
│   └── useAuth.ts                 # JWT login, register, me()
├── stores/
│   ├── auth.ts                    # Current user, JWT token
│   ├── rooms.ts                   # Room list, active room
│   ├── messages.ts                # Messages per room
│   └── presence.ts                # Online user IDs
├── utils/
│   ├── api.ts                     # $fetch wrapper with JWT header
│   └── time.ts                    # formatRelative, formatTime (date-fns)
└── middleware/
    └── auth.ts                    # Redirect to /login if no token
```

---

## Screens Plan

### Auth
- `/login` — email + password + "Scan QR to join room" shortcut
- `/register` — username, email, password

### Main app
- `/app` — sidebar (rooms + DMs) + welcome panel if no room selected
- `/rooms/:roomId` — group chat view
- `/dm/:userId` — DM thread
- `/profile` — avatar upload, display name, status message

### Invite
- `/invite/:token` — room preview (name, member count) + Join button
- Invite modal inside any room (QR + link + settings)

---

## Layout Plan

```
┌──────────────────────────────────────────────────────────┐
│ Sidebar (260px fixed)    │ Chat window (flex-grow)        │
│──────────────────────────│────────────────────────────────│
│ [Avatar] Me        [+]  │ # Dev Team     [Members] [QR]  │
│ [Search...]              │────────────────────────────────│
│                          │                                │
│ ROOMS                    │  [Messages — virtual scroll]   │
│ ● Dev Team          3    │                                │
│   Project Alpha          │                                │
│                          │                                │
│ DIRECT MESSAGES          │────────────────────────────────│
│ ● Alice     (online)     │  Alice is typing...            │
│   Bob                    │────────────────────────────────│
│                          │  [Reply preview if replying]   │
│                          │  [📎] [Input box...] [Send →]  │
└──────────────────────────┴────────────────────────────────┘

Mobile (< 768px):
- Sidebar collapses to off-canvas drawer
- Bottom nav: Rooms | DMs | Profile
- Swipe right from edge to open sidebar
```

---

## QR Invite System

### Flow

```
Admin clicks [QR] button in room header
        │
        ▼
POST /api/rooms/{id}/invite
→ Server generates UUID token
→ Stores in Redis: { roomId, createdBy, expiresAt, maxUses, usedCount }
→ Returns invite URL: https://chat.yourdomain.com/invite/{token}
        │
        ▼
InviteModal opens:
  - QR code rendered from URL
  - Copy link button
  - Share button (Web Share API)
  - Expiry selector (1h / 24h / 7d / never)
  - Max uses selector (1 / 10 / unlimited)
  - Revoke button
        │
        ▼
Someone scans QR → browser opens /invite/{token}
        │
  ┌─────┴──────┐
Not logged in  Logged in
     │              │
 Login/Register   Auto-POST /api/invite/{token}/join
     │              │
     └──────┬────────┘
            ▼
     Redirect to /rooms/{roomId}
```

### Backend endpoints for invite

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/rooms/{id}/invite` | Generate token, store in Redis |
| `GET` | `/api/invite/{token}` | Validate + return room preview (name, member count) |
| `POST` | `/api/invite/{token}/join` | Join room (auth required), increment usedCount |
| `DELETE` | `/api/rooms/{id}/invite/{token}` | Revoke token |

### Redis token structure

```
Key:   invite:{uuid-token}
TTL:   set per admin choice (3600 / 86400 / 604800 / no expiry)
Value: {
  roomId:    "uuid",
  createdBy: "userId",
  expiresAt: "ISO timestamp or null",
  maxUses:   10,
  usedCount: 0
}
```

### Invite modal layout

```
┌─────────────────────────────────────┐
│  Invite to "Dev Team"               │
│─────────────────────────────────────│
│         ┌─────────────┐             │
│         │  QR CODE    │             │
│         │  200 x 200  │             │
│         └─────────────┘             │
│                                     │
│  https://chat.app/invite/abc123     │
│  [Copy link]  [Share]  [Download]   │
│─────────────────────────────────────│
│  Expires: [24 hours ▼]              │
│  Max uses: [Unlimited ▼]            │
│                                     │
│  [Revoke]            [Regenerate]   │
└─────────────────────────────────────┘
```

---

## UX Features

### Messaging
- [ ] Send on Enter, newline on Shift+Enter
- [ ] Emoji picker
- [ ] Reply to specific message (quoted)
- [ ] Edit / delete own messages
- [ ] Image paste from clipboard (Ctrl+V)
- [ ] Drag and drop file upload
- [ ] Unread badge on room list
- [ ] Jump to unread button when scrolled up
- [ ] New messages separator line in history
- [ ] Message reactions (thumbs up, heart, etc.)

### Real-time feedback
- [ ] Typing indicator ("Alice is typing...")
- [ ] Online dot on avatars
- [ ] Message delivery ticks (sent ✓, read ✓✓)
- [ ] Toast for new DMs when in another room

### Onboarding
- [ ] First-time tutorial overlay (3 steps)
- [ ] Empty state with "Create your first room" CTA
- [ ] "Scan QR to join" on login page

### Mobile
- [ ] Off-canvas sidebar on mobile
- [ ] Swipe right to open sidebar
- [ ] Bottom navigation bar
- [ ] PWA installable (home screen icon, fullscreen)
- [ ] Camera QR scanner in-app

---

## WebSocket Topic Plan

| Channel | Direction | Used for |
|---|---|---|
| `/app/group.send` | client → server | Send group message |
| `/app/private.send` | client → server | Send DM |
| `/app/typing` | client → server | Typing event |
| `/topic/group.{roomId}` | server → all members | Broadcast messages, join/leave |
| `/user/{id}/queue/private` | server → one user | Deliver DM |
| `/topic/presence` | server → all | Online / offline status |
| `/topic/typing.{roomId}` | server → room | Typing indicator |

---

## REST API Plan

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/auth/register` | Sign up |
| `POST` | `/api/auth/login` | Login, receive JWT |
| `GET` | `/api/rooms` | List my rooms |
| `POST` | `/api/rooms` | Create a room |
| `POST` | `/api/rooms/{id}/members` | Add member |
| `DELETE` | `/api/rooms/{id}/members/{uid}` | Remove member |
| `GET` | `/api/rooms/{id}/messages?page=0` | Paginated history |
| `GET` | `/api/users/search?q=` | Search users to DM |
| `POST` | `/api/rooms/{id}/invite` | Generate invite token |
| `GET` | `/api/invite/{token}` | Validate + room preview |
| `POST` | `/api/invite/{token}/join` | Join via invite |
| `DELETE` | `/api/rooms/{id}/invite/{token}` | Revoke invite |
| `POST` | `/api/upload` | Upload file/image |

---

## Data Model

### `users`
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| username | VARCHAR | unique |
| email | VARCHAR | unique |
| password_hash | VARCHAR | bcrypt |
| avatar_url | VARCHAR | nullable |
| created_at | TIMESTAMP | |

### `chat_rooms`
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| name | VARCHAR | null for DMs |
| type | ENUM | PRIVATE, GROUP |
| created_by | UUID FK | → users |
| created_at | TIMESTAMP | |

### `room_members`
| Column | Type | Notes |
|---|---|---|
| room_id | UUID FK | → chat_rooms |
| user_id | UUID FK | → users |
| role | ENUM | ADMIN, MEMBER |
| joined_at | TIMESTAMP | |

### `messages`
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| room_id | UUID FK | → chat_rooms |
| sender_id | UUID FK | → users |
| content | TEXT | |
| type | ENUM | TEXT, IMAGE, FILE |
| reply_to | UUID FK | → messages (nullable) |
| sent_at | TIMESTAMP | |

### `message_status`
| Column | Type | Notes |
|---|---|---|
| message_id | UUID FK | → messages |
| user_id | UUID FK | → users |
| status | ENUM | SENT, DELIVERED, READ |
| updated_at | TIMESTAMP | |

---

## Hosting Plan

### MVP (start here — near zero cost)

| Service | Runs | Cost |
|---|---|---|
| Railway.app | Spring Boot + PostgreSQL + Redis | Free tier / ~$5/mo |
| Vercel | Nuxt 3 SPA | Free |
| Cloudflare | CDN + domain | Free tier |

### Production (when you have real users)

| Service | Runs | Cost |
|---|---|---|
| DigitalOcean Droplet / EC2 | Spring Boot (Docker) | ~$12–20/mo |
| AWS RDS / DO Managed DB | PostgreSQL | ~$15/mo |
| AWS ElastiCache / DO Redis | Redis | ~$15/mo |
| Vercel | Nuxt 3 SPA | Free |
| Cloudflare R2 | File / image uploads | Free up to 10GB |

### Self-host with Docker Compose

```
services:
  app         # Spring Boot jar
  postgres    # PostgreSQL 16
  redis       # Redis 7
  nginx       # Reverse proxy + SSL termination
```

### Domain setup

```
Frontend:   https://chat.yourdomain.com    → Vercel
Backend:    https://api.yourdomain.com     → Railway / VPS
WebSocket:  wss://api.yourdomain.com/ws   → same backend (WSS)
```

### Nginx WebSocket config (required)

```nginx
location /ws {
    proxy_pass http://localhost:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
}
```

---

## Backend Project Structure

```
src/main/java/com/yourapp/chat/
├── config/
│   ├── WebSocketConfig.java
│   ├── SecurityConfig.java
│   └── RedisConfig.java
├── controller/
│   ├── ChatController.java          # @MessageMapping handlers
│   ├── AuthController.java
│   ├── RoomController.java
│   ├── InviteController.java        # QR invite endpoints
│   ├── UploadController.java
│   └── UserController.java
├── model/
│   ├── User.java
│   ├── ChatRoom.java
│   ├── RoomMember.java
│   ├── Message.java
│   └── MessageStatus.java
├── repository/
│   ├── UserRepository.java
│   ├── ChatRoomRepository.java
│   ├── MessageRepository.java
│   └── MessageStatusRepository.java
├── service/
│   ├── AuthService.java
│   ├── ChatService.java
│   ├── RoomService.java
│   ├── InviteService.java           # Redis token logic
│   └── PresenceService.java
├── security/
│   ├── JwtService.java
│   ├── JwtFilter.java
│   └── JwtHandshakeInterceptor.java
├── dto/
│   ├── ChatMessage.java
│   ├── TypingEvent.java
│   ├── PresenceEvent.java
│   └── InviteToken.java
└── listener/
    └── WebSocketEventListener.java
```

---

## application.properties

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/chatdb
spring.datasource.username=postgres
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT
app.jwt.secret=your-256-bit-secret-here
app.jwt.expiration=86400000

# CORS (allow Nuxt frontend)
app.cors.allowed-origins=http://localhost:3000,https://chat.yourdomain.com

# File uploads
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
app.upload.dir=./uploads
```

---

## Build Order

| Phase | Tasks |
|---|---|
| 1. Backend core | Auth (JWT), WebSocket config, group + personal messaging, message persistence |
| 2. Nuxt skeleton | Login page, sidebar, basic chat window with STOMP connection, Pinia stores |
| 3. QR invite | Invite token endpoints + InviteModal + /invite/:token landing page |
| 4. Real-time UX | Typing indicator, presence, read receipts, unread badges |
| 5. PWA | Add @vite-pwa/nuxt, manifest, service worker |
| 6. Deploy MVP | Railway (backend) + Vercel (frontend) |
| 7. Polish | Reactions, reply-to, file upload, emoji picker |
| 8. Scale | RabbitMQ relay, Redis sessions, rate limiting |

---

## Key Design Notes

**Private room ID:** Generate a deterministic room ID for DMs by sorting both user IDs and hashing — prevents duplicate DM rooms between the same two users.

**JWT in WebSocket:** Pass token as a query param on WS connect (`/ws?token=...`). Intercept in `JwtHandshakeInterceptor` before the connection is accepted.

**Typing debounce:** Client debounces 300ms before sending typing event. Server auto-clears after 3 seconds with no new event (use Redis TTL key per user per room).

**Message pagination:** Fetch newest-first (`ORDER BY sent_at DESC`, page size 30). Nuxt reverses the array for display. Infinite scroll loads older messages as user scrolls up.

**Invite token security:** Always validate token server-side on join — never trust the client. Check: token exists, not expired, usedCount < maxUses, user not already a member.

**PWA + QR:** With the PWA installed, users can open the camera QR scanner directly inside the app without needing the browser. `html5-qrcode` handles this with the device camera API.

**Scaling trigger:** In-memory STOMP broker works for one server instance. Switch to RabbitMQ relay (`enableStompBrokerRelay()`) before adding a second backend instance.
