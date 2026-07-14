import type { LucideIcon } from 'lucide-react'
import { cn } from '../../lib/utils'

export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  className,
}: {
  icon: LucideIcon
  title: string
  description?: string
  action?: React.ReactNode
  className?: string
}) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center rounded-xl border border-dashed border-white/10',
        'bg-white/[0.02] px-6 py-10 text-center animate-fade-in',
        className,
      )}
    >
      <div className="mb-3 rounded-xl bg-accent-dim p-3 ring-1 ring-accent/20">
        <Icon className="h-6 w-6 text-accent" strokeWidth={1.5} />
      </div>
      <h3 className="font-display text-base font-semibold text-ink">{title}</h3>
      {description && (
        <p className="mt-1.5 max-w-sm text-sm text-ink-muted">{description}</p>
      )}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
