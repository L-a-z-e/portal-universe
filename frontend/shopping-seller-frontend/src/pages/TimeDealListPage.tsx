import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { sellerTimeDealApi } from '@/api'
import { Button, Table } from '@portal/design-react'
import type { TableColumn } from '@portal/design-core'

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

  const columns: TableColumn<any>[] = [
    {
      key: 'name',
      label: 'Name',
      render: (value: unknown) => (
        <span className="font-medium">{value as string}</span>
      ),
    },
    {
      key: 'status',
      label: 'Status',
      render: (value: unknown) => {
        const status = value as string
        return (
          <span className={`px-2 py-1 rounded text-xs font-medium ${
            status === 'ACTIVE' ? 'bg-status-success-bg text-status-success'
              : status === 'SCHEDULED' ? 'bg-status-warning-bg text-status-warning'
              : 'bg-bg-subtle text-text-meta'
          }`}>
            {status}
          </span>
        )
      },
    },
    {
      key: 'startsAt',
      label: 'Start',
      render: (value: unknown) => (value as string)?.substring(0, 16) ?? '-',
    },
    {
      key: 'endsAt',
      label: 'End',
      render: (value: unknown) => (value as string)?.substring(0, 16) ?? '-',
    },
    {
      key: 'products',
      label: 'Products',
      render: (value: unknown) => String((value as any[])?.length ?? 0),
    },
    {
      key: 'id',
      label: 'Actions',
      align: 'right',
      render: (_value: unknown, row: unknown) => {
        const d = row as any
        return (d.status === 'SCHEDULED' || d.status === 'ACTIVE') ? (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => handleCancel(d.id)}
            className="text-status-error hover:bg-status-error-bg"
          >
            Cancel
          </Button>
        ) : null
      },
    },
  ]

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-text-heading">Time Deals</h1>
        <Button variant="primary" onClick={() => navigate('/time-deals/new')}>
          New Time Deal
        </Button>
      </div>

      <div className="bg-bg-card border border-border-default rounded-lg overflow-hidden shadow-sm">
        {!isLoading && deals.length === 0 ? (
          <div className="p-12 text-center">
            <p className="text-lg text-text-heading mb-2">No time deals found</p>
            <Button variant="primary" onClick={() => navigate('/time-deals/new')}>
              Create Time Deal
            </Button>
          </div>
        ) : (
          <Table
            columns={columns}
            data={deals}
            loading={isLoading}
            emptyText="No time deals found"
          />
        )}
      </div>
    </div>
  )
}

export default TimeDealListPage
