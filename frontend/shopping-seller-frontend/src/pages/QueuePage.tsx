import React, { useState } from 'react'
import { sellerQueueApi } from '@/api'
import { Button, Input } from '@portal/design-system-react'

export const QueuePage: React.FC = () => {
  const [eventType, setEventType] = useState('TIME_DEAL')
  const [eventId, setEventId] = useState('')
  const [status, setStatus] = useState<any>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchStatus = async () => {
    if (!eventId) return
    setIsLoading(true)
    setError(null)
    try {
      const res = await sellerQueueApi.getQueueStatus(eventType, eventId)
      setStatus(res.data?.data || null)
    } catch (err: any) {
      if (err?.response?.status === 404) {
        setStatus(null)
        setError('No queue found for this event.')
      } else {
        setError('Failed to fetch queue status.')
      }
    } finally {
      setIsLoading(false)
    }
  }

  const handleActivate = async () => {
    if (!eventId) return
    setIsLoading(true)
    setError(null)
    try {
      const res = await sellerQueueApi.activateQueue(eventType, eventId, {
        maxCapacity: 1000,
        entryBatchSize: 50,
        entryIntervalSeconds: 10,
      })
      setStatus(res.data?.data || null)
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to activate queue.')
    } finally {
      setIsLoading(false)
    }
  }

  const handleDeactivate = async () => {
    if (!eventId) return
    setIsLoading(true)
    setError(null)
    try {
      await sellerQueueApi.deactivateQueue(eventType, eventId)
      await fetchStatus()
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to deactivate queue.')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-text-heading mb-6">Queue Management</h1>

      <div className="bg-bg-card border border-border-default rounded-lg p-6 shadow-sm mb-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
          <div>
            <label className="block text-sm font-medium text-text-heading mb-1">Event Type</label>
            <select
              className="w-full px-3 py-2 border border-border-default rounded-md bg-bg-default text-text-body"
              value={eventType}
              onChange={(e) => setEventType(e.target.value)}
            >
              <option value="TIME_DEAL">Time Deal</option>
              <option value="COUPON">Coupon</option>
              <option value="FLASH_SALE">Flash Sale</option>
            </select>
          </div>
          <Input
            label="Event ID"
            value={eventId}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEventId(e.target.value)}
          />
          <div className="flex items-end">
            <Button variant="primary" onClick={fetchStatus} disabled={isLoading || !eventId}>
              Check Status
            </Button>
          </div>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-status-error-bg border border-status-error rounded">
            <p className="text-sm text-status-error">{error}</p>
          </div>
        )}

        {status && (
          <div className="border-t border-border-default pt-4 mt-4">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
              <div>
                <p className="text-xs text-text-meta">Status</p>
                <p className={`text-lg font-bold ${status.isActive ? 'text-status-success' : 'text-text-meta'}`}>
                  {status.isActive ? 'Active' : 'Inactive'}
                </p>
              </div>
              <div>
                <p className="text-xs text-text-meta">Waiting</p>
                <p className="text-lg font-bold text-text-heading">{status.waitingCount}</p>
              </div>
              <div>
                <p className="text-xs text-text-meta">Entered</p>
                <p className="text-lg font-bold text-text-heading">{status.enteredCount}</p>
              </div>
              <div>
                <p className="text-xs text-text-meta">Max Capacity</p>
                <p className="text-lg font-bold text-text-heading">{status.maxCapacity}</p>
              </div>
            </div>

            <div className="flex gap-3">
              {!status.isActive ? (
                <Button variant="primary" onClick={handleActivate} disabled={isLoading}>
                  Activate Queue
                </Button>
              ) : (
                <Button variant="ghost" onClick={handleDeactivate} disabled={isLoading}>
                  Deactivate Queue
                </Button>
              )}
            </div>
          </div>
        )}

        {!status && !error && !isLoading && (
          <p className="text-sm text-text-meta mt-2">Enter an event type and ID to check queue status.</p>
        )}
      </div>
    </div>
  )
}

export default QueuePage
