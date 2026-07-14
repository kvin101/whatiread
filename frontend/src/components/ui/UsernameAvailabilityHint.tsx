import { Loader2 } from 'lucide-react'
import type { UsernameAvailability } from '../../api/types'

type Props = {
  value: string
  check: {
    isFetching: boolean
    data?: UsernameAvailability
  }
}

export function UsernameAvailabilityHint({ value, check }: Props) {
  const trimmed = value.trim()

  if (!trimmed || trimmed.length < 3) {
    return (
      <p className="mt-1 text-xs text-ink-muted">
        3–30 characters; start with a letter; letters, numbers, underscores only
      </p>
    )
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
