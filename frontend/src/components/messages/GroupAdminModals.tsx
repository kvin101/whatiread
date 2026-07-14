import type { Conversation } from '../../api/types'
import { Button } from '../ui/Button'
import { Input } from '../ui/Input'
import { Modal } from '../ui/Modal'

export function GroupAdminModals({
  active,
  renameOpen,
  renameDraft,
  onRenameDraftChange,
  onRenameClose,
  onRenameSave,
  renamePending,
  leaveOpen,
  onLeaveClose,
  onLeaveConfirm,
  leavePending,
}: {
  active: Conversation | undefined
  renameOpen: boolean
  renameDraft: string
  onRenameDraftChange: (value: string) => void
  onRenameClose: () => void
  onRenameSave: () => void
  renamePending?: boolean
  leaveOpen: boolean
  onLeaveClose: () => void
  onLeaveConfirm: () => void
  leavePending?: boolean
}) {
  if (!active || active.type !== 'GROUP') return null

  return (
    <>
      {renameOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <button type="button" className="absolute inset-0 bg-void/70" onClick={onRenameClose} aria-label="Close" />
          <div className="relative z-10 w-full max-w-md rounded-3xl border border-white/10 bg-paper p-6 shadow-xl">
            <h2 className="text-lg font-semibold text-ink mb-4">Rename group</h2>
            <Input value={renameDraft} onChange={(e) => onRenameDraftChange(e.target.value)} maxLength={255} autoFocus />
            <div className="mt-4 flex gap-2">
              <Button className="flex-1" onClick={onRenameSave} disabled={!renameDraft.trim() || renamePending}>
                Save
              </Button>
              <Button variant="secondary" className="flex-1" onClick={onRenameClose}>
                Cancel
              </Button>
            </div>
          </div>
        </div>
      )}

      <Modal open={leaveOpen} onClose={onLeaveClose} title="Leave group?">
        <p className="text-sm text-ink-muted mb-4">You will no longer receive messages from this group.</p>
        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button variant="secondary" onClick={onLeaveClose}>
            Cancel
          </Button>
          <Button variant="danger" disabled={leavePending} onClick={onLeaveConfirm}>
            Leave group
          </Button>
        </div>
      </Modal>
    </>
  )
}
