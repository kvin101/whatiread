import { cn } from '../../lib/utils'
import { DEFAULT_SHELF_ICON, SHELF_ICON_OPTIONS } from '../../lib/shelfIcons'

export function IconPicker({
  value,
  onChange,
}: {
  value?: string
  onChange: (icon: string) => void
}) {
  const selected = value ?? DEFAULT_SHELF_ICON

  return (
    <div className="grid grid-cols-5 gap-2 sm:grid-cols-10">
      {SHELF_ICON_OPTIONS.map(({ id, label, Icon }) => (
        <button
          key={id}
          type="button"
          onClick={() => onChange(id)}
          className={cn(
            'flex h-10 w-10 items-center justify-center rounded-xl border transition-all',
            selected === id
              ? 'border-accent bg-accent/10 text-accent shadow-sm'
              : 'border-border bg-paper text-ink-muted hover:border-ink/20 hover:text-ink',
          )}
          aria-label={label}
          title={label}
        >
          <Icon className="h-4 w-4" strokeWidth={1.75} />
        </button>
      ))}
    </div>
  )
}
