import { useEffect, useMemo, useState, useCallback } from 'react'

export function useClientInfiniteScroll<T extends { id?: string }>(items: T[], pageSize = 20) {
  const [visibleCount, setVisibleCount] = useState(pageSize)
  const itemsKey = useMemo(
    () => items.map((item) => item.id ?? '').join(','),
    [items],
  )

  useEffect(() => {
    setVisibleCount(pageSize)
  }, [itemsKey, pageSize])

  const visibleItems = useMemo(() => items.slice(0, visibleCount), [items, visibleCount])
  const hasMore = visibleCount < items.length

  const loadMore = useCallback(() => {
    setVisibleCount((count) => Math.min(count + pageSize, items.length))
  }, [items.length, pageSize])

  return { visibleItems, hasMore, loadMore, total: items.length }
}
