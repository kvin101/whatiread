let lockCount = 0

export function lockBodyScroll() {
  lockCount += 1
  document.body.style.overflow = 'hidden'
}

export function unlockBodyScroll() {
  lockCount = Math.max(0, lockCount - 1)
  if (lockCount === 0) {
    document.body.style.overflow = ''
  }
}

/** Safety net when route changes while a modal/drawer was open. */
export function resetBodyScroll() {
  lockCount = 0
  document.body.style.overflow = ''
}
