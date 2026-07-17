import { useEffect, useRef } from 'react'

export function InfiniteScrollSentinel({
  onIntersect,
  disabled = false,
}: {
  onIntersect: () => void
  disabled?: boolean
}) {
  const ref = useRef<HTMLDivElement>(null)
  const onIntersectRef = useRef(onIntersect)
  onIntersectRef.current = onIntersect

  useEffect(() => {
    if (disabled) return
    const element = ref.current
    if (!element) return

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          onIntersectRef.current()
        }
      },
      { rootMargin: '240px' },
    )

    observer.observe(element)
    return () => observer.disconnect()
  }, [disabled])

  return <div ref={ref} className="h-px w-full" aria-hidden />
}
