import { useMutation } from '@tanstack/react-query'
import { Camera, Trash2 } from 'lucide-react'
import { useRef, useState } from 'react'
import { accountApi } from '../../api/account'
import { useAuth } from '../../auth/AuthContext'
import { saveStoredAuth, loadStoredAuth } from '../../auth/storage'
import { getApiErrorMessage } from '../../lib/api'
import { displayName } from '../../lib/utils'
import { Button } from '../ui/Button'
import { UserAvatar } from '../ui/UserAvatar'

export function ProfileAvatarUpload({ compact = false }: { compact?: boolean }) {
  const { user, refreshUser } = useAuth()
  const inputRef = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)
  const name = user ? displayName(user) : ''

  const uploadMutation = useMutation({
    mutationFn: (file: File) => accountApi.uploadAvatar(file),
    onSuccess: async (updated) => {
      setError(null)
      const stored = loadStoredAuth()
      if (stored) saveStoredAuth({ ...stored, user: updated })
      await refreshUser()
    },
    onError: (e: Error) => setError(getApiErrorMessage(e, 'Could not upload photo')),
  })

  const removeMutation = useMutation({
    mutationFn: () => accountApi.removeAvatar(),
    onSuccess: async () => {
      setError(null)
      await refreshUser()
    },
    onError: (e: Error) => setError(getApiErrorMessage(e, 'Could not remove photo')),
  })

  const onFile = (file: File | undefined) => {
    if (!file) return
    if (!file.type.startsWith('image/')) {
      setError('Choose a JPEG, PNG, or WebP image.')
      return
    }
    if (file.size > 2 * 1024 * 1024) {
      setError('Image must be 2 MB or smaller.')
      return
    }
    uploadMutation.mutate(file)
  }

  const busy = uploadMutation.isPending || removeMutation.isPending

  if (compact) {
    return (
      <div className="flex flex-col items-center gap-2">
        <UserAvatar name={name} avatarUrl={user?.avatarUrl} size="xl" />
        <div className="flex flex-wrap justify-center gap-2">
          <Button
            type="button"
            variant="secondary"
            size="sm"
            disabled={busy}
            onClick={() => inputRef.current?.click()}
          >
            <Camera className="h-4 w-4" />
            Change
          </Button>
          {user?.avatarUrl && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={busy}
              onClick={() => removeMutation.mutate()}
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          )}
        </div>
        {error && <p className="text-xs text-danger text-center">{error}</p>}
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          className="sr-only"
          onChange={(e) => {
            onFile(e.target.files?.[0])
            e.target.value = ''
          }}
        />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
      <UserAvatar name={name} avatarUrl={user?.avatarUrl} size="xl" />
      <div className="space-y-3">
        <div>
          <p className="text-sm font-medium text-ink">Profile photo</p>
          <p className="text-sm text-ink-muted">JPEG, PNG, or WebP · max 2 MB</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="secondary"
            size="sm"
            disabled={busy}
            onClick={() => inputRef.current?.click()}
          >
            <Camera className="h-4 w-4" />
            Upload photo
          </Button>
          {user?.avatarUrl && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={busy}
              onClick={() => removeMutation.mutate()}
            >
              <Trash2 className="h-4 w-4" />
              Remove
            </Button>
          )}
        </div>
        {error && <p className="text-sm text-danger">{error}</p>}
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          className="sr-only"
          onChange={(e) => {
            onFile(e.target.files?.[0])
            e.target.value = ''
          }}
        />
      </div>
    </div>
  )
}
