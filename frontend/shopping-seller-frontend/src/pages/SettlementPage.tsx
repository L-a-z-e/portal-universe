import React, { useState, useEffect, useCallback } from 'react'
import { settlementApi } from '@/api'
import { Table } from '@portal/design-react'
import type { TableColumn } from '@portal/design-core'

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

  const columns: TableColumn<any>[] = [
    {
      key: 'startDate',
      label: 'Period',
      render: (_value: unknown, row: unknown) => {
        const period = row as any
        return `${period.startDate} ~ ${period.endDate}`
      },
    },
    {
      key: 'periodType',
      label: 'Type',
    },
    {
      key: 'status',
      label: 'Status',
    },
    {
      key: 'netAmount',
      label: 'Net Amount',
      align: 'right',
      render: () => '-',
    },
  ]

  return (
    <div>
      <h1 className="text-2xl font-bold text-text-heading mb-6">Settlement</h1>
      <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden shadow-sm">
        {!isLoading && periods.length === 0 ? (
          <div className="p-12 text-center">
            <p className="text-lg text-text-heading mb-2">No settlement data</p>
            <p className="text-sm text-text-meta">Settlement data will appear after orders are processed.</p>
          </div>
        ) : (
          <Table
            columns={columns}
            data={periods}
            loading={isLoading}
            emptyText="No settlement data"
          />
        )}
      </div>
    </div>
  )
}

export default SettlementPage
