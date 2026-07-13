import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { ChatProvider } from './chat/ChatProvider'
import { AppShell } from './components/layout/AppShell'
import { FriendsPage } from './pages/FriendsPage'
import { LibraryPage } from './pages/LibraryPage'
import { LoginPage } from './pages/LoginPage'
import { MessagesPage } from './pages/MessagesPage'
import { RecommendationsPage } from './pages/RecommendationsPage'
import { RegisterPage } from './pages/RegisterPage'
import { ExplorePage } from './pages/ExplorePage'
import { ShelfDetailPage } from './pages/ShelfDetailPage'
import { ShelvesPage } from './pages/ShelvesPage'
import { UserProfilePage } from './pages/UserProfilePage'
import { SetupPage } from './pages/SetupPage'
import { SettingsPage } from './pages/SettingsPage'
import { SharedShelfPage } from './pages/SharedShelfPage'
import { AdminUsersPage } from './pages/AdminUsersPage'
import { AdminRoute } from './auth/AdminRoute'
import { APP_ROUTES } from './api/paths'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path={APP_ROUTES.setup} element={<SetupPage />} />
          <Route path={APP_ROUTES.login} element={<LoginPage />} />
          <Route path={APP_ROUTES.register} element={<RegisterPage />} />
          <Route path="/share/shelf/:token" element={<SharedShelfPage />} />
          <Route
            element={
              <ProtectedRoute>
                <ChatProvider enabled>
                  <AppShell />
                </ChatProvider>
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to={APP_ROUTES.library} replace />} />
            <Route path="library" element={<LibraryPage />} />
            <Route path="shelves" element={<ShelvesPage />} />
            <Route path="shelves/:shelfId" element={<ShelfDetailPage />} />
            <Route path="explore" element={<ExplorePage />} />
            <Route path="users/:userId" element={<UserProfilePage />} />
            <Route path="friends" element={<FriendsPage />} />
            <Route path="messages" element={<MessagesPage />} />
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
      </AuthProvider>
    </BrowserRouter>
  )
}
