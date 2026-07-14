import { ArrowLeft, Send, Users, Pencil, LogOut } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import type { Conversation, ConversationParticipant, Message, MessageMention } from '../../api/types'
import { useMentionComposer } from './MentionComposer'
import { MessageBody } from './MessageBody'
import { Button } from '../ui/Button'
import { ComicBurst } from '../ui/ComicBurst'
import { EmptyState } from '../ui/EmptyState'
import { BookLoaderCenter } from '../ui/BookLoader'
import { Textarea } from '../ui/Textarea'
import { displayName } from '../../lib/utils'
import { conversationAvatarUrl, conversationTitle } from '../../lib/messaging'
import { UserAvatar } from '../ui/UserAvatar'

export function ChatThread({
  active,
  userId,
  messages,
  loading,
  error,
  errorDetail,
  onRetry,
  participantsById,
  peerTyping,
  typingLabel,
  typingAvatarUrl,
  hasOlderMessages,
  loadingOlder,
  onLoadOlder,
  onBack,
  onSendMessage,
  onSendTyping,
  onRename,
  onLeave,
}: {
  active: Conversation | undefined
  userId: string | undefined
  messages: Message[]
  loading: boolean
  error: boolean
  errorDetail: string
  onRetry: () => void
  participantsById: Map<string, ConversationParticipant>
  peerTyping: boolean
  typingLabel: string
  typingAvatarUrl?: string | null
  hasOlderMessages: boolean
  loadingOlder: boolean
  onLoadOlder: () => void
  onBack: () => void
  onSendMessage: (conversationId: string, body: string, mentions?: MessageMention[]) => void
  onSendTyping: (conversationId: string, typing: boolean) => void
  onRename: () => void
  onLeave: () => void
}) {
  const [draft, setDraft] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)
  const scrollRef = useRef<HTMLDivElement>(null)
  const activeId = active?.id ?? null

  const { mentions, clearMentions, textareaRef, MentionDropdown } = useMentionComposer(
    draft,
    setDraft,
    active,
  )

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages.length, activeId, peerTyping])

  const handleScroll = useCallback(() => {
    const el = scrollRef.current
    if (!el || loadingOlder || !hasOlderMessages) return
    if (el.scrollTop <= 48) {
      onLoadOlder()
    }
  }, [hasOlderMessages, loadingOlder, onLoadOlder])

  const handleSend = useCallback(() => {
    const body = draft.trim()
    if (!body || !activeId || !userId) return
    onSendMessage(activeId, body, mentions)
    setDraft('')
    clearMentions()
  }, [activeId, clearMentions, draft, mentions, onSendMessage, userId])

  const displayMessages = messages ?? []
  const senderFor = useCallback(
    (senderId: string) => participantsById.get(senderId),
    [participantsById],
  )

  return (
    <div className={`flex flex-1 flex-col min-h-0 min-w-0 ${activeId ? 'flex' : 'hidden sm:flex'}`}>
      {!activeId ? (
        <EmptyState
          icon={Users}
          title="Select a conversation"
          description="Choose a chat from the list or start a new one."
          className="m-auto border-0 bg-transparent"
        />
      ) : (
        <>
          <div className="shrink-0 border-b border-white/10 px-4 py-3 flex items-center gap-2">
            <ComicBurst
              type="button"
              className="sm:hidden rounded-lg p-1 text-ink-muted hover:bg-white/5 comic-btn-quiet"
              onClick={onBack}
              aria-label="Back to conversations"
            >
              <ArrowLeft className="h-5 w-5" />
            </ComicBurst>
            <div className="min-w-0 flex-1 flex items-center gap-2">
              {active?.type === 'DIRECT' && (
                <UserAvatar
                  name={active ? conversationTitle(active) : 'Chat'}
                  avatarUrl={active ? conversationAvatarUrl(active) : undefined}
                  size="sm"
                  className="h-8 w-8 text-xs shrink-0"
                />
              )}
              <div className="min-w-0">
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
            </div>
            {active?.type === 'GROUP' && (
              <div className="flex items-center gap-1 shrink-0">
                {active.viewerIsCreator && (
                  <Button
                    size="sm"
                    variant="secondary"
                    className="comic-btn-quiet shadow-none"
                    onClick={onRename}
                  >
                    <Pencil className="h-3.5 w-3.5" />
                    Rename
                  </Button>
                )}
                <Button
                  size="sm"
                  variant="secondary"
                  className="comic-btn-quiet shadow-none"
                  onClick={onLeave}
                >
                  <LogOut className="h-3.5 w-3.5" />
                  Leave
                </Button>
              </div>
            )}
          </div>

          <div
            ref={scrollRef}
            onScroll={handleScroll}
            className="flex-1 min-h-0 overflow-y-auto px-3 py-3 space-y-1.5"
          >
            {hasOlderMessages && (
              <div className="text-center py-2">
                <button
                  type="button"
                  onClick={onLoadOlder}
                  disabled={loadingOlder}
                  className="text-xs text-ink-muted hover:text-ink disabled:opacity-50"
                >
                  {loadingOlder ? 'Loading older messages…' : 'Load older messages'}
                </button>
              </div>
            )}
            {loading && displayMessages.length === 0 && <BookLoaderCenter />}
            {error && (
              <div className="text-center py-8">
                <p className="text-sm text-danger">Could not load messages.</p>
                <p className="text-xs text-ink-muted mt-1">{errorDetail}</p>
                <Button size="sm" variant="secondary" className="mt-3" onClick={onRetry}>
                  Retry
                </Button>
              </div>
            )}
            {!loading && !error && displayMessages.length === 0 && (
              <p className="text-sm text-ink-muted text-center py-8">No messages yet. Say hello!</p>
            )}
            {displayMessages.map((m, i) => {
              const mine = String(m.senderId) === String(userId)
              const sender = senderFor(String(m.senderId))
              const senderName = sender ? displayName(sender) : 'Friend'
              const isGroup = active?.type === 'GROUP'
              return (
                <div
                  key={m.id}
                  className={`flex items-end gap-2 ${mine ? 'justify-end' : 'justify-start'} ${mine ? 'message-slide-in' : 'message-bounce-in'}`}
                  style={{ animationDelay: `${Math.min(i, 12) * 40}ms` }}
                >
                  {!mine && (
                    <UserAvatar
                      name={senderName}
                      avatarUrl={sender?.avatarUrl}
                      size="sm"
                      className="mb-0.5 h-7 w-7 text-[10px]"
                    />
                  )}
                  <div
                    className={`max-w-[75%] rounded-2xl px-4 py-2.5 text-sm ${
                      mine
                        ? 'bg-accent text-void font-medium border border-accent/60 shadow-sm'
                        : 'bg-paper-elevated border border-white/20 text-ink shadow-sm'
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
                <UserAvatar
                  name={typingLabel}
                  avatarUrl={typingAvatarUrl}
                  size="sm"
                  className="mb-0.5 h-7 w-7 text-[10px]"
                />
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
                if (activeId) onSendTyping(activeId, true)
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
  )
}
