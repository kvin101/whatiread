import type { Conversation, ConversationParticipant, Message } from '../api/types'
import { displayName } from './utils'

export function normalizeMessagingIds(raw: {
  conversations?: Conversation[]
  messages?: Message[]
}) {
  return {
    conversations: raw.conversations ? normalizeConversations(raw.conversations) : undefined,
    messages: raw.messages ? normalizeMessages(raw.messages) : undefined,
  }
}

export function normalizeMessages(raw: Message[]): Message[] {
  return raw.map((m) => ({
    ...m,
    id: String(m.id),
    conversationId: String(m.conversationId),
    senderId: String(m.senderId),
    mentions: m.mentions ?? [],
  }))
}

export function normalizeParticipant(p: ConversationParticipant): ConversationParticipant {
  return { ...p, id: String(p.id) }
}

export function normalizeConversations(raw: Conversation[]): Conversation[] {
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

export function conversationAvatarUrl(conversation: Conversation): string | null | undefined {
  if (conversation.type === 'GROUP') return undefined
  return conversation.otherParticipant?.avatarUrl
}

export function conversationTitle(conversation: Conversation): string {
  if (conversation.type === 'GROUP') {
    return conversation.name?.trim() || 'Group chat'
  }
  return conversation.otherParticipant ? displayName(conversation.otherParticipant) : 'Chat'
}

export function participantMap(conversation: Conversation | undefined): Map<string, ConversationParticipant> {
  const map = new Map<string, ConversationParticipant>()
  if (!conversation) return map
  if (conversation.otherParticipant) {
    map.set(String(conversation.otherParticipant.id), conversation.otherParticipant)
  }
  conversation.participants?.forEach((p) => map.set(String(p.id), p))
  return map
}

/** WhatsApp-style inbox: most recently active conversations first. */
export function sortConversations(conversations: Conversation[]): Conversation[] {
  return [...conversations].sort((left, right) => {
    const leftAt = left.lastMessage?.sentAt ?? ''
    const rightAt = right.lastMessage?.sentAt ?? ''
    return rightAt.localeCompare(leftAt)
  })
}

export function flattenMessagePages(
  pages: Array<{ items: Message[] }> | undefined,
): Message[] {
  if (!pages?.length) return []
  return pages
    .slice()
    .reverse()
    .flatMap((page) => page.items)
}
