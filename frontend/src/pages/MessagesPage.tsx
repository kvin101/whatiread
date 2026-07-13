import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, LogOut, MessageCircle, MessageSquarePlus, Pencil, Send, Users } from 'lucide-react'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { conversationsApi } from '../api/conversations'
import { friendsApi } from '../api/friends'
import type { Conversation, ConversationParticipant, Message } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { MessageBody } from '../components/messages/MessageBody'
import { NewChatModal } from '../components/messages/NewChatModal'
import { useMentionComposer } from '../components/messages/MentionComposer'
import { useChat } from '../hooks/useChat'
import { Button } from '../components/ui/Button'
import { ComicBurst } from '../components/ui/ComicBurst'
import { Modal } from '../components/ui/Modal'
import { EmptyState } from '../components/ui/EmptyState'
import { BookLoaderCenter } from '../components/ui/BookLoader'
import { Input } from '../components/ui/Input'
import { Textarea } from '../components/ui/Textarea'
import { PageHeader } from '../components/layout/PageHeader'
import { copy } from '../lib/copy'
import { displayName, initials } from '../lib/utils'
import { QUERY_KEYS } from '../lib/constants'

function normalizeMessages(raw: Message[]): Message[] {
  return raw.map((m) => ({
    ...m,
    id: String(m.id),
    conversationId: String(m.conversationId),
    senderId: String(m.senderId),
    mentions: m.mentions ?? [],
  }))
}

function normalizeParticipant(p: ConversationParticipant): ConversationParticipant {
  return { ...p, id: String(p.id) }
}

function normalizeConversations(raw: Conversation[]): Conversation[] {
  return raw.map((c) => ({
    ...c,
    id: String(c.id),
    type: c.type ?? 'DIRECT',
    name: c.name ?? undefined,
    createdById: c.createdById ? String(c.createdById) : undefined,
    otherParticipant: c.otherParticipant ? normalizeParticipant(c.otherParticipant) : undefined,
    participants: c.participants?.map(normalizeParticipant),
    memberCount: c.memberCount ?? (c.type === 'GROUP' ? c.participants?.length ?? 0 : 2),
    viewerIsAdmin: c.viewerIsAdmin ?? false,
    viewerIsCreator: c.viewerIsCreator ?? false,
    lastMessage: c.lastMessage
      ? {
          ...c.lastMessage,
          id: String(c.lastMessage.id),
          conversationId: String(c.lastMessage.conversationId),
          senderId: String(c.lastMessage.senderId),
        }
      : undefined,
  }))
}

function conversationTitle(conversation: Conversation): string {
  if (conversation.type === 'GROUP') {
    return conversation.name?.trim() || 'Group chat'
  }
  return conversation.otherParticipant ? displayName(conversation.otherParticipant) : 'Chat'
}

function participantMap(conversation: Conversation | undefined): Map<string, ConversationParticipant> {
  const map = new Map<string, ConversationParticipant>()
  if (!conversation) return map
  if (conversation.otherParticipant) {
    map.set(String(conversation.otherParticipant.id), conversation.otherParticipant)
  }
  conversation.participants?.forEach((p) => map.set(String(p.id), p))
  return map
}

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
  const [draft, setDraft] = useState('')
  const [peerTyping, setPeerTyping] = useState(false)
  const [typingUserId, setTypingUserId] = useState<string | null>(null)
  const bottomRef = useRef<HTMLDivElement>(null)
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const activeIdRef = useRef(activeId)
  activeIdRef.current = activeId

  const { data: conversations = [], isLoading: conversationsLoading } = useQuery({
    queryKey: QUERY_KEYS.conversations.all,
    queryFn: async () => normalizeConversations(await conversationsApi.list()),
  })

  const { data: friends = [] } = useQuery({
    queryKey: QUERY_KEYS.friends.all,
    queryFn: friendsApi.list,
  })

  const active = useMemo(
    () => conversations.find((c) => c.id === activeId),
    [conversations, activeId],
  )

  const participantsById = useMemo(() => participantMap(active), [active])

  const {
    data: messages,
    isPending: messagesLoading,
    isError: messagesError,
    error: messagesFetchError,
    refetch: refetchMessages,
  } = useQuery({
    queryKey: QUERY_KEYS.messages(activeId!),
    queryFn: async () => normalizeMessages(await conversationsApi.messages(activeId!)),
    enabled: !!activeId,
    staleTime: 0,
    retry: 2,
  })

  const displayMessages = messages ?? []

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
  }, [activeId, messages, messagesLoading, messagesError, queryClient])

  const { mentions, clearMentions, textareaRef, MentionDropdown } = useMentionComposer(
    draft,
    setDraft,
    active,
  )

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [displayMessages, activeId])

  const startChatMutation = useMutation({
    mutationFn: (friendUserId: string) => conversationsApi.withFriend(friendUserId),
    onSuccess: (conv) => {
      const normalized = normalizeConversations([conv])[0]
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.conversations.all })
      setNewChatOpen(false)
      setActiveId(normalized.id)
    },
  })

  const createGroupMutation = useMutation({
    mutationFn: ({ name, memberUserIds }: { name: string; memberUserIds: string[] }) =>
      conversationsApi.createGroup(name, memberUserIds),
    onSuccess: (conv) => {
      const normalized = normalizeConversations([conv])[0]
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

  const handleSend = () => {
    const body = draft.trim()
    if (!body || !activeId || !user) return
    try {
      sendMessage(activeId, body, mentions)
      const optimistic: Message = {
        id: `temp-${Date.now()}`,
        conversationId: activeId,
        senderId: user.id,
        body,
        mentions: mentions.length ? mentions : undefined,
        sentAt: new Date().toISOString(),
      }
      queryClient.setQueryData<Message[]>(QUERY_KEYS.messages(activeId), (old) => {
        const base = old ?? []
        return [...base, optimistic]
      })
      setDraft('')
      clearMentions()
    } catch {
      /* websocket not connected — message may still queue on retry */
    }
  }

  const errorDetail =
    messagesFetchError instanceof Error ? messagesFetchError.message : 'Unknown error'

  const typingParticipant = typingUserId ? participantsById.get(typingUserId) : undefined
  const typingLabel = typingParticipant ? displayName(typingParticipant) : 'Someone'

  return (
    <div>
      <PageHeader
        eyebrow="DMs"
        title={copy.messages.title}
        description={
          <span className="inline-flex items-center gap-2">
            {copy.messages.description}
            <span
              className={`inline-block h-2 w-2 rounded-full ${connected ? 'bg-sage' : 'bg-border'}`}
              title={connected ? 'Connected' : 'Connecting…'}
            />
          </span>
        }
      />

      <div className="flex h-[calc(100dvh-14rem)] min-h-[420px] overflow-hidden rounded-3xl chat-panel">
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
              <MessageSquarePlus className="h-4 w-4" />
              New chat
            </Button>
            {friends.length === 0 && (
              <p className="mt-2 text-xs text-ink-muted px-1">
                Add friends first to start chatting.
              </p>
            )}
          </div>

          {conversationsLoading ? (
            <BookLoaderCenter className="flex-1" />
          ) : (
            <ul className="flex-1 min-h-0 overflow-y-auto p-2 space-y-1">
              {conversations.map((c, i) => (
                <ConversationItem
                  key={c.id}
                  conversation={c}
                  active={c.id === activeId}
                  onClick={() => setActiveId(c.id)}
                  style={{ animationDelay: `${i * 30}ms` }}
                />
              ))}
              {conversations.length === 0 && (
                <EmptyState
                  icon={MessageCircle}
                  title="No threads yet"
                  description="Hit New chat to message a friend."
                  className="m-2 py-10 border-0 bg-transparent"
                  action={
                    friends.length > 0 ? (
                      <Button size="sm" className="comic-btn-quiet shadow-none" onClick={() => setNewChatOpen(true)}>
                        New chat
                      </Button>
                    ) : undefined
                  }
                />
              )}
            </ul>
          )}
        </aside>

        <div
          className={`flex flex-1 flex-col min-h-0 min-w-0 ${
            activeId ? 'flex' : 'hidden sm:flex'
          }`}
        >
          {!activeId ? (
            <EmptyState
              icon={MessageCircle}
              title="Pick someone to gossip with"
              description={copy.messages.empty.description}
              className="m-auto border-0 bg-transparent"
              action={
                friends.length > 0 ? (
                  <Button className="comic-btn-quiet shadow-none" onClick={() => setNewChatOpen(true)}>
                    <MessageSquarePlus className="h-4 w-4" />
                    New chat
                  </Button>
                ) : undefined
              }
            />
          ) : (
            <>
              <div className="shrink-0 border-b border-white/10 px-4 py-3 flex items-center gap-2">
                <ComicBurst
                  type="button"
                  className="sm:hidden rounded-lg p-1 text-ink-muted hover:bg-white/5 comic-btn-quiet"
                  onClick={() => setActiveId(null)}
                  aria-label="Back to conversations"
                >
                  <ArrowLeft className="h-5 w-5" />
                </ComicBurst>
                <div className="min-w-0 flex-1">
                  <p className="font-medium text-ink truncate">
                    {active ? conversationTitle(active) : 'Chat'}
                  </p>
                  {active?.type === 'GROUP' && (
                    <p className="text-xs text-ink-muted inline-flex items-center gap-1">
                      <Users className="h-3 w-3" />
                      {active.memberCount} member{active.memberCount === 1 ? '' : 's'}
                    </p>
                  )}
                </div>
                {active?.type === 'GROUP' && (
                  <div className="flex items-center gap-1 shrink-0">
                    {active.viewerIsCreator && (
                      <Button
                        size="sm"
                        variant="secondary"
                        className="comic-btn-quiet shadow-none"
                        onClick={() => {
                          setRenameDraft(active.name ?? '')
                          setRenameOpen(true)
                        }}
                      >
                        <Pencil className="h-3.5 w-3.5" />
                        Rename
                      </Button>
                    )}
                    <Button
                      size="sm"
                      variant="secondary"
                      className="comic-btn-quiet shadow-none"
                      onClick={() => setLeaveOpen(true)}
                    >
                      <LogOut className="h-3.5 w-3.5" />
                      Leave
                    </Button>
                  </div>
                )}
              </div>
              <div className="flex-1 min-h-0 overflow-y-auto px-3 py-3 space-y-1.5">
                {messagesLoading && displayMessages.length === 0 && <BookLoaderCenter />}
                {messagesError && (
                  <div className="text-center py-8">
                    <p className="text-sm text-danger">Could not load messages.</p>
                    <p className="text-xs text-ink-muted mt-1">{errorDetail}</p>
                    <Button size="sm" variant="secondary" className="mt-3" onClick={() => refetchMessages()}>
                      Retry
                    </Button>
                  </div>
                )}
                {!messagesLoading && !messagesError && displayMessages.length === 0 && (
                  <p className="text-sm text-ink-muted text-center py-8">
                    No messages yet. Say hello!
                  </p>
                )}
                {displayMessages.map((m, i) => {
                  const mine = String(m.senderId) === String(user?.id)
                  const sender = participantsById.get(String(m.senderId))
                  const senderName = sender ? displayName(sender) : 'Friend'
                  const isGroup = active?.type === 'GROUP'
                  return (
                    <div
                      key={m.id}
                      className={`flex items-end gap-2 ${mine ? 'justify-end' : 'justify-start'} ${mine ? 'message-slide-in' : 'message-bounce-in'}`}
                      style={{ animationDelay: `${Math.min(i, 12) * 40}ms` }}
                    >
                      {!mine && (
                        <div
                          className="mb-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full border border-sage/40 bg-sage/20 text-[10px] font-bold text-sage shadow-sm"
                          aria-hidden
                          title={senderName}
                        >
                          {initials(senderName)}
                        </div>
                      )}
                      <div
                        className={`max-w-[75%] px-4 py-2.5 text-sm ${
                          mine
                            ? 'speech-bubble-out bg-accent text-void font-medium border border-accent/60 shadow-md'
                            : 'speech-bubble-in bg-paper-elevated border-2 border-white/30 text-ink shadow-md'
                        }`}
                      >
                        {isGroup && !mine && (
                          <p className="text-[10px] font-semibold text-sage mb-1">{senderName}</p>
                        )}
                        <MessageBody body={m.body} mentions={m.mentions} outgoing={mine} />
                      </div>
                    </div>
                  )
                })}
                {peerTyping && (
                  <div className="flex items-end gap-2 justify-start message-bounce-in">
                    <div
                      className="mb-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full border border-sage/40 bg-sage/20 text-[10px] font-bold text-sage shadow-sm"
                      aria-hidden
                    >
                      {initials(typingLabel)}
                    </div>
                    <div className="typing-indicator" aria-label={`${typingLabel} typing`}>
                      <span />
                      <span />
                      <span />
                    </div>
                  </div>
                )}
                <div ref={bottomRef} />
              </div>
              <div className="shrink-0 border-t border-white/10 p-3 flex gap-2 relative">
                {MentionDropdown}
                <Textarea
                  ref={textareaRef}
                  rows={2}
                  className="min-h-0"
                  placeholder="Type a message… Use @ to mention"
                  value={draft}
                  onChange={(e) => {
                    setDraft(e.target.value)
                    if (activeId) sendTyping(activeId, true)
                  }}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && !e.shiftKey) {
                      e.preventDefault()
                      handleSend()
                    }
                  }}
                />
                <Button onClick={handleSend} disabled={!draft.trim()} className="shrink-0 self-end comic-btn-quiet shadow-none">
                  <Send className="h-4 w-4" />
                </Button>
              </div>
            </>
          )}
        </div>
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

      <ModalRenameGroup
        open={renameOpen}
        value={renameDraft}
        onChange={setRenameDraft}
        onClose={() => setRenameOpen(false)}
        onSave={() => {
          if (!activeId || !renameDraft.trim()) return
          renameGroupMutation.mutate({ conversationId: activeId, name: renameDraft.trim() })
        }}
        pending={renameGroupMutation.isPending}
      />

      <Modal open={leaveOpen} onClose={() => setLeaveOpen(false)} title="Leave group?">
        <p className="text-sm text-ink-muted mb-4">
          You will no longer receive messages from this group.
        </p>
        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button variant="secondary" onClick={() => setLeaveOpen(false)}>
            Cancel
          </Button>
          <Button
            variant="danger"
            disabled={leaveGroupMutation.isPending}
            onClick={() => activeId && leaveGroupMutation.mutate(activeId)}
          >
            Leave group
          </Button>
        </div>
      </Modal>
    </div>
  )
}

function ModalRenameGroup({
  open,
  value,
  onChange,
  onClose,
  onSave,
  pending,
}: {
  open: boolean
  value: string
  onChange: (v: string) => void
  onClose: () => void
  onSave: () => void
  pending?: boolean
}) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <button type="button" className="absolute inset-0 bg-void/70" onClick={onClose} aria-label="Close" />
      <div className="relative z-10 w-full max-w-md rounded-3xl border border-white/10 bg-paper p-6 shadow-xl">
        <h2 className="text-lg font-semibold text-ink mb-4">Rename group</h2>
        <Input value={value} onChange={(e) => onChange(e.target.value)} maxLength={255} autoFocus />
        <div className="mt-4 flex gap-2">
          <Button className="flex-1" onClick={onSave} disabled={!value.trim() || pending}>
            Save
          </Button>
          <Button variant="secondary" className="flex-1" onClick={onClose}>
            Cancel
          </Button>
        </div>
      </div>
    </div>
  )
}

function ConversationItem({
  conversation,
  active,
  onClick,
  style,
}: {
  conversation: Conversation
  active: boolean
  onClick: () => void
  style?: React.CSSProperties
}) {
  const isGroup = conversation.type === 'GROUP'
  const name = conversationTitle(conversation)
  return (
    <button
      type="button"
      onClick={onClick}
      style={style}
      className={`w-full flex items-center gap-3 rounded-lg px-3 py-2 text-left transition-colors list-enter ${
        active
          ? 'bg-white/[0.06] border-l-2 border-l-accent pl-[calc(0.75rem-2px)] text-ink'
          : 'hover:bg-white/5 border-l-2 border-l-transparent pl-[calc(0.75rem-2px)]'
      }`}
    >
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-sage/15 text-xs font-semibold text-sage">
        {isGroup ? <Users className="h-4 w-4" /> : initials(name)}
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex items-center justify-between gap-2">
          <p className="text-sm font-medium truncate">{name}</p>
          {conversation.unreadCount > 0 && (
            <span className="relative z-10 shrink-0 rounded-full bg-accent px-1.5 py-0.5 text-[10px] font-bold text-void">
              {conversation.unreadCount > 9 ? '9+' : conversation.unreadCount}
            </span>
          )}
        </div>
        {isGroup && (
          <p className="text-[10px] text-ink-muted">{conversation.memberCount} members</p>
        )}
        {conversation.lastMessage && (
          <p className="text-xs text-ink-muted truncate">{conversation.lastMessage.body}</p>
        )}
      </div>
    </button>
  )
}
