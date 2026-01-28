import { create } from 'zustand';
import { api } from '@/services/api';
import type { Task, CreateTaskRequest, UpdateTaskRequest, TaskStatus, KanbanColumn } from '@/types';

interface TaskState {
  tasks: Task[];
  columns: KanbanColumn[];
  loading: boolean;
  error: string | null;
  executingTaskIds: Set<number>;

  fetchTasks: (boardId: number) => Promise<void>;
  createTask: (data: CreateTaskRequest) => Promise<Task>;
  updateTask: (id: number, data: UpdateTaskRequest) => Promise<void>;
  moveTask: (id: number, status: TaskStatus, position: number) => Promise<void>;
  assignAgent: (taskId: number, agentId: number) => Promise<void>;
  deleteTask: (id: number) => Promise<void>;
  executeTask: (taskId: number) => Promise<void>;
  clearError: () => void;

  // SSE event handlers
  handleTaskCreated: (task: Task) => void;
  handleTaskUpdated: (task: Task) => void;
  handleTaskMoved: (taskId: number, toStatus: TaskStatus, position: number) => void;
  handleTaskDeleted: (taskId: number) => void;
  handleExecutionStarted: (taskId: number) => void;
  handleExecutionCompleted: (taskId: number) => void;
  handleExecutionFailed: (taskId: number) => void;
}

const COLUMN_CONFIG: { id: TaskStatus; title: string }[] = [
  { id: 'TODO', title: 'To Do' },
  { id: 'IN_PROGRESS', title: 'In Progress' },
  { id: 'IN_REVIEW', title: 'In Review' },
  { id: 'DONE', title: 'Done' },
];

const buildColumns = (tasks: Task[]): KanbanColumn[] => {
  return COLUMN_CONFIG.map((col) => ({
    ...col,
    tasks: tasks
      .filter((t) => t.status === col.id)
      .sort((a, b) => a.position - b.position),
  }));
};

export const useTaskStore = create<TaskState>((set, get) => ({
  tasks: [],
  columns: COLUMN_CONFIG.map((col) => ({ ...col, tasks: [] })),
  loading: false,
  error: null,
  executingTaskIds: new Set<number>(),

  fetchTasks: async (boardId: number) => {
    set({ loading: true, error: null });
    try {
      const tasks = await api.getTasks(boardId);
      set({
        tasks,
        columns: buildColumns(tasks),
        loading: false,
      });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to fetch tasks',
        loading: false,
      });
    }
  },

  createTask: async (data: CreateTaskRequest) => {
    set({ loading: true, error: null });
    try {
      const task = await api.createTask(data);
      set((state) => {
        const newTasks = [...state.tasks, task];
        return {
          tasks: newTasks,
          columns: buildColumns(newTasks),
          loading: false,
        };
      });
      return task;
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to create task',
        loading: false,
      });
      throw error;
    }
  },

  updateTask: async (id: number, data: UpdateTaskRequest) => {
    set({ loading: true, error: null });
    try {
      const updated = await api.updateTask(id, data);
      set((state) => {
        const newTasks = state.tasks.map((t) => (t.id === id ? updated : t));
        return {
          tasks: newTasks,
          columns: buildColumns(newTasks),
          loading: false,
        };
      });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to update task',
        loading: false,
      });
      throw error;
    }
  },

  moveTask: async (id: number, status: TaskStatus, position: number) => {
    // Optimistic update
    set((state) => {
      const task = state.tasks.find((t) => t.id === id);
      if (!task) return state;

      const updatedTask = { ...task, status, position };
      const newTasks = state.tasks.map((t) => (t.id === id ? updatedTask : t));
      return {
        ...state,
        tasks: newTasks,
        columns: buildColumns(newTasks),
      };
    });

    try {
      await api.moveTask(id, { status, position });
    } catch (error) {
      // Revert on error - refetch tasks
      set({
        error: error instanceof Error ? error.message : 'Failed to move task',
      });
    }
  },

  assignAgent: async (taskId: number, agentId: number) => {
    set({ loading: true, error: null });
    try {
      const updated = await api.assignAgent(taskId, agentId);
      set((state) => {
        const newTasks = state.tasks.map((t) => (t.id === taskId ? updated : t));
        return {
          tasks: newTasks,
          columns: buildColumns(newTasks),
          loading: false,
        };
      });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to assign agent',
        loading: false,
      });
      throw error;
    }
  },

  deleteTask: async (id: number) => {
    set({ loading: true, error: null });
    try {
      await api.deleteTask(id);
      set((state) => {
        const newTasks = state.tasks.filter((t) => t.id !== id);
        return {
          tasks: newTasks,
          columns: buildColumns(newTasks),
          loading: false,
        };
      });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to delete task',
        loading: false,
      });
      throw error;
    }
  },

  executeTask: async (taskId: number) => {
    set({ loading: true, error: null });
    try {
      await api.executeTask(taskId);
      set({ loading: false });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to execute task',
        loading: false,
      });
      throw error;
    }
  },

  clearError: () => {
    set({ error: null });
  },

  // SSE Event Handlers
  handleTaskCreated: (task: Task) => {
    set((state) => {
      // Avoid duplicates
      if (state.tasks.some((t) => t.id === task.id)) {
        return state;
      }
      const newTasks = [...state.tasks, task];
      return {
        tasks: newTasks,
        columns: buildColumns(newTasks),
      };
    });
  },

  handleTaskUpdated: (task: Task) => {
    set((state) => {
      const newTasks = state.tasks.map((t) => (t.id === task.id ? task : t));
      return {
        tasks: newTasks,
        columns: buildColumns(newTasks),
      };
    });
  },

  handleTaskMoved: (taskId: number, toStatus: TaskStatus, position: number) => {
    set((state) => {
      const task = state.tasks.find((t) => t.id === taskId);
      if (!task) return state;

      const updatedTask = { ...task, status: toStatus, position };
      const newTasks = state.tasks.map((t) => (t.id === taskId ? updatedTask : t));
      return {
        tasks: newTasks,
        columns: buildColumns(newTasks),
      };
    });
  },

  handleTaskDeleted: (taskId: number) => {
    set((state) => {
      const newTasks = state.tasks.filter((t) => t.id !== taskId);
      return {
        tasks: newTasks,
        columns: buildColumns(newTasks),
      };
    });
  },

  handleExecutionStarted: (taskId: number) => {
    set((state) => {
      const newExecutingIds = new Set(state.executingTaskIds);
      newExecutingIds.add(taskId);
      return { executingTaskIds: newExecutingIds };
    });
  },

  handleExecutionCompleted: (taskId: number) => {
    const { fetchTasks, tasks } = get();
    set((state) => {
      const newExecutingIds = new Set(state.executingTaskIds);
      newExecutingIds.delete(taskId);
      return { executingTaskIds: newExecutingIds };
    });
    // Refetch to get updated task status
    const task = tasks.find((t) => t.id === taskId);
    if (task) {
      void fetchTasks(task.boardId);
    }
  },

  handleExecutionFailed: (taskId: number) => {
    set((state) => {
      const newExecutingIds = new Set(state.executingTaskIds);
      newExecutingIds.delete(taskId);
      return { executingTaskIds: newExecutingIds };
    });
  },
}));
