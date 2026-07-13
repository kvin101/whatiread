import { useQuery } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { friendsApi } from '../../api/friends'
import { shelvesApi } from '../../api/shelves'
import type { Conversation, MessageMention } from '../../api/types'
import { displayName } from '../../lib/utils'
import { QUERY_KEYS } from '../../lib/constants'

type MentionOption = MessageMention & { insertText: string }

export function useMentionComposer(
  draft: string,
  setDraft: (v: string) => void,
  conversation: Conversation | undefined,
) {
  const [mentions, setMentions] = useState<MessageMention[]>([])
  const [mentionQuery, setMentionQuery] = useState<string | null>(null)
  const textareaRef = useRef<HTMLTextAreaElement | null>(null)

  const { data: friends = [] } = useQuery({
    queryKey: QUERY_KEYS.friends.all,
    queryFn: friendsApi.list,
    enabled: mentionQuery !== null,
  })

  const { data: myShelves = [] } = useQuery({
    queryKey: QUERY_KEYS.shelves.all,
    queryFn: shelvesApi.listMine,
    enabled: mentionQuery !== null,
  })

  useEffect(() => {
    const el = textareaRef.current
    if (!el) return
    const pos = el.selectionStart ?? draft.length
    const before = draft.slice(0, pos)
    const at = before.lastIndexOf('@')
    if (at === -1 || (at > 0 && /\S/.test(before[at - 1]!))) {
      setMentionQuery(null)
      return
    }
    const q = before.slice(at + 1)
    if (q.includes(' ') || q.length > 40) {
      setMentionQuery(null)
      return
    }
    setMentionQuery(q.toLowerCase())
  }, [draft])

  const options: MentionOption[] = []
  if (mentionQuery !== null && conversation) {
    const seen = new Set<string>()
    const addOption = (opt: MentionOption) => {
      const key = `${opt.type}-${opt.targetId}`
      if (seen.has(key)) return
      seen.add(key)
      options.push(opt)
    }

    const other = conversation.otherParticipant
    if (other) {
      const userLabel = displayName(other)
      if (userLabel.toLowerCase().includes(mentionQuery) || mentionQuery === '') {
        addOption({
          type: 'USER',
          targetId: other.id,
          label: userLabel,
          insertText: `@${userLabel} `,
        })
      }
    }
    if (conversation.type === 'GROUP' && conversation.participants) {
      for (const p of conversation.participants) {
        const label = displayName(p)
        if (label.toLowerCase().includes(mentionQuery)) {
          addOption({
            type: 'USER',
            targetId: p.id,
            label,
            insertText: `@${label} `,
          })
        }
      }
    }
    for (const f of friends) {
      const label = displayName(f)
      if (label.toLowerCase().includes(mentionQuery)) {
        addOption({
          type: 'USER',
          targetId: f.id,
          label,
          insertText: `@${label} `,
        })
      }
    }
    for (const s of myShelves) {
      if (s.name.toLowerCase().includes(mentionQuery)) {
        addOption({
          type: 'SHELF',
          targetId: s.id,
          label: s.name,
          insertText: `@${s.name} `,
        })
      }
    }
  }

  const pickMention = (opt: MentionOption) => {
    const el = textareaRef.current
    if (!el) return
    const pos = el.selectionStart ?? draft.length
    const before = draft.slice(0, pos)
    const at = before.lastIndexOf('@')
    if (at === -1) return
    const next = draft.slice(0, at) + opt.insertText + draft.slice(pos)
    setDraft(next)
    setMentions((prev) => {
      if (prev.some((m) => m.type === opt.type && m.targetId === opt.targetId)) return prev
      return [...prev, { type: opt.type, targetId: opt.targetId, label: opt.label }]
    })
    setMentionQuery(null)
    requestAnimationFrame(() => el.focus())
  }

  const clearMentions = () => setMentions([])

  const MentionDropdown =
    mentionQuery !== null && options.length > 0 ? (
      <ul className="absolute bottom-full left-0 right-0 mb-1 max-h-40 overflow-y-auto rounded-2xl border-2 border-accent/25 bg-paper-elevated shadow-lg z-10 manga-panel">
        {options.slice(0, 8).map((opt) => (
          <li key={`${opt.type}-${opt.targetId}`}>
            <button
              type="button"
              className="w-full rounded-xl px-3 py-2 text-left text-sm hover:bg-accent/10 transition-colors first:mt-1 last:mb-1 mx-1"
              onMouseDown={(e) => {
                e.preventDefault()
                pickMention(opt)
              }}
            >
              <span className="text-xs text-ink-muted">{opt.type}</span>
              <span className="ml-2 text-ink">{opt.label}</span>
            </button>
          </li>
        ))}
      </ul>
    ) : null

  return { mentions, clearMentions, textareaRef, MentionDropdown }
}
