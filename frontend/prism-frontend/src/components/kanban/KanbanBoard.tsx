import { useCallback, useState, useMemo } from 'react';
import { Input, Select, useApiError } from '@portal/design-react';
import {
  DndContext,
  DragEndEvent,
  DragOverEvent,
  DragOverlay,
  DragStartEvent,
  PointerSensor,
  useSensor,
  useSensors,
  closestCorners,
} from '@dnd-kit/core';
import { KanbanColumn } from './KanbanColumn';
import { TaskCard } from './TaskCard';
import { useTaskStore, COLUMN_CONFIG } from '@/stores/taskStore';
import type { Task, TaskStatus } from '@/types';

interface KanbanBoardProps {
  onEditTask?: (task: Task) => void;
  onViewTask?: (task: Task) => void;
  onAddTask?: () => void;
}

const priorityFilterOptions = [
  { value: '', label: 'All Priorities' },
  { value: 'URGENT', label: 'Urgent' },
  { value: 'HIGH', label: 'High' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'LOW', label: 'Low' },
];

export function KanbanBoard({ onEditTask, onViewTask, onAddTask }: KanbanBoardProps) {
  const { columns, moveTask, executeTask } = useTaskStore();
  const { handleError } = useApiError();
  const [activeTask, setActiveTask] = useState<Task | null>(null);

  // K4: Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    })
  );

  // K4: Filtered columns
  const filteredColumns = useMemo(() => {
    if (!searchQuery && !priorityFilter) return columns;

    const query = searchQuery.toLowerCase();
    return columns.map((col) => ({
      ...col,
      tasks: col.tasks.filter((task) => {
        if (priorityFilter && task.priority !== priorityFilter) return false;
        if (query) {
          const matchTitle = task.title.toLowerCase().includes(query);
          const matchDesc = task.description?.toLowerCase().includes(query);
          if (!matchTitle && !matchDesc) return false;
        }
        return true;
      }),
    }));
  }, [columns, searchQuery, priorityFilter]);

  const handleDragStart = useCallback((event: DragStartEvent) => {
    const { active } = event;
    const { tasks } = useTaskStore.getState();
    const task = tasks.find((t) => t.id === active.id);
    if (task) {
      setActiveTask(task);
    }
  }, []);

  const handleDragOver = useCallback((_event: DragOverEvent) => {
    // Visual feedback only, actual state update in handleDragEnd
  }, []);

  const handleDragEnd = useCallback(
    async (event: DragEndEvent) => {
      const { active, over } = event;
      setActiveTask(null);

      if (!over) return;

      const { tasks, columns } = useTaskStore.getState();
      const taskId = active.id as number;
      const task = tasks.find((t) => t.id === taskId);
      if (!task) return;

      let targetStatus: TaskStatus;
      let targetPosition: number;

      const columnIds = COLUMN_CONFIG.map((c) => c.id as string);
      if (columnIds.includes(String(over.id))) {
        targetStatus = over.id as TaskStatus;
        const targetColumn = columns.find((c) => c.id === targetStatus);
        targetPosition = targetColumn ? targetColumn.tasks.length : 0;
      } else {
        const overTask = tasks.find((t) => t.id === over.id);
        if (!overTask) return;
        targetStatus = overTask.status;
        targetPosition = overTask.position;
      }

      if (task.status !== targetStatus || task.position !== targetPosition) {
        try {
          await moveTask(taskId, targetStatus, targetPosition);
        } catch (error) {
          console.error('Failed to move task:', error);
        }
      }
    },
    [moveTask]
  );

  const handleExecuteTask = useCallback(
    async (task: Task) => {
      try {
        await executeTask(task.id);
      } catch (error) {
        handleError(error, 'Failed to execute task');
      }
    },
    [executeTask, handleError]
  );

  return (
    <div className="flex flex-col h-full">
      {/* K4: Filter bar */}
      <div className="flex gap-3 mb-4 items-end">
        <div className="flex-1 max-w-xs">
          <Input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search tasks..."
            size="sm"
          />
        </div>
        <div className="w-40">
          <Select
            value={priorityFilter}
            onChange={(value) => setPriorityFilter(String(value ?? ''))}
            options={priorityFilterOptions}
            size="sm"
          />
        </div>
      </div>

      <DndContext
        sensors={sensors}
        collisionDetection={closestCorners}
        onDragStart={handleDragStart}
        onDragOver={handleDragOver}
        onDragEnd={handleDragEnd}
      >
        <div className="flex gap-4 w-full min-w-0 overflow-x-auto pb-4 flex-1">
          {filteredColumns.map((column) => (
            <KanbanColumn
              key={column.id}
              column={column}
              onEditTask={onEditTask}
              onExecuteTask={handleExecuteTask}
              onViewTask={onViewTask}
              onAddTask={column.id === 'TODO' ? onAddTask : undefined}
            />
          ))}
        </div>

        <DragOverlay>
          {activeTask ? (
            <div className="rotate-3 scale-105">
              <TaskCard task={activeTask} />
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>
    </div>
  );
}
