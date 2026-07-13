import { Star } from 'lucide-react'
import { cn } from '../../lib/utils'

export function StarRating({
  value,
  onChange,
  readonly = false,
  size = 'md',
}: {
  value?: number
  onChange?: (rating: number | undefined) => void
  readonly?: boolean
  size?: 'sm' | 'md'
}) {
  const starSize = size === 'sm' ? 'h-4 w-4' : 'h-6 w-6'

  const setHalf = (halfValue: number) => {
    if (readonly || !onChange) return
    if (value === halfValue) {
      onChange(undefined)
    } else {
      onChange(halfValue)
    }
  }

  return (
    <div className="flex items-center gap-1">
      {[1, 2, 3, 4, 5].map((star) => (
        <div key={star} className="relative flex">
          {!readonly && (
            <>
              <button
                type="button"
                className="absolute left-0 z-10 h-full w-1/2 cursor-pointer"
                aria-label={`${star - 0.5} stars`}
                onClick={() => setHalf(star - 0.5)}
              />
              <button
                type="button"
                className="absolute right-0 z-10 h-full w-1/2 cursor-pointer"
                aria-label={`${star} stars`}
                onClick={() => setHalf(star)}
              />
            </>
          )}
          <Star
            className={cn(
              starSize,
              value != null && value >= star - 0.5
                ? value >= star
                  ? 'fill-accent text-accent'
                  : 'fill-accent/50 text-accent'
                : 'fill-transparent text-border',
            )}
            strokeWidth={1.5}
          />
        </div>
      ))}
      {value != null && (
        <span className="ml-2 text-sm font-medium text-ink-muted">{value.toFixed(1)}</span>
      )}
    </div>
  )
}
