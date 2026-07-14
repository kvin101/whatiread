import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { conversationsApi } from '../api/conversations'
import { friendsApi } from '../api/friends'
import type { Conversation, Message, MessageMention } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { NewChatModal } from '../components/messages/NewChatModal'
import { useChat } from '../hooks/useChat'
import { PageHeader } from '../components/layout/PageHeader'
import { copy } from '../lib/copy'
import { QUERY_KEYS } from '../lib/constants'
import { getApiErrorMessage } from '../lib/api'
import {
  conversationTitle,
  flattenMessagePages,
  normalizeMessagingIds,
  participantMap,
  sortConversations,
} from '../lib/messaging'
import {
  prependOptimisticMessage,
  removeOptimisticMessage,
  type MessageInfiniteData,
} from '../lib/messagingCache'
import { ConversationList } from '../components/messages/ConversationList'
import { ChatThread } from '../components/messages/ChatThread'
import { GroupAdminModals } from '../components/messages/GroupAdminModals'
import { Button } from '../components/ui/Button'
import { displayName } from '../lib/utils'

const MESSAGE_PAGE_SIZE = 50

export function MessagesPage() {
  const { user } = useAuth()
  const location = useLocation()
  const queryClient = useQueryClient()
  const [newChatOpen, setNewChatOpen] = useState(false)
  const [renameOpen, setRenameOpen] = useState(false)
  const [leaveOpen, setLeaveOpen] = useState(false)
  const [renameDraft, setRenameDraft] = useState('')
  const [activeId, setActiveId] = useState<string | null>(() => {
    const fromState = (location.state as { conversationId?: string } | null)?.conversationId
    return fromState ? String(fromState) : null
  })
  const [peerTyping, setPeerTyping] = useState(false)
  const [typingUserId, setTypingUserId] = useState<string | null>(null)
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const activeIdRef = useRef(activeId)
  activeIdRef.current = activeId

  const { data: conversations = [], isLoading: conversationsLoading } = useQuery({
    queryKey: QUERY_KEYS.conversations.all,
    queryFn: async () => {
      const page = normalizeMessagingIds({ conversations: await conversationsApi.list() }).conversations ?? []
      return sortConversations(page)
    },
    staleTime: 30_000,
    refetchOnWindowFocus: true,
  })

  const { data: friends = [] } = useQuery({
    queryKey: QUERY_KEYS.friends.all,
    queryFn: friendsApi.list,
  })

  const active = useMemo(() => conversations.find((c) => c.id === activeId), [conversations, activeId])
  const participantsById = useMemo(() => participantMap(active), [active])

  const {
    data: messagePages,
    isPending: messagesLoading,
    isError: messagesError,
    error: messagesFetchError,
    refetch: refetchMessages,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: QUERY_KEYS.messages(activeId!),
    queryFn: async ({ pageParam }) => {
      const page = await conversationsApi.messages(activeId!, {
        cursor: pageParam as string | undefined,
        limit: MESSAGE_PAGE_SIZE,
      })
      return {
        ...page,
        items: normalizeMessagingIds({ messages: page.items }).messages ?? [],
      }
    },
    enabled: !!activeId,
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasMore ? lastPage.nextCursor ?? undefined : undefined),
    staleTime: 0,
  })

  const displayMessages = useMemo(
    () => flattenMessagePages(messagePages?.pages),
    [messagePages?.pages],
  )

  const syncConversationPreview = useCallback(
    (conversationId: string, lastMessage: Message) => {
      queryClient.setQueryData<Conversation[]>(QUERY_KEYS.conversations.all, (old) => {
        const next = old?.map((c) => (c.id === conversationId ? { ...c, lastMessage } : c)) ?? old
        return next ? sortConversations(next) : next
      })
    },
    [queryClient],
  )

  useEffect(() => {
    if (!activeId || !displayMessages.length) return
    const latest = displayMessages[displayMessages.length - 1]
    if (latest) {
      syncConversationPreview(activeId, latest)
    }
  }, [activeId, displayMessages, syncConversationPreview])

  const onIncoming = useCallback(
    (msg: Message) => {
      const convId = String(msg.conversationId)
      if (convId !== activeIdRef.current) {
        if (msg.senderId !== user?.id && typeof Notification !== 'undefined') {
          if (Notification.permission === 'granted') {
            const conv = conversations.find((c) => c.id === convId)
            const from = conv ? conversationTitle(conv) : 'Friend'
            new Notification(`New message from ${from}`, { body: msg.body })
          } else if (Notification.permission === 'default') {
            Notification.requestPermission()
          }
        }
      }
    },
    [conversations, user?.id],
  )

  const onTyping = useCallback(
    (event: import('../api/types').ChatTypingEvent) => {
      if (String(event.conversationId) !== activeIdRef.current || event.userId === user?.id) {
        return
      }
      setPeerTyping(event.typing)
      setTypingUserId(event.typing ? String(event.userId) : null)
      if (event.typing) {
        if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current)
        typingTimeoutRef.current = setTimeout(() => {
          setPeerTyping(false)
          setTypingUserId(null)
        }, 3000)
      }
    },
    [user?.id],
  )

  const { connected, sendMessage, sendTyping, setActiveConversation } = useChat(onIncoming, onTyping)

  const handleSendMessage = useCallback(
    (conversationId: string, body: string, mentions?: MessageMention[]) => {
      if (!user?.id) return
      const optimistic: Message = {
        id: `temp-${Date.now()}`,
        conversationId,
        senderId: String(user.id),
        body,
        mentions: mentions?.length ? mentions : undefined,
        sentAt: new Date().toISOString(),
      }
      queryClient.setQueryData<MessageInfiniteData>(QUERY_KEYS.messages(conversationId), (old) =>
        prependOptimisticMessage(old, optimistic),
      )
      syncConversationPreview(conversationId, optimistic)
      try {
        sendMessage(conversationId, body, mentions)
      } catch {
        queryClient.setQueryData<MessageInfiniteData>(QUERY_KEYS.messages(conversationId), (old) =>
          removeOptimisticMessage(old, optimistic.id),
        )
      }
    },
    [queryClient, sendMessage, syncConversationPreview, user?.id],
  )

  useEffect(() => {
    setActiveConversation(activeId)
    return () => setActiveConversation(null)
  }, [activeId, setActiveConversation])

  useEffect(() => {
    if (!activeId || messagesLoading || messagesError) return
    queryClient.setQueryData<Conversation[]>(QUERY_KEYS.conversations.all, (old) =>
      old?.map((c) => (c.id === activeId ? { ...c, unreadCount: 0 } : c)) ?? old,
    )
    queryClient.invalidateQueries({ queryKey: QUERY_KEYS.conversations.unreadCount })
  }, [activeId, messagesLoading, messagesError, queryClient])

  const startChatMutation = useMutation({
    mutationFn: (friendUserId: string) => conversationsApi.withFriend(friendUserId),
    onSuccess: (conv) => {
      const normalized = normalizeMessagingIds({ conversations: [conv] }).conversations?.[0]
      if (!normalized) return
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.conversations.all })
      setNewChatOpen(false)
      setActiveId(normalized.id)
    },
  })

  const createGroupMutation = useMutation({
    mutationFn: ({ name, memberUserIds }: { name: string; memberUserIds: string[] }) =>
      conversationsApi.createGroup(name, memberUserIds),
    onSuccess: (conv) => {
      const normalized = normalizeMessagingIds({ conversations: [conv] }).conversations?.[0]
      if (!normalized) return
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.conversations.all })
      setNewChatOpen(false)
      setActiveId(normalized.id)
    },
  })

  const renameGroupMutation = useMutation({
    mutationFn: ({ conversationId, name }: { conversationId: string; name: string }) =>
      conversationsApi.renameGroup(conversationId, name),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.conversations.all })
      setRenameOpen(false)
    },
  })

  const leaveGroupMutation = useMutation({
    mutationFn: (conversationId: string) => conversationsApi.leave(conversationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.conversations.all })
      setLeaveOpen(false)
      setActiveId(null)
    },
  })

  const errorDetail = getApiErrorMessage(messagesFetchError, 'Unknown error')
  const typingParticipant = typingUserId ? participantsById.get(typingUserId) : undefined
  const typingLabel = typingParticipant ? displayName(typingParticipant) : 'Someone'

  return (
    <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
      <div className="shrink-0">
      <PageHeader
        title={copy.messages.title}
        action={
          <span
            className={`inline-block h-2 w-2 rounded-full ${connected ? 'bg-sage' : 'bg-border'}`}
            title={connected ? 'Connected' : 'Connecting…'}
          />
        }
      />

      </div>

      <div className="flex min-h-0 flex-1 overflow-hidden rounded-3xl chat-panel">
        <aside
          className={`w-full max-w-xs shrink-0 border-r border-white/10 flex flex-col min-h-0 ${
            activeId ? 'hidden sm:flex' : 'flex'
          }`}
        >
          <div className="p-3 border-b border-white/10 shrink-0">
            <Button
              size="sm"
              className="w-full comic-btn-quiet shadow-none"
              onClick={() => setNewChatOpen(true)}
              disabled={friends.length === 0}
            >
              New chat
            </Button>
            {friends.length === 0 && (
              <p className="mt-2 text-xs text-ink-muted px-1">
                Add friends first to start chatting.
              </p>
            )}
          </div>

          <ConversationList
            conversations={conversations}
            activeId={activeId}
            loading={conversationsLoading}
            friendsAvailable={friends.length > 0}
            onSelect={setActiveId}
            onNewChat={() => setNewChatOpen(true)}
          />
        </aside>

        <ChatThread
          active={active}
          userId={user?.id}
          messages={displayMessages}
          loading={messagesLoading}
          error={messagesError}
          errorDetail={errorDetail}
          onRetry={refetchMessages}
          participantsById={participantsById}
          peerTyping={peerTyping}
          typingLabel={typingLabel}
          typingAvatarUrl={typingParticipant?.avatarUrl}
          hasOlderMessages={!!hasNextPage}
          loadingOlder={isFetchingNextPage}
          onLoadOlder={() => fetchNextPage()}
          onBack={() => setActiveId(null)}
          onSendMessage={handleSendMessage}
          onSendTyping={sendTyping}
          onRename={() => {
            setRenameDraft(active?.name ?? '')
            setRenameOpen(true)
          }}
          onLeave={() => setLeaveOpen(true)}
        />
      </div>

      <NewChatModal
        open={newChatOpen}
        onClose={() => setNewChatOpen(false)}
        friends={friends}
        conversations={conversations}
        onPick={(id) => startChatMutation.mutate(id)}
        onCreateGroup={(name, memberUserIds) =>
          createGroupMutation.mutate({ name, memberUserIds })
        }
        pending={startChatMutation.isPending || createGroupMutation.isPending}
      />

      <GroupAdminModals
        active={active}
        renameOpen={renameOpen}
        renameDraft={renameDraft}
        onRenameDraftChange={setRenameDraft}
        onRenameClose={() => setRenameOpen(false)}
        onRenameSave={() => {
          if (!activeId || !renameDraft.trim()) return
          renameGroupMutation.mutate({ conversationId: activeId, name: renameDraft.trim() })
        }}
        renamePending={renameGroupMutation.isPending}
        leaveOpen={leaveOpen}
        onLeaveClose={() => setLeaveOpen(false)}
        onLeaveConfirm={() => activeId && leaveGroupMutation.mutate(activeId)}
        leavePending={leaveGroupMutation.isPending}
      />
    </div>
  )
}
