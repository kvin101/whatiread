import { cn } from '../../lib/utils'

export function BookLoader({ className }: { className?: string }) {
  return (
    <div
      className={cn('relative flex items-end justify-center gap-1', className)}
      role="status"
      aria-label="Loading books"
    >
      <div className="book-page-flip relative h-10 w-7 rounded-sm border-2 border-accent/40 bg-paper-elevated shadow-[3px_3px_0_rgba(230,57,70,0.3)]">
        <div className="absolute inset-0.5 rounded-sm bg-gradient-to-br from-accent/20 to-transparent halftone-overlay" />
      </div>
      <div
        className="h-8 w-6 rounded-sm border-2 border-white/10 bg-paper-warm opacity-60"
        style={{ animation: 'book-shimmer 1.4s ease-in-out infinite 0.2s' }}
      />
      <div
        className="h-6 w-5 rounded-sm border-2 border-white/10 bg-paper-warm opacity-40"
        style={{ animation: 'book-shimmer 1.4s ease-in-out infinite 0.4s' }}
      />
      <style>{`
        .book-page-flip {
          animation: page-flip 1.2s ease-in-out infinite;
          transform-origin: left center;
        }
        @media (prefers-reduced-motion: reduce) {
          .book-page-flip { animation: none; }
        }
      `}</style>
    </div>
  )
}

export function BookLoaderCenter({ className }: { className?: string }) {
  return (
    <div className={cn('flex justify-center py-12', className)}>
      <BookLoader />
    </div>
  )
}

export function BookSkeletonCard() {
  return (
    <div className="skeleton-card manga-panel p-4 flex gap-4">
      <div className="skeleton-cover w-16 shrink-0 rounded-lg" />
      <div className="flex-1 space-y-3 pt-1">
        <div className="skeleton-line w-3/4" />
        <div className="skeleton-line w-1/2" />
        <div className="skeleton-line w-1/3 mt-4" />
      </div>
    </div>
  )
}

export function BookSkeletonGrid({ count = 6, className }: { count?: number; className?: string }) {
  return (
    <div className={cn('grid gap-4 sm:grid-cols-2 xl:grid-cols-3', className)}>
      {Array.from({ length: count }, (_, i) => (
        <BookSkeletonCard key={i} />
      ))}
    </div>
  )
}
