import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { AuthQuerySync } from './auth/AuthQuerySync'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { ChatProvider } from './chat/ChatProvider'
import { AppShell } from './components/layout/AppShell'
import { AdminRoute } from './auth/AdminRoute'
import { APP_ROUTES } from './api/paths'
import { ErrorBoundary } from './components/ui/ErrorBoundary'

const SetupPage = lazy(() => import('./pages/SetupPage').then((module) => ({ default: module.SetupPage })))
const LoginPage = lazy(() => import('./pages/LoginPage').then((module) => ({ default: module.LoginPage })))
const RegisterPage = lazy(() => import('./pages/RegisterPage').then((module) => ({ default: module.RegisterPage })))
const SharedShelfPage = lazy(() => import('./pages/SharedShelfPage').then((module) => ({ default: module.SharedShelfPage })))
const LibraryPage = lazy(() => import('./pages/LibraryPage').then((module) => ({ default: module.LibraryPage })))
const ShelvesPage = lazy(() => import('./pages/ShelvesPage').then((module) => ({ default: module.ShelvesPage })))
const ShelfDetailPage = lazy(() => import('./pages/ShelfDetailPage').then((module) => ({ default: module.ShelfDetailPage })))
const ExplorePage = lazy(() => import('./pages/ExplorePage').then((module) => ({ default: module.ExplorePage })))
const UserProfilePage = lazy(() => import('./pages/UserProfilePage').then((module) => ({ default: module.UserProfilePage })))
const FriendsPage = lazy(() => import('./pages/FriendsPage').then((module) => ({ default: module.FriendsPage })))
const MessagesPage = lazy(() => import('./pages/MessagesPage').then((module) => ({ default: module.MessagesPage })))
const RecommendationsPage = lazy(() => import('./pages/RecommendationsPage').then((module) => ({ default: module.RecommendationsPage })))
const SettingsPage = lazy(() => import('./pages/SettingsPage').then((module) => ({ default: module.SettingsPage })))
const AdminUsersPage = lazy(() => import('./pages/AdminUsersPage').then((module) => ({ default: module.AdminUsersPage })))

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AuthQuerySync />
        <Suspense
          fallback={
            <div className="flex min-h-screen items-center justify-center bg-void">
              <div className="h-12 w-12 animate-spin rounded-full border-2 border-accent/30 border-t-accent" />
            </div>
          }
        >
          <Routes>
            <Route path={APP_ROUTES.setup} element={<SetupPage />} />
            <Route path={APP_ROUTES.login} element={<LoginPage />} />
            <Route path={APP_ROUTES.register} element={<RegisterPage />} />
            <Route path="/share/shelf/:token" element={<SharedShelfPage />} />
            <Route
              element={
                <ErrorBoundary>
                  <ProtectedRoute>
                    <AppShell />
                  </ProtectedRoute>
                </ErrorBoundary>
              }
            >
              <Route index element={<Navigate to={APP_ROUTES.library} replace />} />
              <Route path="library" element={<LibraryPage />} />
              <Route path="shelves" element={<ShelvesPage />} />
              <Route path="shelves/:shelfId" element={<ShelfDetailPage />} />
              <Route path="explore" element={<ExplorePage />} />
              <Route path="users/:userId" element={<UserProfilePage />} />
              <Route path="friends" element={<FriendsPage />} />
              <Route
                path="messages"
                element={
                  <ChatProvider enabled>
                    <MessagesPage />
                  </ChatProvider>
                }
              />
              <Route path="recommendations" element={<RecommendationsPage />} />
              <Route path="settings" element={<SettingsPage />} />
              <Route
                path="admin/users"
                element={
                  <AdminRoute>
                    <AdminUsersPage />
                  </AdminRoute>
                }
              />
            </Route>
            <Route path="*" element={<Navigate to={APP_ROUTES.library} replace />} />
          </Routes>
        </Suspense>
      </AuthProvider>
    </BrowserRouter>
  )
}
