import type { ShelfVisibility } from '../../api/types'
import { VISIBILITY_HINTS, VISIBILITY_LABELS } from '../../lib/constants'
import { cn } from '../../lib/utils'

export const SHELF_VISIBILITY_OPTIONS = ['SECRET', 'PRIVATE', 'FRIENDS', 'PUBLIC'] as const

export function VisibilityPicker({
  value,
  onChange,
  compact = false,
  label = 'Visibility',
  disabledOptions = [],
  excludeOptions = [],
}: {
  value: ShelfVisibility
  onChange: (value: ShelfVisibility) => void
  compact?: boolean
  label?: string
  disabledOptions?: ShelfVisibility[]
  excludeOptions?: ShelfVisibility[]
}) {
  const options = SHELF_VISIBILITY_OPTIONS.filter((option) => !excludeOptions.includes(option))

  if (compact) {
    return (
      <div className="flex flex-wrap gap-2">
        {options.map((option) => {
          const disabled = disabledOptions.includes(option)
          return (
            <button
              key={option}
              type="button"
              disabled={disabled}
              onClick={() => onChange(option)}
              className={cn(
                'rounded-full border px-3 py-1.5 text-sm font-medium transition-colors',
                value === option
                  ? 'border-accent bg-accent/10 text-accent'
                  : 'border-border text-ink-muted hover:border-ink/20 hover:text-ink',
                disabled && 'cursor-not-allowed opacity-40 hover:border-border hover:text-ink-muted',
              )}
            >
              {VISIBILITY_LABELS[option]}
            </button>
          )
        })}
      </div>
    )
  }

  return (
    <div>
      <p className="text-sm font-medium text-ink">{label}</p>
      <div className="mt-2 flex flex-col gap-2">
        {options.map((option) => {
          const disabled = disabledOptions.includes(option)
          return (
            <button
              key={option}
              type="button"
              disabled={disabled}
              onClick={() => onChange(option)}
              className={cn(
                'rounded-xl border px-4 py-3 text-left transition-colors',
                value === option
                  ? 'border-accent bg-accent/5'
                  : 'border-border hover:border-ink/20',
                disabled && 'cursor-not-allowed opacity-40 hover:border-border',
              )}
            >
              <span className="text-sm font-medium text-ink">{VISIBILITY_LABELS[option]}</span>
              <p className="mt-0.5 text-xs text-ink-muted">
                {disabled && option === 'SECRET'
                  ? 'You already have a secret shelf'
                  : VISIBILITY_HINTS[option]}
              </p>
            </button>
          )
        })}
      </div>
    </div>
  )
}
