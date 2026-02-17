/**
 * Admin Delivery Page
 * 관리자 배송 조회 및 상태 변경 페이지
 */
import React, { useState } from 'react'
import { useTrackDelivery, useUpdateDeliveryStatus } from '@/hooks/useAdminDelivery'
import { DELIVERY_STATUS_LABELS } from '@/types'
import type { DeliveryStatus, Delivery } from '@/types'
import { Button, Spinner, Input, Alert, Select } from '@portal/design-react'
import type { SelectOption } from '@portal/design-core'

const DELIVERY_STATUS_OPTIONS: DeliveryStatus[] = [
  'PENDING', 'PREPARING', 'SHIPPED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED'
]

const getDeliveryStatusColor = (status: DeliveryStatus): string => {
  switch (status) {
    case 'DELIVERED': return 'text-status-success bg-status-success/10'
    case 'SHIPPED':
    case 'IN_TRANSIT':
    case 'OUT_FOR_DELIVERY': return 'text-brand-primary bg-brand-primary/10'
    case 'PENDING':
    case 'PREPARING': return 'text-status-warning bg-status-warning/10'
    case 'FAILED': return 'text-status-error bg-status-error/10'
    default: return 'text-text-meta bg-bg-subtle'
  }
}

const AdminDeliveryPage: React.FC = () => {
  const [searchInput, setSearchInput] = useState('')
  const { data: delivery, isLoading, error, track } = useTrackDelivery()
  const updateStatusMutation = useUpdateDeliveryStatus()
  const [actionError, setActionError] = useState<string | null>(null)
  const [newStatus, setNewStatus] = useState<DeliveryStatus>('SHIPPED')
  const [location, setLocation] = useState('')
  const [description, setDescription] = useState('')

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!searchInput.trim()) return
    setActionError(null)
    try {
      await track(searchInput.trim())
    } catch {
      // error handled by hook
    }
  }

  const handleUpdateStatus = async () => {
    if (!delivery) return
    setActionError(null)
    try {
      await updateStatusMutation.mutateAsync(delivery.trackingNumber, {
        status: newStatus,
        location: location || undefined,
        description: description || undefined
      })
      await track(delivery.trackingNumber)
      setLocation('')
      setDescription('')
    } catch (e) {
      setActionError(e instanceof Error ? e.message : 'Failed to update status')
    }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-text-heading">Delivery Management</h1>

      {/* Search */}
      <form onSubmit={handleSearch} className="flex items-center gap-2">
        <Input
          type="text"
          value={searchInput}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchInput(e.target.value)}
          placeholder="Enter tracking number or order number..."
        />
        <Button type="submit" variant="primary" disabled={isLoading}>
          {isLoading ? 'Searching...' : 'Search'}
        </Button>
      </form>

      {error && <Alert variant="error">{error.message}</Alert>}
      {actionError && <Alert variant="error">{actionError}</Alert>}

      {/* Delivery Detail */}
      {delivery && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Info */}
          <div className="bg-bg-card border border-border-default rounded-lg p-5 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-text-heading">Delivery Info</h2>
              <span className={`inline-flex px-2.5 py-1 text-xs font-medium rounded-full ${getDeliveryStatusColor(delivery.status)}`}>
                {DELIVERY_STATUS_LABELS[delivery.status]}
              </span>
            </div>
            <div className="text-sm space-y-2">
              <div className="flex justify-between">
                <span className="text-text-meta">Order</span>
                <span className="font-mono text-text-body">{delivery.orderNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-text-meta">Tracking</span>
                <span className="font-mono text-text-body">{delivery.trackingNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-text-meta">Carrier</span>
                <span className="text-text-body">{delivery.carrier}</span>
              </div>
              {delivery.estimatedDeliveryDate && (
                <div className="flex justify-between">
                  <span className="text-text-meta">ETA</span>
                  <span className="text-text-body">{new Date(delivery.estimatedDeliveryDate).toLocaleDateString('ko-KR')}</span>
                </div>
              )}
            </div>

            {/* Status Update */}
            <div className="pt-4 border-t border-border-default space-y-3">
              <h3 className="text-sm font-medium text-text-heading">Update Status</h3>
              <Select
                value={newStatus}
                options={DELIVERY_STATUS_OPTIONS.map((s): SelectOption => ({
                  value: s,
                  label: DELIVERY_STATUS_LABELS[s],
                }))}
                onChange={(value) => setNewStatus(value as DeliveryStatus)}
              />
              <Input
                type="text"
                value={location}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setLocation(e.target.value)}
                placeholder="Location (optional)"
              />
              <Input
                type="text"
                value={description}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setDescription(e.target.value)}
                placeholder="Description (optional)"
              />
              <Button
                variant="primary"
                size="sm"
                onClick={handleUpdateStatus}
                disabled={updateStatusMutation.isPending}
                className="w-full"
              >
                {updateStatusMutation.isPending ? 'Updating...' : 'Update Status'}
              </Button>
            </div>
          </div>

          {/* History */}
          <div className="bg-bg-card border border-border-default rounded-lg p-5">
            <h2 className="text-lg font-semibold text-text-heading mb-4">Delivery History</h2>
            {delivery.histories.length === 0 ? (
              <p className="text-sm text-text-meta text-center py-6">No history records</p>
            ) : (
              <div className="space-y-0">
                {delivery.histories.map((entry, index) => (
                  <div key={entry.id} className="relative pl-6 pb-6 last:pb-0">
                    {index < delivery.histories.length - 1 && (
                      <div className="absolute left-[9px] top-4 bottom-0 w-0.5 bg-border-default" />
                    )}
                    <div className={`absolute left-0 top-1 w-[18px] h-[18px] rounded-full border-2 ${
                      index === 0 ? 'border-brand-primary bg-brand-primary' : 'border-border-default bg-bg-card'
                    }`} />
                    <div>
                      <span className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full ${getDeliveryStatusColor(entry.status)}`}>
                        {DELIVERY_STATUS_LABELS[entry.status]}
                      </span>
                      {entry.location && (
                        <p className="text-sm text-text-body mt-1">{entry.location}</p>
                      )}
                      {entry.description && (
                        <p className="text-sm text-text-meta mt-0.5">{entry.description}</p>
                      )}
                      <p className="text-xs text-text-meta mt-1">
                        {new Date(entry.createdAt).toLocaleString('ko-KR')}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Empty state */}
      {!delivery && !isLoading && !error && (
        <div className="bg-bg-subtle rounded-lg p-12 text-center">
          <svg className="w-16 h-16 mx-auto text-text-placeholder mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
          </svg>
          <p className="text-text-meta">Enter a tracking number or order number to search</p>
        </div>
      )}
    </div>
  )
}

export default AdminDeliveryPage
