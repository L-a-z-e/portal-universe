import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { sellerTimeDealApi } from '@/api'
import { Button } from '@portal/design-system-react'

export const TimeDealListPage: React.FC = () => {
  const [deals, setDeals] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      try {
        const res = await sellerTimeDealApi.getTimeDeals({ page: 1, size: 10 })
        setDeals(res.data?.items || [])
      } catch (err) {
        console.error(err)
      } finally {
        setIsLoading(false)
      }
    }
    load()
  }, [])

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-text-heading">Time Deals</h1>
        <Link to="/time-deals/new">
          <Button variant="primary">New Time Deal</Button>
        </Link>
      </div>

      <div className="bg-bg-card border border-border-default rounded-lg p-6">
        {isLoading ? (
          <p className="text-text-meta">Loading...</p>
        ) : deals.length === 0 ? (
          <p className="text-text-meta">No time deals found</p>
        ) : (
          <div className="space-y-2">
            {deals.map((d: any) => (
              <div key={d.id} className="p-3 border border-border-default rounded">
                {d.name}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default TimeDealListPage
