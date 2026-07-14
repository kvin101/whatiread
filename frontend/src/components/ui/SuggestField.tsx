import { Loader2 } from 'lucide-react'
import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { Input } from './Input'

type DropdownPosition = {
  top: number
  left: number
  width: number
  maxHeight: number
}

export function SuggestField<T>({
  value,
  onValueChange,
  suggestions,
  isFetching,
  enabled = true,
  placeholder,
  inputId,
  inputClassName,
  minLength = 2,
  loadingLabel = 'Searching…',
  emptyLabel = 'No matches',
  isError = false,
  errorLabel = 'Search failed — try again',
  getKey,
  renderItem,
  onSelect,
  autoFocus = false,
}: {
  value: string
  onValueChange: (value: string) => void
  suggestions: T[]
  isFetching?: boolean
  enabled?: boolean
  placeholder?: string
  inputId?: string
  inputClassName?: string
  minLength?: number
  loadingLabel?: string
  emptyLabel?: string
  isError?: boolean
  errorLabel?: string
  getKey: (item: T, index: number) => string
  renderItem: (item: T, highlighted: boolean) => ReactNode
  onSelect: (item: T) => void
  autoFocus?: boolean
}) {
  const [open, setOpen] = useState(false)
  const [highlight, setHighlight] = useState(0)
  const [dropdownPosition, setDropdownPosition] = useState<DropdownPosition | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const dropdownRef = useRef<HTMLUListElement>(null)

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
    const maxHeight = Math.max(220, Math.min(480, preferBelow ? spaceBelow : spaceAbove))

    setDropdownPosition({
      top: preferBelow ? rect.bottom + gap : rect.top - gap - maxHeight,
      left: rect.left,
      width: rect.width,
      maxHeight,
    })
  }, [])

  useEffect(() => {
    setHighlight(0)
  }, [value, suggestions])

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

  const showDropdown = enabled && open && value.trim().length >= minLength

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
                <Loader2 className="h-5 w-5 shrink-0 animate-spin" />
                {loadingLabel}
              </li>
            )}
            {!isFetching && isError && (
              <li className="px-4 py-4 text-base text-danger">{errorLabel}</li>
            )}
            {!isFetching && !isError && suggestions.length === 0 && (
              <li className="px-4 py-4 text-base text-ink-muted">{emptyLabel}</li>
            )}
            {suggestions.map((item, index) => (
              <li key={getKey(item, index)}>
                <button
                  type="button"
                  className={`flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-accent-dim ${
                    index === highlight ? 'bg-accent-dim' : ''
                  }`}
                  onMouseDown={(e) => {
                    e.preventDefault()
                    onSelect(item)
                    setOpen(false)
                  }}
                >
                  {renderItem(item, index === highlight)}
                </button>
              </li>
            ))}
          </ul>,
          document.body,
        )
      : null

  return (
    <div className="relative min-w-0 flex-1" ref={containerRef}>
      <Input
        id={inputId}
        placeholder={placeholder}
        value={value}
        autoFocus={autoFocus}
        className={inputClassName}
        onChange={(e) => {
          onValueChange(e.target.value)
          setOpen(true)
        }}
        onFocus={() => setOpen(true)}
        onKeyDown={(e) => {
          if (e.key === 'ArrowDown' && suggestions.length > 0) {
            e.preventDefault()
            setHighlight((current) => Math.min(current + 1, suggestions.length - 1))
            return
          }
          if (e.key === 'ArrowUp' && suggestions.length > 0) {
            e.preventDefault()
            setHighlight((current) => Math.max(current - 1, 0))
            return
          }
          if (e.key === 'Enter' && showDropdown && suggestions[highlight]) {
            e.preventDefault()
            onSelect(suggestions[highlight])
            setOpen(false)
            return
          }
          if (e.key === 'Escape') {
            setOpen(false)
          }
        }}
      />
      {dropdown}
    </div>
  )
}
