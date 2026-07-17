import { useEffect } from 'react'

const RIPPLE_LIFETIME_MS = 550

/** Global tap ripple at the pointer position. Respects prefers-reduced-motion. */
export function TapEffect() {
  useEffect(() => {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)')
    if (reducedMotion.matches) return

    const layer = document.createElement('div')
    layer.className = 'tap-effect-layer'
    layer.setAttribute('aria-hidden', 'true')
    document.body.appendChild(layer)

    const spawnRipple = (x: number, y: number) => {
      const ripple = document.createElement('span')
      ripple.className = 'tap-ripple'
      ripple.style.left = `${x}px`
      ripple.style.top = `${y}px`
      layer.appendChild(ripple)

      const remove = () => ripple.remove()
      ripple.addEventListener('animationend', remove, { once: true })
      window.setTimeout(remove, RIPPLE_LIFETIME_MS)
    }

    const onPointerDown = (event: PointerEvent) => {
      if (event.button !== 0) return
      spawnRipple(event.clientX, event.clientY)
    }

    document.addEventListener('pointerdown', onPointerDown, { passive: true })

    return () => {
      document.removeEventListener('pointerdown', onPointerDown)
      layer.remove()
    }
  }, [])

  return null
}
