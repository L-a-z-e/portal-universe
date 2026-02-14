import React, { useState, useEffect } from 'react'
import { dashboardApi } from '@/api'

const DashboardPage: React.FC = () => {
  const [stats, setStats] = useState<any>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      try {
        const res = await dashboardApi.getStats()
        setStats(res.data?.data || null)
      } catch (err) {
        console.error('Failed to load dashboard stats:', err)
      } finally {
        setIsLoading(false)
      }
    }
    load()
  }, [])

  const cards = [
    { label: 'Products', value: stats?.productCount ?? '-' },
    { label: 'Active Coupons', value: stats?.activeCouponCount ?? '-' },
    { label: 'Total Coupons', value: stats?.couponCount ?? '-' },
    { label: 'Active Time Deals', value: stats?.activeTimeDealCount ?? '-' },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-text-heading mb-6">Dashboard</h1>

      {isLoading ? (
        <p className="text-text-meta">Loading dashboard...</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {cards.map((card) => (
            <div key={card.label} className="bg-bg-card border border-border-default rounded-lg p-6 shadow-sm">
              <p className="text-sm text-text-meta mb-1">{card.label}</p>
              <p className="text-2xl font-bold text-text-heading">{card.value}</p>
            </div>
          ))}
        </div>
      )}

      <div className="mt-8 bg-bg-card border border-border-default rounded-lg p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-text-heading mb-4">Overview</h2>
        <p className="text-text-meta text-sm">
          Total time deals: {stats?.timeDealCount ?? '-'}
        </p>
      </div>
    </div>
  )
}

export default DashboardPage
