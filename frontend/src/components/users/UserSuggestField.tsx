import { UserPlus } from 'lucide-react'
import type { UserSuggestResult } from '../../api/types'
import { useUserSuggest, type UserSuggestScope } from '../../hooks/useUserSuggest'
import { initials } from '../../lib/utils'
import { SuggestField } from '../ui/SuggestField'

export function UserSuggestField({
  value,
  onValueChange,
  onSelect,
  scope = 'invite',
  placeholder = 'Search by username or name…',
  inputId,
  enabled = true,
  autoFocus = false,
}: {
  value: string
  onValueChange: (value: string) => void
  onSelect: (user: UserSuggestResult) => void
  scope?: UserSuggestScope
  placeholder?: string
  inputId?: string
  enabled?: boolean
  autoFocus?: boolean
}) {
  const { data: suggestions = [], isFetching, isError } = useUserSuggest(value, scope, enabled)

  return (
    <SuggestField
      value={value}
      onValueChange={onValueChange}
      suggestions={suggestions}
      isFetching={isFetching}
      isError={isError}
      enabled={enabled}
      placeholder={placeholder}
      inputId={inputId}
      loadingLabel="Finding readers…"
      emptyLabel="No matching readers"
      autoFocus={autoFocus}
      getKey={(user) => user.id}
      onSelect={onSelect}
      renderItem={(user) => (
        <>
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-sage/15 text-xs font-semibold text-sage">
            {initials(user.displayName)}
          </div>
          <div className="min-w-0">
            <p className="truncate text-base font-medium text-ink">{user.displayName}</p>
            <p className="truncate text-sm text-ink-muted">@{user.username}</p>
          </div>
          <UserPlus className="ml-auto h-4 w-4 shrink-0 text-accent" />
        </>
      )}
    />
  )
}
