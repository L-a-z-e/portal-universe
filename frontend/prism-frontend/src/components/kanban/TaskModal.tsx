import { useState, useEffect, useMemo } from 'react';
import { Modal, Button, Input, Select, Textarea, Checkbox, useApiError } from '@portal/design-react';
import { ConfirmDialog } from '@/components/common/ConfirmDialog';
import { useAgentStore } from '@/stores/agentStore';
import { useTaskStore } from '@/stores/taskStore';
import type { Task, CreateTaskRequest, UpdateTaskRequest, TaskPriority } from '@/types';

interface TaskModalProps {
  isOpen: boolean;
  onClose: () => void;
  task?: Task | null;
  boardId: number;
  onSubmit: (data: CreateTaskRequest | UpdateTaskRequest) => Promise<void>;
  onDelete?: (id: number) => Promise<void>;
}

const priorityOptions = [
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
  { value: 'URGENT', label: 'Urgent' },
];

export function TaskModal({
  isOpen,
  onClose,
  task,
  boardId,
  onSubmit,
  onDelete,
}: TaskModalProps) {
  const { agents, fetchAgents } = useAgentStore();
  const { tasks } = useTaskStore();
  const { handleError } = useApiError();
  const [loading, setLoading] = useState(false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    priority: 'MEDIUM' as TaskPriority,
    agentId: '',
    dueDate: '',
    referencedTaskIds: [] as number[],
  });

  useEffect(() => {
    if (isOpen) {
      fetchAgents();
      if (task) {
        setFormData({
          title: task.title,
          description: task.description || '',
          priority: task.priority,
          agentId: task.agentId?.toString() || '',
          dueDate: task.dueDate?.split('T')[0] || '',
          referencedTaskIds: task.referencedTaskIds || [],
        });
      } else {
        setFormData({
          title: '',
          description: '',
          priority: 'MEDIUM',
          agentId: '',
          dueDate: '',
          referencedTaskIds: [],
        });
      }
    }
  }, [isOpen, task, fetchAgents]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.title.trim()) return;

    setLoading(true);
    try {
      const data = {
        ...(task ? {} : { boardId }),
        title: formData.title,
        description: formData.description || undefined,
        priority: formData.priority,
        agentId: formData.agentId ? parseInt(formData.agentId) : undefined,
        dueDate: formData.dueDate || undefined,
        referencedTaskIds: formData.referencedTaskIds.length > 0 ? formData.referencedTaskIds : undefined,
      };
      await onSubmit(data);
      onClose();
    } catch (err) {
      handleError(err, 'Failed to save task');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!task || !onDelete) return;

    setLoading(true);
    try {
      await onDelete(task.id);
      onClose();
    } catch (err) {
      handleError(err, 'Failed to delete task');
    } finally {
      setLoading(false);
    }
  };

  // M3: Agent options with model info
  const agentOptions = useMemo(() => [
    { value: '', label: 'No agent assigned' },
    ...agents.filter((a) => a.isActive).map((a) => ({
      value: a.id.toString(),
      label: `${a.name} — ${a.model}`,
    })),
  ], [agents]);

  const selectedAgent = useMemo(() => {
    if (!formData.agentId) return null;
    return agents.find((a) => a.id.toString() === formData.agentId) ?? null;
  }, [agents, formData.agentId]);

  const doneTasks = useMemo(
    () => tasks.filter((t) => t.id !== task?.id && t.status === 'DONE'),
    [tasks, task?.id]
  );

  // Auto-open advanced if there are existing values
  const hasAdvancedValues = !!(formData.dueDate || formData.referencedTaskIds.length > 0);

  return (
    <>
      <Modal
        open={isOpen}
        onClose={onClose}
        title={task ? 'Edit Task' : 'Create Task'}
        size="md"
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Row 1: Title */}
          <Input
            label="Title"
            value={formData.title}
            onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            placeholder="Enter task title"
            required
            autoFocus
          />

          {/* Row 2: Description */}
          <Textarea
            label="Description"
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            placeholder="Enter task description (optional)"
            rows={3}
          />

          {/* Row 3: Priority + Agent */}
          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Priority"
              value={formData.priority}
              onChange={(value) => setFormData({ ...formData, priority: value as TaskPriority })}
              options={priorityOptions}
            />
            <div>
              <Select
                label="Assigned Agent"
                value={formData.agentId}
                onChange={(value) => setFormData({ ...formData, agentId: String(value ?? '') })}
                options={agentOptions}
              />
              {/* M3: Provider/Model info */}
              {selectedAgent && (
                <p className="mt-1 text-xs text-text-meta">
                  {selectedAgent.providerName} / {selectedAgent.model}
                </p>
              )}
            </div>
          </div>

          {/* M1: Advanced options */}
          <details open={hasAdvancedValues || undefined} className="group">
            <summary className="text-sm font-medium text-text-meta cursor-pointer hover:text-text-body select-none">
              Advanced options
            </summary>
            <div className="mt-3 space-y-4 pl-1">
              <Input
                label="Due Date"
                type="date"
                value={formData.dueDate}
                onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
              />

              {/* M2: Referenced Tasks */}
              <div>
                <label className="block text-sm font-medium text-text-body mb-2">
                  Referenced Tasks
                </label>
                <p className="text-xs text-text-meta mb-2">
                  Select completed tasks whose results can be referenced by the AI agent
                </p>
                <div className="border border-border-default rounded-lg max-h-40 overflow-y-auto">
                  {doneTasks.map((t) => (
                    <div
                      key={t.id}
                      className="px-3 py-2 hover:bg-bg-hover border-b border-border-default last:border-b-0"
                    >
                      <Checkbox
                        size="sm"
                        label={t.title}
                        checked={formData.referencedTaskIds.includes(t.id)}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setFormData({
                              ...formData,
                              referencedTaskIds: [...formData.referencedTaskIds, t.id],
                            });
                          } else {
                            setFormData({
                              ...formData,
                              referencedTaskIds: formData.referencedTaskIds.filter((id) => id !== t.id),
                            });
                          }
                        }}
                      />
                    </div>
                  ))}
                  {doneTasks.length === 0 && (
                    <p className="px-3 py-4 text-sm text-text-meta text-center">
                      No completed tasks available to reference
                    </p>
                  )}
                </div>
              </div>
            </div>
          </details>

          <div className="flex justify-between pt-4 border-t">
            <div>
              {task && onDelete && (
                <Button
                  type="button"
                  variant="danger"
                  onClick={() => setDeleteConfirmOpen(true)}
                >
                  Delete
                </Button>
              )}
            </div>
            <div className="flex gap-2">
              <Button type="button" variant="secondary" onClick={onClose}>
                Cancel
              </Button>
              <Button type="submit" loading={loading}>
                {task ? 'Update' : 'Create'}
              </Button>
            </div>
          </div>
        </form>
      </Modal>

      {/* C1: Delete confirmation */}
      <ConfirmDialog
        open={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleDelete}
        title="Delete Task"
        message={`Are you sure you want to delete "${task?.title}"?`}
        confirmLabel="Delete"
        variant="danger"
      />
    </>
  );
}
