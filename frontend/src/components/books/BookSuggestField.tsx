import { BookOpen } from 'lucide-react'
import type { BookSuggestResult } from '../../api/types'
import { useBookSuggest } from '../../hooks/useBookSuggest'
import { SuggestField } from '../ui/SuggestField'

export function BookSuggestField({
  value,
  onValueChange,
  onSelect,
  placeholder = 'Search books…',
  inputId,
  enabled = true,
  autoFocus = false,
}: {
  value: string
  onValueChange: (value: string) => void
  onSelect: (book: BookSuggestResult) => void
  placeholder?: string
  inputId?: string
  enabled?: boolean
  autoFocus?: boolean
}) {
  const { data: suggestions = [], isFetching, isError } = useBookSuggest(value, enabled)

  return (
    <SuggestField
      value={value}
      onValueChange={onValueChange}
      suggestions={suggestions}
      isFetching={isFetching}
      isError={isError}
      enabled={enabled}
      placeholder={placeholder}
      inputId={inputId}
      loadingLabel="Finding books…"
      emptyLabel="No matching books"
      autoFocus={autoFocus}
      getKey={(book) => book.title}
      onSelect={onSelect}
      renderItem={(book) => (
        <>
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-sage/15 text-sage">
            <BookOpen className="h-4 w-4" />
          </div>
          <div className="min-w-0">
            <p className="truncate text-base font-medium text-ink">{book.title}</p>
          </div>
        </>
      )}
    />
  )
}
