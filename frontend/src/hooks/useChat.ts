import { useEffect, useRef } from 'react'
import { useChatContext } from '../chat/ChatProvider'
import type { ChatTypingEvent, Message } from '../api/types'

/** Subscribe to global chat connection (managed by ChatProvider on the messages route). */
export function useChat(
  onMessage?: (message: Message) => void,
  onTyping?: (event: ChatTypingEvent) => void,
) {
  const { connected, sendMessage, sendTyping, subscribeMessages, subscribeTyping, setActiveConversation } =
    useChatContext()

  const onMessageRef = useRef(onMessage)
  const onTypingRef = useRef(onTyping)
  onMessageRef.current = onMessage
  onTypingRef.current = onTyping

  useEffect(() => {
    return subscribeMessages((msg) => onMessageRef.current?.(msg))
  }, [subscribeMessages])

  useEffect(() => {
    return subscribeTyping((event) => onTypingRef.current?.(event))
  }, [subscribeTyping])

  return { connected, sendMessage, sendTyping, setActiveConversation }
}
