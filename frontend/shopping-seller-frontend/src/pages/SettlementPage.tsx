import React, { useState, useEffect, useCallback } from 'react'
import { settlementApi } from '@/api'

const SettlementPage: React.FC = () => {
  const [periods, setPeriods] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(true)

  const fetchPeriods = useCallback(async () => {
    setIsLoading(true)
    try {
      const response = await settlementApi.getPeriods({ periodType: 'DAILY' })
      setPeriods(response.data?.data || [])
    } catch (err) {
      console.error('Failed to load settlement periods:', err)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchPeriods()
  }, [fetchPeriods])

  return (
    <div>
      <h1 className="text-2xl font-bold text-text-heading mb-6">Settlement</h1>
      <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden shadow-sm">
        {isLoading ? (
          <div className="p-12 text-center">
            <p className="text-text-meta">Loading settlement data...</p>
          </div>
        ) : periods.length === 0 ? (
          <div className="p-12 text-center">
            <p className="text-lg text-text-heading mb-2">No settlement data</p>
            <p className="text-sm text-text-meta">Settlement data will appear after orders are processed.</p>
          </div>
        ) : (
          <table className="w-full">
            <thead className="bg-bg-subtle border-b border-border-default">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">Period</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">Type</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-text-meta uppercase">Status</th>
                <th className="px-6 py-4 text-right text-xs font-medium text-text-meta uppercase">Net Amount</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border-default">
              {periods.map((period: any) => (
                <tr key={period.id} className="hover:bg-bg-hover transition-colors">
                  <td className="px-6 py-4 text-sm text-text-body">{period.startDate} ~ {period.endDate}</td>
                  <td className="px-6 py-4 text-sm text-text-body">{period.periodType}</td>
                  <td className="px-6 py-4 text-sm text-text-body">{period.status}</td>
                  <td className="px-6 py-4 text-sm text-text-body text-right">-</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

export default SettlementPage
