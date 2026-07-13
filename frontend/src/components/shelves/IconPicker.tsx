import { cn } from '../../lib/utils'
import { SHELF_EMOJI_ICONS } from '../../lib/constants'

export function IconPicker({
  value,
  onChange,
}: {
  value?: string
  onChange: (icon: string) => void
}) {
  return (
    <div className="flex flex-wrap gap-2">
      {SHELF_EMOJI_ICONS.map((emoji) => (
        <button
          key={emoji}
          type="button"
          onClick={() => onChange(emoji)}
          className={cn(
            'flex h-10 w-10 items-center justify-center rounded-xl border text-lg transition-all',
            value === emoji
              ? 'border-accent bg-accent/10 scale-110 shadow-sm'
              : 'border-border bg-paper hover:border-ink/20',
          )}
          aria-label={`Icon ${emoji}`}
        >
          {emoji}
        </button>
      ))}
    </div>
  )
}
