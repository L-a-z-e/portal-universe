import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Button, Badge, Tooltip, Spinner } from '@portal/design-react';
import { useTaskStore } from '@/stores/taskStore';
import type { Task, TaskPriority } from '@/types';

interface TaskCardProps {
  task: Task;
  onEdit?: (task: Task) => void;
  onExecute?: (task: Task) => void;
  onView?: (task: Task) => void;
}

const priorityBadgeVariant: Record<TaskPriority, 'default' | 'primary' | 'warning' | 'error'> = {
  LOW: 'default',
  MEDIUM: 'primary',
  HIGH: 'warning',
  URGENT: 'error',
};

const priorityLabels: Record<TaskPriority, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  URGENT: 'Urgent',
};

// K1: Priority left border colors
const priorityBorderColors: Record<TaskPriority, string> = {
  LOW: '',
  MEDIUM: '',
  HIGH: 'border-l-4 border-l-status-warning',
  URGENT: 'border-l-4 border-l-status-error',
};

// K1: DueDate helpers
function getDueDateInfo(dueDate: string): { label: string; className: string } {
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  const due = new Date(dueDate);
  due.setHours(0, 0, 0, 0);
  const diffDays = Math.floor((due.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));

  if (diffDays < 0) {
    return { label: `${Math.abs(diffDays)}d overdue`, className: 'text-status-error' };
  }
  if (diffDays === 0) {
    return { label: 'Due today', className: 'text-status-warning' };
  }
  if (diffDays === 1) {
    return { label: 'Due tomorrow', className: 'text-status-warning' };
  }
  return { label: `Due in ${diffDays}d`, className: 'text-text-meta' };
}

export function TaskCard({ task, onEdit, onExecute, onView }: TaskCardProps) {
  const executingTaskIds = useTaskStore((state) => state.executingTaskIds);
  const isExecuting = executingTaskIds.has(task.id);

  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: task.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  const handleEdit = (e: React.MouseEvent) => {
    e.stopPropagation();
    onEdit?.(task);
  };

  const handleExecute = (e: React.MouseEvent) => {
    e.stopPropagation();
    onExecute?.(task);
  };

  const handleView = (e: React.MouseEvent) => {
    e.stopPropagation();
    onView?.(task);
  };

  const canEdit = task.status === 'TODO';
  const canRun = task.status === 'TODO' && task.agentId;
  const canView = ['IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CANCELLED'].includes(task.status);
  const showReviewActions = task.status === 'IN_REVIEW';

  const refCount = task.referencedTaskIds?.length ?? 0;
  const dueDateInfo = task.dueDate ? getDueDateInfo(task.dueDate) : null;

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      className={`
        bg-bg-card rounded-lg shadow-sm border border-border-default p-3
        hover:shadow-md transition-shadow cursor-grab
        ${priorityBorderColors[task.priority]}
        ${isDragging ? 'shadow-lg' : ''}
      `}
    >
      {/* Header: title + priority badge */}
      <div className="flex items-start justify-between gap-2 mb-1">
        <h4 className="font-medium text-text-heading text-sm line-clamp-2">
          {task.title}
        </h4>
        <Badge variant={priorityBadgeVariant[task.priority]} size="sm">
          {priorityLabels[task.priority]}
        </Badge>
      </div>

      {/* K3: description 1 line */}
      {task.description && (
        <p className="text-xs text-text-meta mb-2 line-clamp-1">
          {task.description}
        </p>
      )}

      {/* Meta row: agent + dueDate + referenced */}
      <div className="flex items-center gap-2 text-xs">
        {task.agentName ? (
          <span className="bg-brand-primary/10 text-brand-primary px-2 py-0.5 rounded truncate max-w-[120px]">
            {task.agentName}
          </span>
        ) : (
          // T2: Assign agent prompt
          task.status === 'TODO' ? (
            <button
              onClick={handleEdit}
              className="text-brand-primary hover:underline cursor-pointer"
            >
              + Assign agent
            </button>
          ) : (
            <span className="text-text-muted">No agent</span>
          )
        )}
        {dueDateInfo && (
          <span className={`${dueDateInfo.className} shrink-0`}>
            {dueDateInfo.label}
          </span>
        )}
        {/* T3: Referenced tasks indicator */}
        {refCount > 0 && (
          <Tooltip content={`${refCount} referenced task${refCount > 1 ? 's' : ''}`}>
            <span className="text-text-muted shrink-0 cursor-default">
              <svg className="w-3 h-3 inline mr-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
              </svg>
              {refCount}
            </span>
          </Tooltip>
        )}
      </div>

      {/* T1: Action buttons - separated area */}
      <div className="flex gap-1 items-center border-t border-border-default pt-2 mt-2" onPointerDown={(e) => e.stopPropagation()}>
        {isExecuting ? (
          <span className="flex items-center gap-1 text-xs text-brand-primary flex-1 justify-center">
            <Spinner size="sm" />
            Running...
          </span>
        ) : (
          <>
            {canRun && (
              <Button size="sm" variant="primary" onClick={handleExecute} className="flex-1">
                Run
              </Button>
            )}
            {showReviewActions && (
              <Button
                size="sm"
                variant="ghost"
                onClick={handleView}
                className="flex-1 bg-status-warning/20 text-status-warning hover:bg-status-warning/30"
              >
                Review
              </Button>
            )}
            {canView && !showReviewActions && (
              <Button size="sm" variant="ghost" onClick={handleView} className="flex-1 text-text-meta hover:text-text-heading">
                View
              </Button>
            )}
            {canEdit && (
              <Button size="sm" variant="ghost" onClick={handleEdit} className="flex-1 text-text-meta hover:text-text-heading">
                Edit
              </Button>
            )}
          </>
        )}
      </div>
    </div>
  );
}
