import { cn } from '../../lib/utils'

export function Spinner({ className }: { className?: string }) {
  return (
    <div
      className={cn(
        'h-10 w-10 animate-spin rounded-full border-2 border-accent/30 border-t-accent',
        className,
      )}
      role="status"
      aria-label="Loading"
    />
  )
}

export function LoadingCenter({ className }: { className?: string }) {
  return (
    <div className={cn('flex justify-center py-12', className)}>
      <Spinner />
    </div>
  )
}
