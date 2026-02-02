import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { KanbanBoard, TaskModal } from '@/components/kanban';
import { Button } from '@portal/design-system-react';
import { useBoardStore } from '@/stores/boardStore';
import { useTaskStore } from '@/stores/taskStore';
import { useSse, SseEvent } from '@/hooks/useSse';
import type { Task, CreateTaskRequest, UpdateTaskRequest, TaskStatus } from '@/types';

function BoardPage() {
  const { boardId } = useParams<{ boardId: string }>();
  const navigate = useNavigate();

  const { currentBoard, fetchBoard, loading: boardLoading } = useBoardStore();
  const {
    fetchTasks,
    createTask,
    updateTask,
    deleteTask,
    loading: taskLoading,
    handleTaskCreated,
    handleTaskUpdated,
    handleTaskMoved,
    handleTaskDeleted,
    handleExecutionStarted,
    handleExecutionCompleted,
    handleExecutionFailed,
  } = useTaskStore();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);

  const boardIdNum = boardId ? parseInt(boardId) : null;

  // SSE Event Handler
  const handleSseEvent = useCallback((event: SseEvent) => {
    switch (event.type) {
      case 'task.created':
        handleTaskCreated(event.payload.task as Task);
        break;
      case 'task.updated':
        handleTaskUpdated(event.payload.task as Task);
        break;
      case 'task.moved':
        handleTaskMoved(
          event.payload.taskId as number,
          event.payload.toStatus as TaskStatus,
          event.payload.position as number
        );
        break;
      case 'task.deleted':
        handleTaskDeleted(event.payload.taskId as number);
        break;
      case 'execution.started':
        handleExecutionStarted(event.payload.taskId as number);
        break;
      case 'execution.completed':
        handleExecutionCompleted(event.payload.taskId as number);
        break;
      case 'execution.failed':
        handleExecutionFailed(event.payload.taskId as number);
        break;
    }
  }, [
    handleTaskCreated,
    handleTaskUpdated,
    handleTaskMoved,
    handleTaskDeleted,
    handleExecutionStarted,
    handleExecutionCompleted,
    handleExecutionFailed,
  ]);

  // Connect to SSE
  useSse({
    boardId: boardIdNum,
    onEvent: handleSseEvent,
    enabled: !!boardIdNum,
  });

  useEffect(() => {
    if (boardIdNum) {
      fetchBoard(boardIdNum);
      fetchTasks(boardIdNum);
    }
  }, [boardIdNum, fetchBoard, fetchTasks]);

  const handleAddTask = useCallback(() => {
    setSelectedTask(null);
    setIsModalOpen(true);
  }, []);

  const handleEditTask = useCallback((task: Task) => {
    setSelectedTask(task);
    setIsModalOpen(true);
  }, []);

  const handleCloseModal = useCallback(() => {
    setIsModalOpen(false);
    setSelectedTask(null);
  }, []);

  const handleSubmitTask = useCallback(
    async (data: CreateTaskRequest | UpdateTaskRequest) => {
      if (selectedTask) {
        await updateTask(selectedTask.id, data as UpdateTaskRequest);
      } else {
        await createTask(data as CreateTaskRequest);
      }
    },
    [selectedTask, createTask, updateTask]
  );

  const handleDeleteTask = useCallback(
    async (id: number) => {
      await deleteTask(id);
    },
    [deleteTask]
  );

  if (boardLoading && !currentBoard) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-primary" />
      </div>
    );
  }

  if (!currentBoard) {
    return (
      <div className="text-center py-12">
        <h2 className="text-xl font-semibold text-text-body">Board not found</h2>
        <Button
          variant="secondary"
          className="mt-4"
          onClick={() => navigate('/boards')}
        >
          Back to Boards
        </Button>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col">
      <div className="flex items-center justify-between mb-6">
        <div>
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/boards')}
              className="p-1 text-text-muted hover:text-text-body rounded-lg hover:bg-bg-hover"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <h1 className="text-2xl font-bold text-text-heading">{currentBoard.name}</h1>
          </div>
          {currentBoard.description && (
            <p className="text-text-meta mt-1 ml-8">{currentBoard.description}</p>
          )}
        </div>

        <Button onClick={handleAddTask}>
          <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Add Task
        </Button>
      </div>

      {taskLoading && (
        <div className="absolute top-4 right-4">
          <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-brand-primary" />
        </div>
      )}

      <div className="flex-1 overflow-hidden">
        <KanbanBoard onEditTask={handleEditTask} onAddTask={handleAddTask} />
      </div>

      <TaskModal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        task={selectedTask}
        boardId={currentBoard.id}
        onSubmit={handleSubmitTask}
        onDelete={handleDeleteTask}
      />
    </div>
  );
}

export default BoardPage;
