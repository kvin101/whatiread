import { useCallback, useRef, type ButtonHTMLAttributes, type ReactNode, type RefObject } from 'react'
import { cn } from '../../lib/utils'

/** Brief comic tap flash on an element. Respects prefers-reduced-motion. */
export function triggerComicBurst(el: HTMLElement | null) {
  if (!el) return
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  el.classList.remove('comic-burst-active')
  void el.offsetWidth
  el.classList.add('comic-burst-active')
  window.setTimeout(() => el.classList.remove('comic-burst-active'), 450)
}

export function useComicBurst(
  onClick?: ButtonHTMLAttributes<HTMLButtonElement>['onClick'],
  { disabled, burst = true }: { disabled?: boolean; burst?: boolean } = {},
) {
  const ref = useRef<HTMLButtonElement>(null)

  const handleClick = useCallback(
    (e: React.MouseEvent<HTMLButtonElement>) => {
      if (burst && !disabled) triggerComicBurst(ref.current)
      onClick?.(e)
    },
    [burst, disabled, onClick],
  )

  return { ref, handleClick }
}

/** Attach comic burst to any clickable element via ref + wrapped handler. */
export function useComicBurstOn<T extends HTMLElement>(
  ref: RefObject<T | null>,
  onClick?: React.MouseEventHandler<T>,
  { burst = true }: { burst?: boolean } = {},
) {
  return useCallback(
    (e: React.MouseEvent<T>) => {
      if (burst) triggerComicBurst(ref.current)
      onClick?.(e)
    },
    [burst, onClick, ref],
  )
}

type ComicBurstProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  children: ReactNode
  burst?: boolean
}

/** Wraps raw `<button>` elements with Valorant-style comic tap burst on click. */
export function ComicBurst({
  children,
  className,
  burst = true,
  onClick,
  disabled,
  ...props
}: ComicBurstProps) {
  const { ref, handleClick } = useComicBurst(onClick, { disabled, burst })

  return (
    <button
      ref={ref}
      className={cn('comic-btn', className)}
      onClick={handleClick}
      disabled={disabled}
      {...props}
    >
      {children}
    </button>
  )
}
