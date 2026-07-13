import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Zap } from 'lucide-react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { ApiError } from '../api/client'
import { setupApi } from '../api/setup'
import { useAuth } from '../auth/AuthContext'
import { Button } from '../components/ui/Button'
import { BookLoaderCenter } from '../components/ui/BookLoader'
import { Input, Label } from '../components/ui/Input'
import { copy } from '../lib/copy'
import { QUERY_KEYS } from '../lib/constants'
import { APP_ROUTES } from '../api/paths'

export function SetupPage() {
  const { completeAuth } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [allowRegistration, setAllowRegistration] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const { data: setup, isLoading } = useQuery({
    queryKey: QUERY_KEYS.setup.required,
    queryFn: setupApi.required,
    staleTime: 0,
    refetchOnMount: 'always',
  })

  const mutation = useMutation({
    mutationFn: () =>
      setupApi.createAdmin({
        email,
        password,
        firstName,
        lastName: lastName || undefined,
        registrationEnabled: allowRegistration,
      }),
    onSuccess: (auth) => {
      completeAuth(auth)
      queryClient.setQueryData(QUERY_KEYS.setup.required, {
        setupRequired: false,
        registrationEnabled: allowRegistration,
      })
      navigate(APP_ROUTES.library, { replace: true })
    },
    onError: (e: Error) => {
      if (e instanceof ApiError && e.status === 409) {
        queryClient.invalidateQueries({ queryKey: QUERY_KEYS.setup.required })
        navigate(APP_ROUTES.login, { replace: true })
        return
      }
      setError(e.message)
    },
  })

  if (isLoading) return <BookLoaderCenter className="min-h-screen" />
  if (setup && !setup.setupRequired) {
    return <Navigate to={APP_ROUTES.login} replace />
  }

  return (
    <div className="min-h-screen min-h-[100dvh] flex items-center justify-center p-6 speed-lines">
      <div className="w-full max-w-md glass-strong manga-modal-panel rounded-2xl border border-white/10 p-8 shadow-2xl animate-slide-up halftone-overlay">
        <div className="glow-line w-full -mx-8 mb-6" style={{ width: 'calc(100% + 4rem)' }} />
        <div className="flex items-center gap-2 mb-6">
          <Zap className="h-7 w-7 text-accent" fill="currentColor" />
          <h1 className="font-display text-2xl font-bold text-ink manga-title">{copy.auth.setup.title}</h1>
        </div>
        <p className="text-sm text-ink-muted mb-6 leading-relaxed">
          {copy.auth.setup.subtitle}
        </p>
        <form
          className="space-y-4"
          onSubmit={(e) => {
            e.preventDefault()
            mutation.mutate()
          }}
        >
          <div>
            <Label>First name</Label>
            <Input value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
          </div>
          <div>
            <Label>Last name</Label>
            <Input value={lastName} onChange={(e) => setLastName(e.target.value)} />
          </div>
          <div>
            <Label>Email</Label>
            <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </div>
          <div>
            <Label>Password</Label>
            <Input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={8}
            />
          </div>
          <label className="flex items-center gap-2 text-sm text-ink-muted">
            <input
              type="checkbox"
              checked={allowRegistration}
              onChange={(e) => setAllowRegistration(e.target.checked)}
            />
            Allow others to register on this instance
          </label>
          {!allowRegistration && (
            <p className="text-sm text-ink-muted">{copy.auth.setup.registrationHint}</p>
          )}
          {error && <p className="text-sm text-danger">{error}</p>}
          <Button type="submit" className="w-full comic-cta-starburst" disabled={mutation.isPending}>
            {mutation.isPending ? 'Opening the doors…' : copy.auth.setup.submit}
          </Button>
        </form>
      </div>
    </div>
  )
}
