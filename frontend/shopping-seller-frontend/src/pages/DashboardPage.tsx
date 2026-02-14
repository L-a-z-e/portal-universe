import React from 'react'

const DashboardPage: React.FC = () => {
  return (
    <div>
      <h1 className="text-2xl font-bold text-text-heading mb-6">Dashboard</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-bg-card border border-border-default rounded-lg p-6 shadow-sm">
          <p className="text-sm text-text-meta mb-1">Total Sales</p>
          <p className="text-2xl font-bold text-text-heading">-</p>
        </div>
        <div className="bg-bg-card border border-border-default rounded-lg p-6 shadow-sm">
          <p className="text-sm text-text-meta mb-1">Total Orders</p>
          <p className="text-2xl font-bold text-text-heading">-</p>
        </div>
        <div className="bg-bg-card border border-border-default rounded-lg p-6 shadow-sm">
          <p className="text-sm text-text-meta mb-1">Products</p>
          <p className="text-2xl font-bold text-text-heading">-</p>
        </div>
        <div className="bg-bg-card border border-border-default rounded-lg p-6 shadow-sm">
          <p className="text-sm text-text-meta mb-1">Pending Settlement</p>
          <p className="text-2xl font-bold text-text-heading">-</p>
        </div>
      </div>
      <div className="mt-8 bg-bg-card border border-border-default rounded-lg p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-text-heading mb-4">Recent Orders</h2>
        <p className="text-text-meta">No recent orders.</p>
      </div>
    </div>
  )
}

export default DashboardPage
