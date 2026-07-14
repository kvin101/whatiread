import type { QueryClient, QueryKey } from '@tanstack/react-query'

export function setQueryData<T>(
  queryClient: QueryClient,
  queryKey: QueryKey,
  updater: T | ((oldData: T | undefined) => T | undefined),
) {
  queryClient.setQueryData<T>(queryKey, updater)
}

export function updateArrayQueryData<T extends { id: string }>(
  queryClient: QueryClient,
  queryKey: QueryKey,
  updater: (items: T[]) => T[],
) {
  queryClient.setQueryData<T[]>(queryKey, (old) => {
    if (!old) return old
    return updater(old)
  })
}

export function removeArrayQueryItem(
  queryClient: QueryClient,
  queryKey: QueryKey,
  id: string,
) {
  updateArrayQueryData(queryClient, queryKey, (items) => items.filter((item) => item.id !== id))
}

export function appendArrayQueryItems<T extends { id: string }>(
  queryClient: QueryClient,
  queryKey: QueryKey,
  items: T[],
) {
  updateArrayQueryData(queryClient, queryKey, (current) => {
    const next = [...current]
    items.forEach((item) => {
      const existingIndex = next.findIndex((candidate) => candidate.id === item.id)
      if (existingIndex >= 0) {
        next[existingIndex] = item
      } else {
        next.push(item)
      }
    })
    return next
  })
}
