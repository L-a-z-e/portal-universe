/**
 * Admin Queue Page
 * 관리자 대기열 활성화/비활성화/수동처리 페이지
 */
import React, { useState } from 'react'
import { useActivateQueue, useDeactivateQueue, useProcessQueue } from '@/hooks/useAdminQueue'
import { Button, Input, Alert } from '@portal/design-system-react'

const AdminQueuePage: React.FC = () => {
  const [eventType, setEventType] = useState('TIME_DEAL')
  const [eventId, setEventId] = useState('')
  const [maxCapacity, setMaxCapacity] = useState('100')
  const [entryBatchSize, setEntryBatchSize] = useState('10')
  const [entryIntervalSeconds, setEntryIntervalSeconds] = useState('5')
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)

  const activateMutation = useActivateQueue()
  const deactivateMutation = useDeactivateQueue()
  const processMutation = useProcessQueue()

  const eventIdNum = parseInt(eventId)
  const isValidInput = eventId && !isNaN(eventIdNum) && eventIdNum > 0

  const handleActivate = async () => {
    if (!isValidInput) return
    setMessage(null)
    try {
      await activateMutation.mutateAsync(eventType, eventIdNum, {
        maxCapacity: parseInt(maxCapacity),
        entryBatchSize: parseInt(entryBatchSize),
        entryIntervalSeconds: parseInt(entryIntervalSeconds)
      })
      setMessage({ type: 'success', text: `Queue activated for ${eventType}:${eventId}` })
    } catch (e) {
      setMessage({ type: 'error', text: e instanceof Error ? e.message : 'Failed to activate queue' })
    }
  }

  const handleDeactivate = async () => {
    if (!isValidInput) return
    if (!confirm('Are you sure you want to deactivate this queue?')) return
    setMessage(null)
    try {
      await deactivateMutation.mutateAsync(eventType, eventIdNum)
      setMessage({ type: 'success', text: `Queue deactivated for ${eventType}:${eventId}` })
    } catch (e) {
      setMessage({ type: 'error', text: e instanceof Error ? e.message : 'Failed to deactivate queue' })
    }
  }

  const handleProcess = async () => {
    if (!isValidInput) return
    setMessage(null)
    try {
      await processMutation.mutateAsync(eventType, eventIdNum)
      setMessage({ type: 'success', text: `Queue manually processed for ${eventType}:${eventId}` })
    } catch (e) {
      setMessage({ type: 'error', text: e instanceof Error ? e.message : 'Failed to process queue' })
    }
  }

  const isPending = activateMutation.isPending || deactivateMutation.isPending || processMutation.isPending

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-text-heading">Queue Management</h1>

      {message && (
        <Alert variant={message.type === 'success' ? 'success' : 'error'}>
          {message.text}
        </Alert>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Queue Target */}
        <div className="bg-bg-card border border-border-default rounded-lg p-5 space-y-4">
          <h2 className="text-lg font-semibold text-text-heading">Queue Target</h2>

          <div className="space-y-3">
            <div>
              <label className="block text-sm font-medium text-text-body mb-1">Event Type</label>
              <select
                value={eventType}
                onChange={(e) => setEventType(e.target.value)}
                className="w-full px-3 py-2 border border-border-default rounded-lg bg-bg-card text-text-body text-sm focus:outline-none focus:ring-2 focus:ring-brand-primary"
              >
                <option value="TIME_DEAL">Time Deal</option>
                <option value="COUPON">Coupon</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-text-body mb-1">Event ID</label>
              <Input
                type="number"
                value={eventId}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEventId(e.target.value)}
                placeholder="Enter event ID..."
                min="1"
              />
            </div>
          </div>
        </div>

        {/* Activation Settings */}
        <div className="bg-bg-card border border-border-default rounded-lg p-5 space-y-4">
          <h2 className="text-lg font-semibold text-text-heading">Activation Settings</h2>

          <div className="space-y-3">
            <div>
              <label className="block text-sm font-medium text-text-body mb-1">Max Capacity</label>
              <Input
                type="number"
                value={maxCapacity}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setMaxCapacity(e.target.value)}
                min="1"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-text-body mb-1">Entry Batch Size</label>
              <Input
                type="number"
                value={entryBatchSize}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEntryBatchSize(e.target.value)}
                min="1"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-text-body mb-1">Entry Interval (seconds)</label>
              <Input
                type="number"
                value={entryIntervalSeconds}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEntryIntervalSeconds(e.target.value)}
                min="1"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Actions */}
      <div className="bg-bg-card border border-border-default rounded-lg p-5">
        <h2 className="text-lg font-semibold text-text-heading mb-4">Actions</h2>
        <div className="flex flex-wrap gap-3">
          <Button
            variant="primary"
            onClick={handleActivate}
            disabled={!isValidInput || isPending}
          >
            {activateMutation.isPending ? 'Activating...' : 'Activate Queue'}
          </Button>

          <Button
            variant="secondary"
            onClick={handleProcess}
            disabled={!isValidInput || isPending}
          >
            {processMutation.isPending ? 'Processing...' : 'Process Queue'}
          </Button>

          <Button
            variant="secondary"
            onClick={handleDeactivate}
            disabled={!isValidInput || isPending}
            className="text-status-error border-status-error/30 hover:bg-status-error/10"
          >
            {deactivateMutation.isPending ? 'Deactivating...' : 'Deactivate Queue'}
          </Button>
        </div>
      </div>

      {/* Help */}
      <div className="bg-bg-subtle rounded-lg p-5">
        <h3 className="text-sm font-medium text-text-heading mb-2">How Queue Management Works</h3>
        <ul className="text-sm text-text-meta space-y-1.5 list-disc list-inside">
          <li><strong>Activate</strong>: Start a queue for the specified event with capacity and batch settings</li>
          <li><strong>Process</strong>: Manually trigger processing of waiting users in the queue</li>
          <li><strong>Deactivate</strong>: Stop the queue and remove all waiting entries</li>
        </ul>
      </div>
    </div>
  )
}

export default AdminQueuePage
