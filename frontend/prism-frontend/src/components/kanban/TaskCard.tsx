import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { useTaskStore } from '@/stores/taskStore';
import type { Task, TaskPriority } from '@/types';

interface TaskCardProps {
  task: Task;
  onEdit?: (task: Task) => void;
  onExecute?: (task: Task) => void;
  onView?: (task: Task) => void;
}

const priorityColors: Record<TaskPriority, string> = {
  LOW: 'bg-bg-muted text-text-meta',
  MEDIUM: 'bg-status-info/20 text-status-info',
  HIGH: 'bg-status-warning/20 text-status-warning',
  URGENT: 'bg-status-error/20 text-status-error',
};

const priorityLabels: Record<TaskPriority, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  URGENT: 'Urgent',
};

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

  // 상태별 버튼 렌더링 로직
  const canEdit = task.status === 'TODO';
  const canRun = task.status === 'TODO' && task.agentId;
  const canView = ['IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CANCELLED'].includes(task.status);
  const showReviewActions = task.status === 'IN_REVIEW';

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      className={`
        bg-bg-card rounded-lg shadow-sm border border-border-default p-3
        hover:shadow-md transition-shadow cursor-grab
        ${isDragging ? 'shadow-lg' : ''}
      `}
    >
      <div className="flex items-start justify-between gap-2 mb-2">
        <h4 className="font-medium text-text-heading text-sm line-clamp-2">
          {task.title}
        </h4>
        <span className={`text-xs px-2 py-0.5 rounded-full shrink-0 ${priorityColors[task.priority]}`}>
          {priorityLabels[task.priority]}
        </span>
      </div>

      {task.description && (
        <p className="text-xs text-text-meta mb-2 line-clamp-2">
          {task.description}
        </p>
      )}

      <div className="flex items-center justify-between mt-3">
        {task.agentName ? (
          <span className="text-xs bg-brand-primary/10 text-brand-primary px-2 py-0.5 rounded">
            {task.agentName}
          </span>
        ) : (
          <span className="text-xs text-text-muted">No agent</span>
        )}

        <div className="flex gap-1 items-center">
          {isExecuting ? (
            <span className="flex items-center gap-1 text-xs text-brand-primary">
              <svg className="animate-spin h-3 w-3" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
              Running...
            </span>
          ) : (
            <>
              {/* TODO: Run 버튼 */}
              {canRun && (
                <button
                  onClick={handleExecute}
                  className="text-xs px-2 py-1 bg-brand-primary text-white rounded hover:bg-brand-primaryHover transition-colors"
                >
                  Run
                </button>
              )}

              {/* IN_REVIEW: View (결과 확인 + Approve/Reject) */}
              {showReviewActions && (
                <button
                  onClick={handleView}
                  className="text-xs px-2 py-1 bg-status-warning/20 text-status-warning rounded hover:bg-status-warning/30 transition-colors"
                >
                  Review
                </button>
              )}

              {/* IN_PROGRESS, DONE, CANCELLED: View 버튼 */}
              {canView && !showReviewActions && (
                <button
                  onClick={handleView}
                  className="text-xs px-2 py-1 text-text-meta hover:text-text-heading hover:bg-bg-hover rounded transition-colors"
                >
                  View
                </button>
              )}

              {/* TODO: Edit 버튼 */}
              {canEdit && (
                <button
                  onClick={handleEdit}
                  className="text-xs px-2 py-1 text-text-meta hover:text-text-heading hover:bg-bg-hover rounded transition-colors"
                >
                  Edit
                </button>
              )}
            </>
          )}
        </div>
      </div>

      {task.dueDate && (
        <div className="mt-2 text-xs text-text-muted">
          Due: {new Date(task.dueDate).toLocaleDateString()}
        </div>
      )}
    </div>
  );
}
