import { Loader2 } from 'lucide-react'
import type { UsernameAvailability } from '../../api/types'
import { UsernameUtils } from '../../lib/username'

type Props = {
  value: string
  check: {
    isFetching: boolean
    data?: UsernameAvailability
  }
  /** When the field matches this saved handle, do not show an availability result. */
  savedUsername?: string
}

export function UsernameAvailabilityHint({ value, check, savedUsername }: Props) {
  const trimmed = value.trim()

  if (!trimmed || trimmed.length < 3) {
    return (
      <p className="mt-1 text-xs text-ink-muted">
        3–30 characters; start with a letter; letters, numbers, underscores only
      </p>
    )
  }

  if (savedUsername && UsernameUtils.equals(trimmed, savedUsername)) {
    return null
  }

  if (check.isFetching) {
    return (
      <p className="mt-1 flex items-center gap-1 text-xs text-ink-muted">
        <Loader2 className="h-3 w-3 animate-spin" />
        Checking availability…
      </p>
    )
  }

  const result = check.data
  if (!result) return null

  if (!result.valid) {
    return <p className="mt-1 text-xs text-danger">{result.message ?? 'Invalid username'}</p>
  }

  if (result.available) {
    return <p className="mt-1 text-xs text-sage">@{result.username} is available</p>
  }

  return <p className="mt-1 text-xs text-danger">{result.message ?? 'Username already taken'}</p>
}
