import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { sellerTimeDealApi } from '@/api'
import { Button } from '@portal/design-system-react'

export const TimeDealListPage: React.FC = () => {
  const navigate = useNavigate()
  const [deals, setDeals] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [page, _setPage] = useState(0)
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    const load = async () => {
      setIsLoading(true)
      try {
        const res = await sellerTimeDealApi.getTimeDeals({ page, size: 10 })
        const apiData = res.data?.data
        const items = apiData?.content ?? apiData?.items ?? (Array.isArray(apiData) ? apiData : [])
        setDeals(items)
      } catch (err) {
        console.error('Failed to load time deals:', err)
        setDeals([])
      } finally {
        setIsLoading(false)
      }
    }
    load()
  }, [page, refreshKey])

  const handleCancel = async (id: number) => {
    if (!confirm('Cancel this time deal?')) return
    try {
      await sellerTimeDealApi.deleteTimeDeal(id)
      setRefreshKey(prev => prev + 1)
    } catch (err) {
      console.error('Failed to cancel time deal:', err)
    }
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-text-heading">Time Deals</h1>
        <Button variant="primary" onClick={() => navigate('/time-deals/new')}>
          New Time Deal
        </Button>
      </div>

      <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden shadow-sm">
        {isLoading ? (
          <div className="p-12 text-center">
            <p className="text-text-meta">Loading...</p>
          </div>
        ) : deals.length === 0 ? (
          <div className="p-12 text-center">
            <p className="text-lg text-text-heading mb-2">No time deals found</p>
            <Button variant="primary" onClick={() => navigate('/time-deals/new')}>
              Create Time Deal
            </Button>
          </div>
        ) : (
          <table className="w-full">
            <thead className="bg-bg-subtle border-b border-border-default">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">Name</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">Status</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">Start</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">End</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">Products</th>
                <th className="px-6 py-4 text-right text-xs font-medium text-text-meta uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border-default">
              {deals.map((d: any) => (
                <tr key={d.id} className="hover:bg-bg-hover transition-colors">
                  <td className="px-6 py-4 text-sm text-text-body font-medium">{d.name}</td>
                  <td className="px-6 py-4 text-sm">
                    <span className={`px-2 py-1 rounded text-xs font-medium ${
                      d.status === 'ACTIVE' ? 'bg-status-success-bg text-status-success'
                        : d.status === 'SCHEDULED' ? 'bg-status-warning-bg text-status-warning'
                        : 'bg-bg-subtle text-text-meta'
                    }`}>
                      {d.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-text-body">{d.startsAt?.substring(0, 16)}</td>
                  <td className="px-6 py-4 text-sm text-text-body">{d.endsAt?.substring(0, 16)}</td>
                  <td className="px-6 py-4 text-sm text-text-body">{d.products?.length ?? 0}</td>
                  <td className="px-6 py-4 text-right">
                    {(d.status === 'SCHEDULED' || d.status === 'ACTIVE') && (
                      <button
                        onClick={() => handleCancel(d.id)}
                        className="p-2 text-status-error hover:bg-status-error-bg rounded transition-colors text-sm"
                      >
                        Cancel
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

export default TimeDealListPage
