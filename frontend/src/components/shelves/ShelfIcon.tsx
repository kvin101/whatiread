import { cn } from '../../lib/utils'
import { shelfIconComponent } from '../../lib/shelfIcons'

export function ShelfIcon({
  icon,
  className,
  size = 'md',
}: {
  icon?: string | null
  className?: string
  size?: 'sm' | 'md' | 'lg'
}) {
  const Icon = shelfIconComponent(icon)
  const sizes = {
    sm: 'h-4 w-4',
    md: 'h-5 w-5',
    lg: 'h-6 w-6',
  }

  return (
    <span
      className={cn(
        'flex shrink-0 items-center justify-center rounded-xl bg-accent-dim text-accent ring-1 ring-accent/20',
        size === 'sm' && 'h-9 w-9',
        size === 'md' && 'h-12 w-12',
        size === 'lg' && 'h-14 w-14',
        className,
      )}
    >
      <Icon className={sizes[size]} strokeWidth={1.75} />
    </span>
  )
}
