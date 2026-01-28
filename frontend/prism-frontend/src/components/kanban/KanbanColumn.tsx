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
  onAddTask?: () => void;
}

const statusColors: Record<string, string> = {
  TODO: 'border-t-gray-400',
  IN_PROGRESS: 'border-t-blue-500',
  IN_REVIEW: 'border-t-yellow-500',
  DONE: 'border-t-green-500',
};

export function KanbanColumn({
  column,
  onEditTask,
  onExecuteTask,
  onAddTask,
}: KanbanColumnProps) {
  const { setNodeRef, isOver } = useDroppable({
    id: column.id,
  });

  return (
    <div
      className={`
        flex-shrink-0 w-72 bg-gray-50 rounded-lg
        border-t-4 ${statusColors[column.id]}
        ${isOver ? 'bg-gray-100' : ''}
      `}
    >
      <div className="p-3 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold text-gray-700">{column.title}</h3>
          <span className="text-sm text-gray-500 bg-gray-200 px-2 py-0.5 rounded-full">
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
              />
            ))}
          </div>
        </SortableContext>

        {column.id === 'TODO' && (
          <button
            onClick={onAddTask}
            className="w-full mt-2 p-2 text-sm text-gray-500 border-2 border-dashed border-gray-300 rounded-lg hover:border-prism-400 hover:text-prism-600 transition-colors"
          >
            + Add Task
          </button>
        )}
      </div>
    </div>
  );
}
