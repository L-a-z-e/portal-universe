import { useState, useEffect } from 'react';
import { Modal, Button, Textarea } from '@portal/design-system-react';
import { api } from '@/services/api';
import type { Task, Execution } from '@/types';

interface TaskResultModalProps {
  isOpen: boolean;
  onClose: () => void;
  task: Task | null;
  onApprove?: (task: Task) => Promise<void>;
  onReject?: (task: Task, feedback: string) => Promise<void>;
}

interface TaskContext {
  previousExecutions: Execution[];
  referencedTasks: Array<{
    taskId: number;
    taskTitle: string;
    lastExecution: Execution | null;
  }>;
}

export function TaskResultModal({
  isOpen,
  onClose,
  task,
  onApprove,
  onReject,
}: TaskResultModalProps) {
  const [context, setContext] = useState<TaskContext | null>(null);
  const [contextLoading, setContextLoading] = useState(false);
  const [showRejectForm, setShowRejectForm] = useState(false);
  const [feedback, setFeedback] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    if (isOpen && task) {
      setShowRejectForm(false);
      setFeedback('');
      fetchContext();
    }
  }, [isOpen, task?.id]);

  const fetchContext = async () => {
    if (!task) return;

    setContextLoading(true);
    try {
      const data = await api.getTaskContext(task.id);
      setContext(data);
    } catch (error) {
      console.error('Failed to fetch task context:', error);
    } finally {
      setContextLoading(false);
    }
  };

  const handleApprove = async () => {
    if (!task || !onApprove) return;

    setActionLoading(true);
    try {
      await onApprove(task);
      onClose();
    } catch (error) {
      console.error('Failed to approve task:', error);
    } finally {
      setActionLoading(false);
    }
  };

  const handleReject = async () => {
    if (!task || !onReject) return;

    setActionLoading(true);
    try {
      await onReject(task, feedback);
      onClose();
    } catch (error) {
      console.error('Failed to reject task:', error);
    } finally {
      setActionLoading(false);
    }
  };

  const latestExecution = context?.previousExecutions?.[0];
  const isInReview = task?.status === 'IN_REVIEW';

  const formatDate = (dateString?: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString();
  };

  const getStatusBadge = (status: string) => {
    const styles: Record<string, string> = {
      COMPLETED: 'bg-status-success/20 text-status-success',
      FAILED: 'bg-status-error/20 text-status-error',
      RUNNING: 'bg-status-info/20 text-status-info',
      PENDING: 'bg-bg-muted text-text-meta',
    };
    return styles[status] || styles.PENDING;
  };

  return (
    <Modal
      open={isOpen}
      onClose={onClose}
      title={task?.title || 'Task Result'}
      size="lg"
    >
      <div className="space-y-6">
        {/* Task Info */}
        <div className="bg-bg-subtle rounded-lg p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-text-heading">Task Status</span>
            <span className={`text-xs px-2 py-1 rounded-full ${
              task?.status === 'IN_REVIEW' ? 'bg-status-warning/20 text-status-warning' :
              task?.status === 'DONE' ? 'bg-status-success/20 text-status-success' :
              'bg-bg-muted text-text-meta'
            }`}>
              {task?.status}
            </span>
          </div>
          {task?.description && (
            <p className="text-sm text-text-body">{task.description}</p>
          )}
        </div>

        {/* Loading State */}
        {contextLoading && (
          <div className="flex items-center justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-primary" />
          </div>
        )}

        {/* Execution Result */}
        {!contextLoading && latestExecution && (
          <div className="border border-border-default rounded-lg overflow-hidden">
            <div className="bg-bg-subtle px-4 py-3 border-b border-border-default flex items-center justify-between">
              <span className="font-medium text-text-heading">
                Execution #{latestExecution.executionNumber}
              </span>
              <span className={`text-xs px-2 py-1 rounded-full ${getStatusBadge(latestExecution.status)}`}>
                {latestExecution.status}
              </span>
            </div>

            <div className="p-4 space-y-4">
              {/* Execution Meta */}
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-text-meta">Started:</span>
                  <span className="ml-2 text-text-body">{formatDate(latestExecution.startedAt)}</span>
                </div>
                <div>
                  <span className="text-text-meta">Completed:</span>
                  <span className="ml-2 text-text-body">{formatDate(latestExecution.completedAt)}</span>
                </div>
                {latestExecution.durationMs && (
                  <div>
                    <span className="text-text-meta">Duration:</span>
                    <span className="ml-2 text-text-body">{(latestExecution.durationMs / 1000).toFixed(2)}s</span>
                  </div>
                )}
                {(latestExecution.inputTokens || latestExecution.outputTokens) && (
                  <div>
                    <span className="text-text-meta">Tokens:</span>
                    <span className="ml-2 text-text-body">
                      {latestExecution.inputTokens || 0} in / {latestExecution.outputTokens || 0} out
                    </span>
                  </div>
                )}
              </div>

              {/* Output Result */}
              {latestExecution.outputResult && (
                <div>
                  <label className="block text-sm font-medium text-text-heading mb-2">
                    AI Response
                  </label>
                  <div className="bg-bg-input border border-border-default rounded-lg p-4 max-h-80 overflow-y-auto">
                    <pre className="whitespace-pre-wrap text-sm text-text-body font-mono">
                      {latestExecution.outputResult}
                    </pre>
                  </div>
                </div>
              )}

              {/* Error Message */}
              {latestExecution.errorMessage && (
                <div>
                  <label className="block text-sm font-medium text-status-error mb-2">
                    Error
                  </label>
                  <div className="bg-status-error/10 border border-status-error/20 rounded-lg p-4">
                    <pre className="whitespace-pre-wrap text-sm text-status-error font-mono">
                      {latestExecution.errorMessage}
                    </pre>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Referenced Tasks */}
        {!contextLoading && context?.referencedTasks && context.referencedTasks.length > 0 && (
          <div className="border border-border-default rounded-lg overflow-hidden">
            <div className="bg-bg-subtle px-4 py-3 border-b border-border-default">
              <span className="font-medium text-text-heading">Referenced Tasks</span>
            </div>
            <div className="divide-y divide-border-default">
              {context.referencedTasks.map((ref) => (
                <div key={ref.taskId} className="p-4">
                  <div className="flex items-center justify-between mb-2">
                    <span className="font-medium text-text-heading text-sm">{ref.taskTitle}</span>
                    {ref.lastExecution && (
                      <span className={`text-xs px-2 py-0.5 rounded-full ${getStatusBadge(ref.lastExecution.status)}`}>
                        {ref.lastExecution.status}
                      </span>
                    )}
                  </div>
                  {ref.lastExecution?.outputResult && (
                    <div className="bg-bg-input border border-border-default rounded-lg p-3 max-h-32 overflow-y-auto">
                      <pre className="whitespace-pre-wrap text-xs text-text-body font-mono">
                        {ref.lastExecution.outputResult.substring(0, 500)}
                        {ref.lastExecution.outputResult.length > 500 && '...'}
                      </pre>
                    </div>
                  )}
                  {!ref.lastExecution && (
                    <p className="text-xs text-text-meta">No execution result yet</p>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* No Execution */}
        {!contextLoading && !latestExecution && (
          <div className="text-center py-8 text-text-meta">
            No execution results yet
          </div>
        )}

        {/* Previous Executions */}
        {!contextLoading && context?.previousExecutions && context.previousExecutions.length > 1 && (
          <details className="border border-border-default rounded-lg">
            <summary className="px-4 py-3 bg-bg-subtle cursor-pointer text-sm font-medium text-text-heading">
              Previous Executions ({context.previousExecutions.length - 1})
            </summary>
            <div className="divide-y divide-border-default">
              {context.previousExecutions.slice(1).map((exec) => (
                <div key={exec.id} className="p-4">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm text-text-heading">
                      Execution #{exec.executionNumber}
                    </span>
                    <span className={`text-xs px-2 py-0.5 rounded-full ${getStatusBadge(exec.status)}`}>
                      {exec.status}
                    </span>
                  </div>
                  <p className="text-xs text-text-meta">{formatDate(exec.completedAt)}</p>
                  {exec.outputResult && (
                    <div className="mt-2 bg-bg-input border border-border-default rounded p-2 max-h-24 overflow-y-auto">
                      <pre className="whitespace-pre-wrap text-xs text-text-body font-mono">
                        {exec.outputResult.substring(0, 200)}
                        {exec.outputResult.length > 200 && '...'}
                      </pre>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </details>
        )}

        {/* Reject Form */}
        {showRejectForm && (
          <div className="border border-status-warning rounded-lg p-4">
            <label className="block text-sm font-medium text-text-heading mb-2">
              Feedback for Retry
            </label>
            <Textarea
              value={feedback}
              onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setFeedback(e.target.value)}
              placeholder="Provide feedback for the AI agent..."
              rows={3}
            />
          </div>
        )}

        {/* Actions */}
        <div className="flex justify-end gap-2 pt-4 border-t border-border-default">
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>

          {isInReview && onApprove && onReject && (
            <>
              {showRejectForm ? (
                <>
                  <Button
                    variant="secondary"
                    onClick={() => setShowRejectForm(false)}
                  >
                    Cancel
                  </Button>
                  <Button
                    variant="danger"
                    onClick={handleReject}
                    loading={actionLoading}
                  >
                    Submit & Retry
                  </Button>
                </>
              ) : (
                <>
                  <Button
                    variant="danger"
                    onClick={() => setShowRejectForm(true)}
                  >
                    Reject & Retry
                  </Button>
                  <Button
                    onClick={handleApprove}
                    loading={actionLoading}
                  >
                    Approve
                  </Button>
                </>
              )}
            </>
          )}
        </div>
      </div>
    </Modal>
  );
}
