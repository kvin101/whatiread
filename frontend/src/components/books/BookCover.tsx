import { BookOpen } from 'lucide-react'
import { cn } from '../../lib/utils'

export function BookCover({
  title,
  coverUrl,
  className,
  size = 'md',
}: {
  title: string
  coverUrl?: string
  className?: string
  size?: 'sm' | 'md' | 'lg'
}) {
  const sizes = {
    sm: 'h-24 w-16',
    md: 'h-36 w-24',
    lg: 'h-48 w-32',
  }

  return (
    <div
      className={cn(
        'relative shrink-0 overflow-hidden rounded-lg bg-ink/5 shadow-md ring-1 ring-ink/10',
        sizes[size],
        className,
      )}
    >
      {coverUrl ? (
        <img src={coverUrl} alt="" className="h-full w-full object-cover" loading="lazy" />
      ) : (
        <div className="flex h-full w-full flex-col items-center justify-center bg-gradient-to-br from-sage/20 to-accent/10 p-2 text-center">
          <BookOpen className="h-6 w-6 text-ink-muted/50" strokeWidth={1.5} />
          <span className="mt-1 line-clamp-3 text-[9px] font-medium leading-tight text-ink-muted">
            {title}
          </span>
        </div>
      )}
    </div>
  )
}
