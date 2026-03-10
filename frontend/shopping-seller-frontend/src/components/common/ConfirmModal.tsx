import React from 'react'
import { Modal, Button } from '@portal/design-react'

interface ConfirmModalProps {
  isOpen: boolean
  title: string
  message: string | React.ReactNode
  confirmText?: string
  cancelText?: string
  variant?: 'danger' | 'warning' | 'default'
  onConfirm: () => void | Promise<void>
  onCancel: () => void
  loading?: boolean
}

export const ConfirmModal: React.FC<ConfirmModalProps> = ({
  isOpen,
  title,
  message,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  variant = 'default',
  onConfirm,
  onCancel,
  loading = false
}) => {
  const confirmVariant = variant === 'danger' ? 'danger' : 'primary'

  return (
    <Modal open={isOpen} title={title} onClose={onCancel} size="sm">
      <div className="mb-6 text-sm">
        {typeof message === 'string' ? <p>{message}</p> : message}
      </div>
      <div className="flex justify-end gap-3 pt-4 border-t border-border-default">
        <Button variant="ghost" onClick={onCancel} disabled={loading}>
          {cancelText}
        </Button>
        <Button variant={confirmVariant} onClick={onConfirm} loading={loading}>
          {confirmText}
        </Button>
      </div>
    </Modal>
  )
}

export default ConfirmModal
