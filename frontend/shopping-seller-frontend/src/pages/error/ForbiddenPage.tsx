import React from 'react'

const ForbiddenPage: React.FC = () => (
  <div className="min-h-[400px] flex items-center justify-center">
    <div className="text-center">
      <h1 className="text-4xl font-bold text-status-error mb-4">403</h1>
      <p className="text-text-body text-lg mb-2">Access Denied</p>
      <p className="text-text-meta">You don't have permission to access this page.</p>
    </div>
  </div>
)

export default ForbiddenPage
