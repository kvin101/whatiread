import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Flame } from 'lucide-react'
import { readingApi } from '../../api/reading'
import { QUERY_KEYS } from '../../lib/constants'
import { StreakShareModal } from './StreakShareModal'
import { cn } from '../../lib/utils'

export function StreakBadge({ className }: { className?: string }) {
  const [shareOpen, setShareOpen] = useState(false)
  const queryClient = useQueryClient()

  const { data: streak } = useQuery({
    queryKey: QUERY_KEYS.reading.streak,
    queryFn: readingApi.getStreak,
  })

  const current = streak?.currentStreak ?? 0

  return (
    <>
      <button
        type="button"
        onClick={() => setShareOpen(true)}
        className={cn(
          'inline-flex items-center gap-1 rounded-full bg-orange-500/15 px-3 py-1 text-sm font-semibold text-orange-300 transition-colors hover:bg-orange-500/25',
          className,
        )}
        title="Reading streak"
      >
        <Flame className="h-4 w-4" />
        {current}
      </button>
      <StreakShareModal
        open={shareOpen}
        onClose={() => setShareOpen(false)}
        streak={current}
        longest={streak?.longestStreak ?? 0}
        onShared={() => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.reading.streak })}
      />
    </>
  )
}
