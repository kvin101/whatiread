import { Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext'
import { APP_ROUTES } from '../api/paths'
import { BookLoaderCenter } from '../components/ui/BookLoader'

export function AdminRoute({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth()

  if (isLoading) {
    return <BookLoaderCenter className="min-h-[40vh]" />
  }

  if (!user?.admin) {
    return <Navigate to={APP_ROUTES.library} replace />
  }

  return children
}
