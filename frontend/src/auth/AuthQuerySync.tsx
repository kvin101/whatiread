import { useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useAuth } from './AuthContext'
import { QUERY_KEYS } from '../lib/constants'

/** WhatsApp-style sync: drop stale inbox state on login/logout. */
export function AuthQuerySync() {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  useEffect(() => {
    if (user) {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.conversations.all })
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.conversations.unreadCount })
      return
    }
    queryClient.removeQueries({ queryKey: QUERY_KEYS.conversations.all })
    queryClient.removeQueries({ queryKey: QUERY_KEYS.conversations.unreadCount })
    queryClient.removeQueries({ queryKey: ['messages'] })
  }, [queryClient, user?.id])

  return null
}
