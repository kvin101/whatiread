import type { InfiniteData } from '@tanstack/react-query'
import type { CursorPage, Message } from '../api/types'

export type MessageInfiniteData = InfiniteData<CursorPage<Message>, string | undefined>

export function appendMessagePage(
  data: MessageInfiniteData | undefined,
  incoming: Message,
): MessageInfiniteData | undefined {
  if (!data?.pages.length) {
    return {
      pageParams: [undefined],
      pages: [{ items: [incoming], nextCursor: null, hasMore: false }],
    }
  }
  const pages = [...data.pages]
  const lastIdx = pages.length - 1
  const lastPage = pages[lastIdx]
  const withoutOptimistic = lastPage.items.filter(
    (m) =>
      !(
        m.id.startsWith('temp-') &&
        m.conversationId === incoming.conversationId &&
        m.senderId === incoming.senderId &&
        m.body === incoming.body
      ),
  )
  if (withoutOptimistic.some((m) => m.id === incoming.id)) {
    pages[lastIdx] = { ...lastPage, items: withoutOptimistic }
    return { ...data, pages }
  }
  pages[lastIdx] = { ...lastPage, items: [...withoutOptimistic, incoming] }
  return { ...data, pages }
}

export function prependOptimisticMessage(
  data: MessageInfiniteData | undefined,
  optimistic: Message,
): MessageInfiniteData {
  if (!data?.pages.length) {
    return {
      pageParams: [undefined],
      pages: [{ items: [optimistic], nextCursor: null, hasMore: false }],
    }
  }
  const pages = [...data.pages]
  const lastIdx = pages.length - 1
  const lastPage = pages[lastIdx]
  pages[lastIdx] = { ...lastPage, items: [...lastPage.items, optimistic] }
  return { ...data, pages }
}

export function removeOptimisticMessage(
  data: MessageInfiniteData | undefined,
  optimisticId: string,
): MessageInfiniteData | undefined {
  if (!data) return data
  const pages = data.pages.map((page) => ({
    ...page,
    items: page.items.filter((m) => m.id !== optimisticId),
  }))
  return { ...data, pages }
}
