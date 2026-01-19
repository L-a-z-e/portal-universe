/**
 * ConfirmModal Component
 * 확인 모달 컴포넌트
 */
import React from 'react'
import type { ConfirmModalProps } from '@/types/admin'

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
  if (!isOpen) return null

  const iconColor = {
    danger: 'bg-status-error-bg text-status-error',
    warning: 'bg-status-warning-bg text-status-warning',
    default: 'bg-status-info-bg text-status-info'
  }[variant]

  const confirmButtonColor = {
    danger: 'bg-status-error hover:bg-status-error/90',
    warning: 'bg-status-warning hover:bg-status-warning/90',
    default: 'bg-brand-primary hover:bg-brand-primaryHover'
  }[variant]

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/50 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-bg-card rounded-lg shadow-xl max-w-md w-full p-6 animate-in zoom-in-95 duration-200">
        {/* Icon */}
        <div className={`w-12 h-12 rounded-full mx-auto mb-4 flex items-center justify-center ${iconColor}`}>
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </div>

        {/* Title */}
        <h3 className="text-lg font-bold text-text-heading text-center mb-2">
          {title}
        </h3>

        {/* Message */}
        <div className="text-sm text-text-meta text-center mb-6">
          {typeof message === 'string' ? <p>{message}</p> : message}
        </div>

        {/* Actions */}
        <div className="flex items-center gap-3">
          <button
            onClick={onCancel}
            disabled={loading}
            className="flex-1 px-4 py-3 rounded-lg font-medium bg-bg-subtle text-text-body hover:bg-bg-hover transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {cancelText}
          </button>
          <button
            onClick={onConfirm}
            disabled={loading}
            className={`flex-1 px-4 py-3 rounded-lg font-medium text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${confirmButtonColor}`}
          >
            {loading ? (
              <div className="flex items-center justify-center gap-2">
                <div className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" />
                <span>Loading...</span>
              </div>
            ) : (
              confirmText
            )}
          </button>
        </div>
      </div>
    </div>
  )
}

export default ConfirmModal
