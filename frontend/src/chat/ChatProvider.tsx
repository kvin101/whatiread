import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  useSyncExternalStore,
  type ReactNode,
} from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { apiUrl, getAccessToken, onTokensRefreshed } from '../api/client'
import { AUTH_HEADERS, WS_PATH, WS_STOMP } from '../api/paths'
import type { ChatTypingEvent, Conversation, Message, MessageMention } from '../api/types'
import { conversationsApi } from '../api/conversations'
import { QUERY_KEYS } from '../lib/constants'
import { appendMessagePage, type MessageInfiniteData } from '../lib/messagingCache'
import { sortConversations } from '../lib/messaging'

type MessageListener = (message: Message) => void
type TypingListener = (event: ChatTypingEvent) => void

type ChatContextValue = {
  connected: boolean
  sendMessage: (conversationId: string, body: string, mentions?: MessageMention[]) => void
  sendTyping: (conversationId: string, typing: boolean) => void
  subscribeMessages: (listener: MessageListener) => () => void
  subscribeTyping: (listener: TypingListener) => () => void
  setActiveConversation: (conversationId: string | null) => void
}

const ChatContext = createContext<ChatContextValue | null>(null)
const chatConnectionSubscribers = new Set<() => void>()
let chatConnected = false

function setChatConnected(next: boolean) {
  if (chatConnected === next) return
  chatConnected = next
  chatConnectionSubscribers.forEach((listener) => listener())
}

function subscribeChatConnection(listener: () => void) {
  chatConnectionSubscribers.add(listener)
  return () => chatConnectionSubscribers.delete(listener)
}

function normalizeMessage(raw: Message): Message {
  return {
    ...raw,
    id: String(raw.id),
    conversationId: String(raw.conversationId),
    senderId: String(raw.senderId),
    mentions: raw.mentions ?? [],
  }
}

export function ChatProvider({ children, enabled }: { children: ReactNode; enabled: boolean }) {
  const queryClient = useQueryClient()
  const clientRef = useRef<Client | null>(null)
  const messageListeners = useRef(new Set<MessageListener>())
  const typingListeners = useRef(new Set<TypingListener>())
  const activeConversationIdRef = useRef<string | null>(null)
  const [connected, setConnected] = useState(false)

  const markConversationRead = useCallback(
    (conversationId: string) => {
      let cleared = 0
      queryClient.setQueryData<Conversation[]>(QUERY_KEYS.conversations.all, (old) => {
        if (!old?.length) return old
        return old.map((c) => {
          if (c.id === conversationId) {
            cleared = c.unreadCount
            return { ...c, unreadCount: 0 }
          }
          return c
        })
      })
      if (cleared > 0) {
        queryClient.setQueryData<number>(QUERY_KEYS.conversations.unreadCount, (old) =>
          Math.max(0, (old ?? 0) - cleared),
        )
      } else {
        queryClient.invalidateQueries({ queryKey: QUERY_KEYS.conversations.unreadCount })
      }
      conversationsApi.markRead(conversationId).catch(() => {})
    },
    [queryClient],
  )

  const setActiveConversation = useCallback(
    (conversationId: string | null) => {
      activeConversationIdRef.current = conversationId
      if (conversationId) {
        markConversationRead(conversationId)
      }
    },
    [markConversationRead],
  )

  const notifyMessage = useCallback(
    (raw: Message) => {
      const msg = normalizeMessage(raw)
      const isActive = activeConversationIdRef.current === msg.conversationId

      queryClient.setQueryData<MessageInfiniteData>(QUERY_KEYS.messages(msg.conversationId), (old) =>
        appendMessagePage(old, msg),
      )

      queryClient.setQueryData<Conversation[]>(QUERY_KEYS.conversations.all, (old) => {
        if (!old?.length) return old
        const next = old.map((c) =>
          c.id === msg.conversationId
            ? { ...c, lastMessage: msg, unreadCount: isActive ? 0 : c.unreadCount + 1 }
            : c,
        )
        return sortConversations(next)
      })

      if (isActive) {
        markConversationRead(msg.conversationId)
      } else {
        queryClient.setQueryData<number>(QUERY_KEYS.conversations.unreadCount, (old) => (old ?? 0) + 1)
      }

      messageListeners.current.forEach((listener) => listener(msg))
    },
    [queryClient, markConversationRead],
  )

  useEffect(() => {
    if (!enabled) {
      clientRef.current?.deactivate()
      clientRef.current = null
      setConnected(false)
      setChatConnected(false)
      return
    }

    if (!getAccessToken()) return

    const client = new Client({
      webSocketFactory: () => new SockJS(apiUrl(WS_PATH)),
      connectHeaders: {},
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      beforeConnect: () => {
        const latest = getAccessToken()
        if (!latest) {
          throw new Error('No access token')
        }
        client.connectHeaders = { Authorization: AUTH_HEADERS.bearer(latest) }
      },
      onConnect: () => {
        setConnected(true)
        setChatConnected(true)
        queryClient.invalidateQueries({ queryKey: QUERY_KEYS.conversations.all })
        queryClient.invalidateQueries({ queryKey: QUERY_KEYS.conversations.unreadCount })
        client.subscribe(WS_STOMP.queueMessages, (frame) => {
          const msg = JSON.parse(frame.body) as Message
          notifyMessage(msg)
        })
        client.subscribe(WS_STOMP.queueTyping, (frame) => {
          const event = JSON.parse(frame.body) as ChatTypingEvent
          typingListeners.current.forEach((listener) => listener(event))
        })
        client.subscribe(WS_STOMP.queueRecommendations, () => {
          queryClient.invalidateQueries({ queryKey: QUERY_KEYS.recommendations.all })
          if (typeof Notification !== 'undefined') {
            if (Notification.permission === 'granted') {
              new Notification('New book recommendation', {
                body: 'A friend thinks you should read something.',
              })
            } else if (Notification.permission === 'default') {
              Notification.requestPermission()
            }
          }
        })
        client.subscribe(WS_STOMP.queueFriends, (frame) => {
          const request = JSON.parse(frame.body) as { requester?: { displayName?: string; firstName?: string } }
          queryClient.invalidateQueries({ queryKey: QUERY_KEYS.friends.incoming })
          queryClient.invalidateQueries({ queryKey: QUERY_KEYS.friends.all })
          if (typeof Notification !== 'undefined') {
            const from =
              request.requester?.displayName ??
              request.requester?.firstName ??
              'Someone'
            if (Notification.permission === 'granted') {
              new Notification('New friend request', { body: `${from} wants to connect.` })
            } else if (Notification.permission === 'default') {
              Notification.requestPermission()
            }
          }
        })
      },
      onDisconnect: () => {
        setConnected(false)
        setChatConnected(false)
      },
      onStompError: () => {
        setConnected(false)
        setChatConnected(false)
      },
    })

    client.activate()
    clientRef.current = client

    const unsubscribeRefresh = onTokensRefreshed(() => {
      if (!clientRef.current?.active) {
        clientRef.current?.activate()
        return
      }
      clientRef.current.deactivate()
      clientRef.current.activate()
    })

    return () => {
      unsubscribeRefresh()
      client.deactivate()
      clientRef.current = null
      setConnected(false)
      setChatConnected(false)
    }
  }, [enabled, notifyMessage])

  const sendTyping = useCallback((conversationId: string, typing: boolean) => {
    const client = clientRef.current
    if (!client?.connected) return
    client.publish({
      destination: WS_STOMP.sendTyping,
      body: JSON.stringify({ conversationId, typing }),
    })
  }, [])

  const sendMessage = useCallback(
    (conversationId: string, body: string, mentions: MessageMention[] = []) => {
      const client = clientRef.current
      if (!client?.connected) {
        throw new Error('Chat not connected')
      }
      client.publish({
        destination: WS_STOMP.sendMessage,
        body: JSON.stringify({
          conversationId,
          body,
          mentions: mentions.length ? mentions : undefined,
        }),
      })
    },
    [],
  )

  const subscribeMessages = useCallback((listener: MessageListener) => {
    messageListeners.current.add(listener)
    return () => messageListeners.current.delete(listener)
  }, [])

  const subscribeTyping = useCallback((listener: TypingListener) => {
    typingListeners.current.add(listener)
    return () => typingListeners.current.delete(listener)
  }, [])

  const value = useMemo(
    () => ({
      connected,
      sendMessage,
      sendTyping,
      subscribeMessages,
      subscribeTyping,
      setActiveConversation,
    }),
    [connected, sendMessage, sendTyping, subscribeMessages, subscribeTyping, setActiveConversation],
  )

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>
}

export function useChatContext() {
  const ctx = useContext(ChatContext)
  if (!ctx) {
    throw new Error('useChatContext must be used within ChatProvider')
  }
  return ctx
}

export function useChatConnectionState() {
  return useSyncExternalStore(subscribeChatConnection, () => chatConnected, () => false)
}
