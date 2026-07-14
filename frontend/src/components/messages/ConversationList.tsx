import { MessageCircle, MessageSquarePlus, Users } from 'lucide-react'
import type { Conversation } from '../../api/types'
import { conversationTitle } from '../../lib/messaging'
import { initials } from '../../lib/utils'
import { Button } from '../ui/Button'
import { EmptyState } from '../ui/EmptyState'
import { BookLoaderCenter } from '../ui/BookLoader'

export function ConversationList({
  conversations,
  activeId,
  loading,
  friendsAvailable,
  onSelect,
  onNewChat,
}: {
  conversations: Conversation[]
  activeId: string | null
  loading: boolean
  friendsAvailable: boolean
  onSelect: (id: string) => void
  onNewChat: () => void
}) {
  if (loading) {
    return <BookLoaderCenter className="flex-1" />
  }

  return (
    <ul className="flex-1 min-h-0 overflow-y-auto p-2 space-y-1">
      {conversations.map((c, i) => (
        <li key={c.id}>
          <button
            type="button"
            onClick={() => onSelect(c.id)}
            style={{ animationDelay: `${i * 30}ms` }}
            className={`w-full flex items-center gap-3 rounded-lg px-3 py-2 text-left transition-colors list-enter ${
              c.id === activeId
                ? 'bg-white/[0.06] border-l-2 border-l-accent pl-[calc(0.75rem-2px)] text-ink'
                : 'hover:bg-white/5 border-l-2 border-l-transparent pl-[calc(0.75rem-2px)]'
            }`}
          >
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-sage/15 text-xs font-semibold text-sage">
              {c.type === 'GROUP' ? <Users className="h-4 w-4" /> : initials(conversationTitle(c))}
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center justify-between gap-2">
                <p className="text-sm font-medium truncate">{conversationTitle(c)}</p>
                {c.unreadCount > 0 && (
                  <span className="relative z-10 shrink-0 rounded-full bg-accent px-1.5 py-0.5 text-[10px] font-bold text-void">
                    {c.unreadCount > 9 ? '9+' : c.unreadCount}
                  </span>
                )}
              </div>
              {c.type === 'GROUP' && (
                <p className="text-[10px] text-ink-muted">{c.memberCount} members</p>
              )}
              {c.lastMessage && (
                <p className="text-xs text-ink-muted truncate">{c.lastMessage.body}</p>
              )}
            </div>
          </button>
        </li>
      ))}
      {conversations.length === 0 && (
        <EmptyState
          icon={MessageCircle}
          title="No threads yet"
          description="Hit New chat to message a friend."
          className="m-2 py-10 border-0 bg-transparent"
          action={
            friendsAvailable ? (
              <Button size="sm" className="comic-btn-quiet shadow-none" onClick={onNewChat}>
                <MessageSquarePlus className="h-4 w-4" />
                New chat
              </Button>
            ) : undefined
          }
        />
      )}
    </ul>
  )
}
