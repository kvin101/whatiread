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
        'flex flex-col items-center justify-center rounded-2xl border border-dashed border-white/10',
        'bg-white/[0.02] px-8 py-16 text-center animate-fade-in',
        className,
      )}
    >
      <div className="mb-5 rounded-2xl bg-accent-dim p-4 ring-1 ring-accent/20">
        <Icon className="h-8 w-8 text-accent" strokeWidth={1.5} />
      </div>
      <h3 className="font-display text-lg font-bold text-ink">{title}</h3>
      {description && (
        <p className="mt-2 max-w-md text-sm text-ink-muted leading-relaxed">{description}</p>
      )}
      {action && <div className="mt-6">{action}</div>}
    </div>
  )
}
