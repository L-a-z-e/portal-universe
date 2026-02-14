import React from 'react'

export const DeliveryPage: React.FC = () => {
  return (
    <div>
      <h1 className="text-2xl font-bold text-text-heading mb-6">Delivery Management</h1>
      <div className="bg-bg-card border border-border-default rounded-lg p-6 shadow-sm">
        <div className="p-8 text-center">
          <p className="text-lg text-text-heading mb-2">Cross-service integration required</p>
          <p className="text-sm text-text-meta">
            Delivery data is managed by shopping-service. Feign proxy integration is needed to display delivery information here.
          </p>
        </div>
      </div>
    </div>
  )
}

export default DeliveryPage
