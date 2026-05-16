export interface User {
  id: string
  username: string
  email: string
  avatarUrl: string | null
  timezone: string
  createdAt: string
}

export interface AuthResponse {
  token: string
  user: User
}
