import { useCallback, useState } from 'react';
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
import { useTaskStore } from '@/stores/taskStore';
import type { Task, TaskStatus } from '@/types';

interface KanbanBoardProps {
  onEditTask?: (task: Task) => void;
  onAddTask?: () => void;
}

export function KanbanBoard({ onEditTask, onAddTask }: KanbanBoardProps) {
  const { columns, tasks, moveTask, executeTask } = useTaskStore();
  const [activeTask, setActiveTask] = useState<Task | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    })
  );

  const handleDragStart = useCallback(
    (event: DragStartEvent) => {
      const { active } = event;
      const task = tasks.find((t) => t.id === active.id);
      if (task) {
        setActiveTask(task);
      }
    },
    [tasks]
  );

  const handleDragOver = useCallback((_event: DragOverEvent) => {
    // Handle drag over for visual feedback
    // Actual state update happens in handleDragEnd
  }, []);

  const handleDragEnd = useCallback(
    async (event: DragEndEvent) => {
      const { active, over } = event;
      setActiveTask(null);

      if (!over) return;

      const taskId = active.id as number;
      const task = tasks.find((t) => t.id === taskId);
      if (!task) return;

      // Determine target column
      let targetStatus: TaskStatus;
      let targetPosition: number;

      // Check if dropped on a column
      if (['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'].includes(String(over.id))) {
        targetStatus = over.id as TaskStatus;
        const targetColumn = columns.find((c) => c.id === targetStatus);
        targetPosition = targetColumn ? targetColumn.tasks.length : 0;
      } else {
        // Dropped on another task
        const overTask = tasks.find((t) => t.id === over.id);
        if (!overTask) return;

        targetStatus = overTask.status;
        targetPosition = overTask.position;
      }

      // Only move if something changed
      if (task.status !== targetStatus || task.position !== targetPosition) {
        await moveTask(taskId, targetStatus, targetPosition);
      }
    },
    [tasks, columns, moveTask]
  );

  const handleExecuteTask = useCallback(
    async (task: Task) => {
      try {
        await executeTask(task.id);
      } catch (error) {
        console.error('Failed to execute task:', error);
      }
    },
    [executeTask]
  );

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCorners}
      onDragStart={handleDragStart}
      onDragOver={handleDragOver}
      onDragEnd={handleDragEnd}
    >
      <div className="flex gap-4 overflow-x-auto pb-4">
        {columns.map((column) => (
          <KanbanColumn
            key={column.id}
            column={column}
            onEditTask={onEditTask}
            onExecuteTask={handleExecuteTask}
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
  );
}
