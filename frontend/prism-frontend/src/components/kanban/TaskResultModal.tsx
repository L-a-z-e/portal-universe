import { useState, useEffect, useMemo } from 'react';
import { Modal, Button, Textarea, useApiError } from '@portal/design-system-react';
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
  const { handleError } = useApiError();
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
      handleError(error, 'Failed to fetch task context');
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
      handleError(error, 'Failed to approve task');
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
      handleError(error, 'Failed to reject task');
    } finally {
      setActionLoading(false);
    }
  };

  const isInReview = task?.status === 'IN_REVIEW';

  // Agent별로 execution 그룹핑
  const executionsByAgent = useMemo(() => {
    if (!context?.previousExecutions) return {};
    return context.previousExecutions.reduce((acc, exec) => {
      const key = exec.agentName || `Agent #${exec.agentId}`;
      if (!acc[key]) acc[key] = [];
      acc[key].push(exec);
      return acc;
    }, {} as Record<string, Execution[]>);
  }, [context?.previousExecutions]);

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

        {/* Execution Results by Agent */}
        {!contextLoading && Object.keys(executionsByAgent).length > 0 && (
          <div className="space-y-4">
            <h4 className="text-sm font-medium text-text-heading">Execution Results by Agent</h4>
            {Object.entries(executionsByAgent).map(([agentName, executions]) => (
              <div key={agentName} className="border border-border-default rounded-lg overflow-hidden">
                <div className="bg-bg-subtle px-4 py-3 border-b border-border-default flex items-center justify-between">
                  <span className="font-medium text-text-heading">{agentName}</span>
                  <span className="text-xs text-text-meta bg-bg-muted px-2 py-0.5 rounded-full">
                    {executions.length} execution(s)
                  </span>
                </div>
                <div className="divide-y divide-border-default">
                  {executions.map((exec) => (
                    <div key={exec.id} className="p-4 space-y-3">
                      {/* Execution Header */}
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-text-heading">
                          Execution #{exec.executionNumber}
                        </span>
                        <span className={`text-xs px-2 py-0.5 rounded-full ${getStatusBadge(exec.status)}`}>
                          {exec.status}
                        </span>
                      </div>

                      {/* Execution Meta */}
                      <div className="grid grid-cols-2 gap-2 text-xs">
                        <div>
                          <span className="text-text-meta">Started:</span>
                          <span className="ml-1 text-text-body">{formatDate(exec.startedAt)}</span>
                        </div>
                        <div>
                          <span className="text-text-meta">Completed:</span>
                          <span className="ml-1 text-text-body">{formatDate(exec.completedAt)}</span>
                        </div>
                        {exec.durationMs && (
                          <div>
                            <span className="text-text-meta">Duration:</span>
                            <span className="ml-1 text-text-body">{(exec.durationMs / 1000).toFixed(2)}s</span>
                          </div>
                        )}
                        {(exec.inputTokens || exec.outputTokens) && (
                          <div>
                            <span className="text-text-meta">Tokens:</span>
                            <span className="ml-1 text-text-body">
                              {exec.inputTokens || 0} in / {exec.outputTokens || 0} out
                            </span>
                          </div>
                        )}
                      </div>

                      {/* Output Result */}
                      {exec.outputResult && (
                        <div>
                          <label className="block text-xs font-medium text-text-heading mb-1">
                            AI Response
                          </label>
                          <div className="bg-bg-input border border-border-default rounded-lg p-3 max-h-60 overflow-y-auto">
                            <pre className="whitespace-pre-wrap text-xs text-text-body font-mono">
                              {exec.outputResult}
                            </pre>
                          </div>
                        </div>
                      )}

                      {/* Error Message */}
                      {exec.errorMessage && (
                        <div>
                          <label className="block text-xs font-medium text-status-error mb-1">
                            Error
                          </label>
                          <div className="bg-status-error/10 border border-status-error/20 rounded-lg p-3">
                            <pre className="whitespace-pre-wrap text-xs text-status-error font-mono">
                              {exec.errorMessage}
                            </pre>
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            ))}
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
        {!contextLoading && Object.keys(executionsByAgent).length === 0 && (
          <div className="text-center py-8 text-text-meta">
            No execution results yet
          </div>
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
