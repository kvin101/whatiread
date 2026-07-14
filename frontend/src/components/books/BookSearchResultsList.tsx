import { Check, Plus } from 'lucide-react'
import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import type { BookSearchResult } from '../../api/types'
import { formatAuthors } from '../../lib/utils'
import { BookCover } from './BookCover'
import { BookPreviewPopover } from './BookPreviewPopover'
import { Button } from '../ui/Button'

const HOVER_OPEN_DELAY_MS = 750
const HOVER_CLOSE_DELAY_MS = 300
const SWITCH_DELAY_MS = 400

export function bookSearchResultKey(book: BookSearchResult) {
  return book.id ?? `${book.title}-${book.externalId ?? book.isbn ?? ''}`
}

export function BookSearchResultsList({
  results,
  searching,
  query,
  emptyMessage,
  renderAction,
}: {
  results: BookSearchResult[]
  searching: boolean
  query: string
  emptyMessage: string
  renderAction: (book: BookSearchResult) => ReactNode
}) {
  const [previewBook, setPreviewBook] = useState<BookSearchResult | null>(null)
  const [previewAnchor, setPreviewAnchor] = useState<DOMRect | null>(null)
  const [previewKey, setPreviewKey] = useState<string | null>(null)
  const openTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const closeTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const rowRefs = useRef<Map<string, HTMLLIElement>>(new Map())
  const hoveredRowKey = useRef<string | null>(null)
  const popoverHovered = useRef(false)
  const pendingBook = useRef<BookSearchResult | null>(null)

  const clearOpenTimer = useCallback(() => {
    if (openTimer.current) {
      clearTimeout(openTimer.current)
      openTimer.current = null
    }
  }, [])

  const clearCloseTimer = useCallback(() => {
    if (closeTimer.current) {
      clearTimeout(closeTimer.current)
      closeTimer.current = null
    }
  }, [])

  const closePreview = useCallback(() => {
    clearOpenTimer()
    clearCloseTimer()
    pendingBook.current = null
    setPreviewBook(null)
    setPreviewAnchor(null)
    setPreviewKey(null)
  }, [clearCloseTimer, clearOpenTimer])

  const showPreview = useCallback((book: BookSearchResult) => {
    const key = bookSearchResultKey(book)
    const row = rowRefs.current.get(key)
    if (!row) {
      return
    }
    setPreviewBook(book)
    setPreviewAnchor(row.getBoundingClientRect())
    setPreviewKey(key)
    pendingBook.current = null
  }, [])

  const scheduleClose = useCallback(() => {
    clearCloseTimer()
    closeTimer.current = setTimeout(() => {
      if (!hoveredRowKey.current && !popoverHovered.current) {
        closePreview()
      }
      closeTimer.current = null
    }, HOVER_CLOSE_DELAY_MS)
  }, [clearCloseTimer, closePreview])

  const scheduleOpen = useCallback(
    (book: BookSearchResult, delayMs = HOVER_OPEN_DELAY_MS) => {
      const key = bookSearchResultKey(book)
      pendingBook.current = book
      clearOpenTimer()
      clearCloseTimer()
      setPreviewKey(key)

      openTimer.current = setTimeout(() => {
        if (hoveredRowKey.current !== key && !popoverHovered.current) {
          pendingBook.current = null
          openTimer.current = null
          return
        }
        showPreview(book)
        openTimer.current = null
      }, delayMs)
    },
    [clearCloseTimer, clearOpenTimer, showPreview],
  )

  const handleRowEnter = useCallback(
    (book: BookSearchResult) => {
      const key = bookSearchResultKey(book)
      hoveredRowKey.current = key
      clearCloseTimer()

      if (previewBook && previewKey === key) {
        return
      }

      if (previewBook && previewKey !== key) {
        closePreview()
        scheduleOpen(book, SWITCH_DELAY_MS)
        return
      }

      scheduleOpen(book)
    },
    [closePreview, previewBook, previewKey, scheduleOpen],
  )

  const handleRowLeave = useCallback(
    (book: BookSearchResult) => {
      const key = bookSearchResultKey(book)
      if (hoveredRowKey.current === key) {
        hoveredRowKey.current = null
      }
      clearOpenTimer()
      pendingBook.current = null
      scheduleClose()
    },
    [clearOpenTimer, scheduleClose],
  )

  const handlePopoverEnter = useCallback(() => {
    popoverHovered.current = true
    clearCloseTimer()
    clearOpenTimer()
  }, [clearCloseTimer, clearOpenTimer])

  const handlePopoverLeave = useCallback(() => {
    popoverHovered.current = false
    scheduleClose()
  }, [scheduleClose])

  useEffect(() => {
    closePreview()
  }, [query, closePreview])

  useEffect(() => {
    return () => {
      clearOpenTimer()
      clearCloseTimer()
    }
  }, [clearCloseTimer, clearOpenTimer])

  return (
    <>
      <ul className="mt-6 space-y-3 max-h-96 overflow-y-auto">
        {results.map((book) => {
          const key = bookSearchResultKey(book)
          const isPreviewTarget = previewKey === key && !!previewBook

          return (
            <li
              key={key}
              ref={(node) => {
                if (node) {
                  rowRefs.current.set(key, node)
                } else {
                  rowRefs.current.delete(key)
                }
              }}
              className={`rounded-2xl border transition-colors ${
                isPreviewTarget ? 'border-accent/30 bg-accent-dim/10' : 'border-border'
              }`}
              onMouseEnter={() => handleRowEnter(book)}
              onMouseLeave={() => handleRowLeave(book)}
            >
              <div className="flex items-center gap-3 p-4">
                <BookCover title={book.title} coverUrl={book.coverUrl} size="sm" />
                <div className="min-w-0 flex-1">
                  <p className="text-base font-medium text-ink line-clamp-2 sm:text-lg">{book.title}</p>
                  <p className="mt-1 text-sm text-ink-muted sm:text-base">{formatAuthors(book.authors)}</p>
                </div>
                <div>{renderAction(book)}</div>
              </div>
            </li>
          )
        })}
        {!searching && results.length === 0 && query && (
          <p className="text-center text-sm text-ink-muted py-8">{emptyMessage}</p>
        )}
      </ul>

      <BookPreviewPopover
        book={previewBook}
        anchorRect={previewAnchor}
        open={!!previewBook}
        onClose={closePreview}
        onMouseEnter={handlePopoverEnter}
        onMouseLeave={handlePopoverLeave}
      />
    </>
  )
}

export function BookSearchAddButton({
  onAdd,
  pending,
  added,
}: {
  onAdd: () => void
  pending: boolean
  added?: boolean
}) {
  if (added) {
    return (
      <Button size="md" variant="secondary" disabled>
        <Check className="h-4 w-4" />
        Added
      </Button>
    )
  }

  return (
    <Button size="md" onClick={onAdd} disabled={pending}>
      <Plus className="h-4 w-4" />
      Add
    </Button>
  )
}
