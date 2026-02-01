/**
 * 403 Forbidden Page
 * 권한이 없는 경우 표시되는 페이지
 */
import React from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@portal/design-system-react'

export const ForbiddenPage: React.FC = () => {
  const navigate = useNavigate()

  return (
    <div className="min-h-[600px] flex items-center justify-center px-6">
      <div className="text-center max-w-md">
        {/* Icon */}
        <div className="w-20 h-20 mx-auto mb-6 bg-status-error-bg rounded-full flex items-center justify-center">
          <svg
            className="w-10 h-10 text-status-error"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
            />
          </svg>
        </div>

        {/* Title */}
        <h1 className="text-3xl font-bold text-text-heading mb-3">
          Access Denied
        </h1>

        {/* Message */}
        <p className="text-text-body mb-2">
          You don't have permission to access this page.
        </p>
        <p className="text-sm text-text-meta mb-8">
          This area is restricted to administrators only.
        </p>

        {/* Actions */}
        <div className="flex items-center justify-center gap-3">
          <Button
            variant="secondary"
            onClick={() => navigate(-1)}
          >
            Go Back
          </Button>
          <Button
            variant="primary"
            onClick={() => navigate('/')}
          >
            Go to Home
          </Button>
        </div>
      </div>
    </div>
  )
}

export default ForbiddenPage
