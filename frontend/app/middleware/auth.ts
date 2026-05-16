import { useAuthStore } from '~/stores/auth'

export default defineNuxtRouteMiddleware((to) => {
  const auth = useAuthStore()
  if (!auth.isAuthenticated && to.path !== '/login' && to.path !== '/register') {
    return navigateTo('/login')
  }
})
