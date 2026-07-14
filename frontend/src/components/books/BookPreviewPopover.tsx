import { Loader2, X } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import type { BookSearchResult } from '../../api/types'
import { useBookPreview } from '../../hooks/useBookPreview'
import { formatAuthors } from '../../lib/utils'
import { BookPreviewPanel } from './BookPreviewPanel'

type Placement = 'left' | 'right'

type PopoverPosition = {
  top: number
  left: number
  width: number
  maxHeight: number
  placement: Placement
  bridgeTop: number
  bridgeLeft: number
  bridgeWidth: number
  bridgeHeight: number
  tailOffset: number
}

const POPOVER_WIDTH = 360
const VIEWPORT_PADDING = 16
const GAP = 12

function rowAlignedTop(anchor: DOMRect, maxHeight: number) {
  return Math.max(
    VIEWPORT_PADDING,
    Math.min(anchor.top, window.innerHeight - VIEWPORT_PADDING - maxHeight),
  )
}

function tailOffset(anchor: DOMRect, top: number, maxHeight: number) {
  return Math.min(
    Math.max(anchor.top + anchor.height / 2 - top - 8, 20),
    maxHeight - 28,
  )
}

function computePosition(anchor: DOMRect): PopoverPosition {
  const spaceLeft = anchor.left - GAP - VIEWPORT_PADDING
  const spaceRight = window.innerWidth - anchor.right - GAP - VIEWPORT_PADDING
  const maxHeight = Math.min(480, window.innerHeight - VIEWPORT_PADDING * 2)
  const top = rowAlignedTop(anchor, maxHeight)
  const resolvedMaxHeight = Math.min(maxHeight, window.innerHeight - top - VIEWPORT_PADDING)
  const offset = tailOffset(anchor, top, resolvedMaxHeight)

  if (spaceLeft >= POPOVER_WIDTH) {
    const left = anchor.left - POPOVER_WIDTH - GAP
    return {
      top,
      left,
      width: POPOVER_WIDTH,
      maxHeight: resolvedMaxHeight,
      placement: 'left',
      bridgeTop: Math.min(anchor.top, top),
      bridgeLeft: left + POPOVER_WIDTH,
      bridgeWidth: Math.max(anchor.left - (left + POPOVER_WIDTH), GAP),
      bridgeHeight: Math.max(anchor.height, resolvedMaxHeight),
      tailOffset: offset,
    }
  }

  if (spaceRight >= POPOVER_WIDTH) {
    const left = anchor.right + GAP
    return {
      top,
      left,
      width: POPOVER_WIDTH,
      maxHeight: resolvedMaxHeight,
      placement: 'right',
      bridgeTop: Math.min(anchor.top, top),
      bridgeLeft: anchor.right,
      bridgeWidth: Math.max(left - anchor.right, GAP),
      bridgeHeight: Math.max(anchor.height, resolvedMaxHeight),
      tailOffset: offset,
    }
  }

  const width = Math.max(280, Math.min(POPOVER_WIDTH, spaceLeft))
  const left = Math.max(VIEWPORT_PADDING, anchor.left - width - GAP)
  return {
    top,
    left,
    width,
    maxHeight: resolvedMaxHeight,
    placement: 'left',
    bridgeTop: Math.min(anchor.top, top),
    bridgeLeft: left + width,
    bridgeWidth: Math.max(anchor.left - (left + width), GAP),
    bridgeHeight: Math.max(anchor.height, resolvedMaxHeight),
    tailOffset: offset,
  }
}

export function BookPreviewPopover({
  book,
  anchorRect,
  open,
  onClose,
  onMouseEnter,
  onMouseLeave,
}: {
  book: BookSearchResult | null
  anchorRect: DOMRect | null
  open: boolean
  onClose: () => void
  onMouseEnter?: () => void
  onMouseLeave?: () => void
}) {
  const [position, setPosition] = useState<PopoverPosition | null>(null)
  const { data: preview, isFetching, isError } = useBookPreview(book, open && !!book)

  const updatePosition = useCallback(() => {
    if (!anchorRect) {
      return
    }
    setPosition(computePosition(anchorRect))
  }, [anchorRect])

  useEffect(() => {
    if (!open) {
      setPosition(null)
      return
    }
    updatePosition()
    const handleLayout = () => updatePosition()
    window.addEventListener('resize', handleLayout)
    window.addEventListener('scroll', handleLayout, true)
    return () => {
      window.removeEventListener('resize', handleLayout)
      window.removeEventListener('scroll', handleLayout, true)
    }
  }, [open, updatePosition])

  useEffect(() => {
    if (!open) {
      return
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose()
      }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open || !book || !position) {
    return null
  }

  const handleEnter = () => onMouseEnter?.()
  const handleLeave = () => onMouseLeave?.()

  const tailClass =
    position.placement === 'left'
      ? 'absolute -right-2 top-0 h-4 w-4 rotate-45 border-t border-r border-white/15 bg-surface'
      : 'absolute -left-2 top-0 h-4 w-4 rotate-45 border-b border-l border-white/15 bg-surface'

  return createPortal(
    <>
      <div
        aria-hidden
        className="fixed z-[139]"
        style={{
          top: position.bridgeTop,
          left: position.bridgeLeft,
          width: position.bridgeWidth,
          height: position.bridgeHeight,
        }}
        onMouseEnter={handleEnter}
        onMouseLeave={handleLeave}
      />
      <div
        role="dialog"
        aria-label={`Preview: ${book.title}`}
        className="fixed z-[140] overflow-visible rounded-2xl border border-white/15 bg-surface shadow-2xl animate-slide-up"
        style={{
          top: position.top,
          left: position.left,
          width: position.width,
          maxHeight: position.maxHeight,
        }}
        onMouseEnter={handleEnter}
        onMouseLeave={handleLeave}
      >
        <div className={tailClass} style={{ top: position.tailOffset }} />
        <div className="overflow-hidden rounded-2xl">
          <div className="flex items-start justify-between gap-3 border-b border-border/60 px-4 py-3">
            <div className="min-w-0">
              <p className="truncate font-display text-base font-bold text-ink">{book.title}</p>
              <p className="truncate text-sm text-ink-muted">{formatAuthors(book.authors)}</p>
            </div>
            <button
              type="button"
              className="shrink-0 rounded-lg p-1.5 text-ink-muted transition-colors hover:bg-accent-dim hover:text-ink"
              aria-label="Close preview"
              onClick={onClose}
            >
              <X className="h-5 w-5" />
            </button>
          </div>
          <div
            className="overflow-y-auto overscroll-contain p-4"
            style={{ maxHeight: position.maxHeight - 72 }}
          >
            {isFetching && !preview ? (
              <div className="flex items-center justify-center gap-3 py-10 text-sm text-ink-muted">
                <Loader2 className="h-5 w-5 animate-spin text-accent" />
                Loading preview…
              </div>
            ) : (
              <BookPreviewPanel preview={preview} loading={false} error={isError} compact />
            )}
          </div>
        </div>
      </div>
    </>,
    document.body,
  )
}
