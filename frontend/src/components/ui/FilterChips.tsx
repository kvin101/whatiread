import { cn } from '../../lib/utils'

export function FilterChips<T extends string>({
  options,
  value,
  onChange,
  label,
}: {
  options: { value: T; label: string }[]
  value: T
  onChange: (v: T) => void
  label?: string
}) {
  return (
    <div role="group" aria-label={label} className="flex flex-wrap gap-2">
      {options.map((opt) => (
        <button
          key={opt.value}
          type="button"
          onClick={() => onChange(opt.value)}
          aria-pressed={value === opt.value}
          className={cn(
            'rounded-full px-4 py-1.5 text-sm font-medium transition-all duration-300',
            value === opt.value
              ? 'bg-accent text-void font-semibold border border-accent/50 shadow-sm'
              : 'bg-white/5 text-ink-muted border border-white/8 hover:border-white/20 hover:text-ink',
          )}
        >
          {opt.label}
        </button>
      ))}
    </div>
  )
}
