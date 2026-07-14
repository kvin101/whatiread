import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { resetBodyScroll } from '../../lib/bodyScrollLock'

export function PageTransition() {
  const location = useLocation()

  useEffect(() => {
    resetBodyScroll()
  }, [location.pathname])

  return (
    <div key={location.pathname} className="page-enter flex min-h-0 flex-1 flex-col">
      <Outlet />
    </div>
  )
}
