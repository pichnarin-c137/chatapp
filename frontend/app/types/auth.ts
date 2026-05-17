export type UserStatus = 'ACTIVE' | 'DISABLED' | 'BANNED'

export interface User {
  id: string
  username: string
  email: string
  status: UserStatus
  avatarUrl: string | null
  timezone: string | null
  createdAt: string
}

export interface AuthResponse {
  token: string
  user: User
}
