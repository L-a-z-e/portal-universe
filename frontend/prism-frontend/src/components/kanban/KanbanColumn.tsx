import { useDroppable } from '@dnd-kit/core';
import {
  SortableContext,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { TaskCard } from './TaskCard';
import type { KanbanColumn as KanbanColumnType, Task } from '@/types';

interface KanbanColumnProps {
  column: KanbanColumnType;
  onEditTask?: (task: Task) => void;
  onExecuteTask?: (task: Task) => void;
  onViewTask?: (task: Task) => void;
  onAddTask?: () => void;
}

const statusColors: Record<string, string> = {
  TODO: 'border-t-text-muted',
  IN_PROGRESS: 'border-t-status-info',
  IN_REVIEW: 'border-t-status-warning',
  DONE: 'border-t-status-success',
  CANCELLED: 'border-t-status-error',
};

export function KanbanColumn({
  column,
  onEditTask,
  onExecuteTask,
  onViewTask,
  onAddTask,
}: KanbanColumnProps) {
  const { setNodeRef, isOver } = useDroppable({
    id: column.id,
  });

  return (
    <div
      className={`
        flex-1 min-w-[180px] bg-bg-subtle rounded-lg
        border-t-4 ${statusColors[column.id]}
        ${isOver ? 'bg-bg-muted' : ''}
      `}
    >
      <div className="p-3 border-b border-border-default">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold text-text-body">{column.title}</h3>
          <span className="text-sm text-text-meta bg-bg-muted px-2 py-0.5 rounded-full">
            {column.tasks.length}
          </span>
        </div>
      </div>

      <div
        ref={setNodeRef}
        className="p-2 min-h-[200px] max-h-[calc(100vh-280px)] overflow-y-auto"
      >
        <SortableContext
          items={column.tasks.map((t) => t.id)}
          strategy={verticalListSortingStrategy}
        >
          <div className="space-y-2">
            {column.tasks.map((task) => (
              <TaskCard
                key={task.id}
                task={task}
                onEdit={onEditTask}
                onExecute={onExecuteTask}
                onView={onViewTask}
              />
            ))}
          </div>
        </SortableContext>

        {/* K6: Guide text for empty TODO column instead of duplicate Add Task button */}
        {column.id === 'TODO' && column.tasks.length === 0 && onAddTask && (
          <p className="text-center text-sm text-text-muted py-8">
            Drag tasks here or use the Add Task button above
          </p>
        )}
      </div>
    </div>
  );
}
