import { BookOpen, Loader2, Search } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import type { BookSuggestResult } from '../../api/types'
import { useBookSuggest } from '../../hooks/useBookSuggest'
import { Button } from '../ui/Button'
import { Input } from '../ui/Input'

type DropdownPosition = {
  top: number
  left: number
  width: number
  maxHeight: number
}

export function BookSearchField({
  query,
  onQueryChange,
  onSearch,
  searching,
  enabled = true,
  autoFocus = false,
}: {
  query: string
  onQueryChange: (value: string) => void
  onSearch: (nextQuery?: string) => void
  searching: boolean
  enabled?: boolean
  autoFocus?: boolean
}) {
  const [open, setOpen] = useState(false)
  const [highlight, setHighlight] = useState(0)
  const [dropdownPosition, setDropdownPosition] = useState<DropdownPosition | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const dropdownRef = useRef<HTMLUListElement>(null)
  const { data: suggestions = [], isFetching } = useBookSuggest(query, enabled && open)

  const updateDropdownPosition = useCallback(() => {
    const anchor = containerRef.current
    if (!anchor) {
      return
    }
    const rect = anchor.getBoundingClientRect()
    const gap = 8
    const viewportPadding = 16
    const spaceBelow = window.innerHeight - rect.bottom - gap - viewportPadding
    const spaceAbove = rect.top - gap - viewportPadding
    const preferBelow = spaceBelow >= 180 || spaceBelow >= spaceAbove
    const maxHeight = Math.max(
      220,
      Math.min(480, preferBelow ? spaceBelow : spaceAbove),
    )

    setDropdownPosition({
      top: preferBelow ? rect.bottom + gap : rect.top - gap - maxHeight,
      left: rect.left,
      width: rect.width,
      maxHeight,
    })
  }, [])

  useEffect(() => {
    setHighlight(0)
  }, [query, suggestions])

  useEffect(() => {
    if (!open) {
      setDropdownPosition(null)
      return
    }
    updateDropdownPosition()
    const handleLayout = () => updateDropdownPosition()
    window.addEventListener('resize', handleLayout)
    window.addEventListener('scroll', handleLayout, true)
    return () => {
      window.removeEventListener('resize', handleLayout)
      window.removeEventListener('scroll', handleLayout, true)
    }
  }, [open, suggestions, updateDropdownPosition])

  useEffect(() => {
    if (!open) {
      return
    }
    const handlePointerDown = (event: MouseEvent) => {
      const target = event.target as Node
      if (containerRef.current?.contains(target) || dropdownRef.current?.contains(target)) {
        return
      }
      setOpen(false)
    }
    document.addEventListener('mousedown', handlePointerDown)
    return () => document.removeEventListener('mousedown', handlePointerDown)
  }, [open])

  useEffect(() => {
    const handleVisibility = () => setOpen(false)
    document.addEventListener('visibilitychange', handleVisibility)
    return () => document.removeEventListener('visibilitychange', handleVisibility)
  }, [])

  const showDropdown = open && query.trim().length >= 2

  const selectSuggestion = (suggestion: BookSuggestResult) => {
    onQueryChange(suggestion.title)
    setOpen(false)
    onSearch(suggestion.title)
  }

  const dropdown =
    showDropdown && dropdownPosition
      ? createPortal(
          <ul
            ref={dropdownRef}
            style={{
              position: 'fixed',
              top: dropdownPosition.top,
              left: dropdownPosition.left,
              width: dropdownPosition.width,
              maxHeight: dropdownPosition.maxHeight,
            }}
            className="z-[120] overflow-y-auto rounded-2xl border border-white/15 bg-surface shadow-2xl backdrop-blur-md"
          >
            {isFetching && suggestions.length === 0 && (
              <li className="flex items-center gap-3 px-4 py-4 text-base text-ink-muted">
                <Loader2 className="h-5 w-5 animate-spin shrink-0" />
                Finding titles…
              </li>
            )}
            {!isFetching && suggestions.length === 0 && (
              <li className="px-4 py-4 text-base text-ink-muted">No matching titles</li>
            )}
            {suggestions.map((suggestion, index) => (
              <li key={`${suggestion.title}-${index}`}>
                <button
                  type="button"
                  className={`flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-accent-dim ${
                    index === highlight ? 'bg-accent-dim' : ''
                  }`}
                  onMouseDown={(e) => {
                    e.preventDefault()
                    selectSuggestion(suggestion)
                  }}
                >
                  <BookOpen className="h-5 w-5 shrink-0 text-accent" />
                  <span className="text-base font-medium text-ink sm:text-lg">{suggestion.title}</span>
                </button>
              </li>
            ))}
          </ul>,
          document.body,
        )
      : null

  return (
    <>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-stretch">
        <div className="relative min-w-0 flex-1" ref={containerRef}>
          <Input
            placeholder="Start typing a book title…"
            value={query}
            autoFocus={autoFocus}
            className="py-3.5 text-base sm:text-lg"
            onChange={(e) => {
              onQueryChange(e.target.value)
              setOpen(true)
            }}
            onKeyDown={(e) => {
              if (e.key === 'ArrowDown' && showDropdown && suggestions.length > 0) {
                e.preventDefault()
                setHighlight((value) => Math.min(value + 1, suggestions.length - 1))
                return
              }
              if (e.key === 'ArrowUp' && showDropdown && suggestions.length > 0) {
                e.preventDefault()
                setHighlight((value) => Math.max(value - 1, 0))
                return
              }
              if (e.key === 'Enter') {
                e.preventDefault()
                setOpen(false)
                onSearch()
                return
              }
              if (e.key === 'Escape') {
                setOpen(false)
              }
            }}
          />
        </div>
        <Button
          size="lg"
          className="shrink-0 px-6"
          onClick={() => {
            setOpen(false)
            onSearch()
          }}
          disabled={searching}
        >
          {searching ? <Loader2 className="h-5 w-5 animate-spin" /> : <Search className="h-5 w-5" />}
          Search
        </Button>
      </div>
      {dropdown}
    </>
  )
}
