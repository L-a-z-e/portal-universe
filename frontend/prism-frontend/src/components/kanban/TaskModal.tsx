import { useState, useEffect } from 'react';
import { Modal, Button, Input, Select, Textarea } from '@portal/design-system-react';
import { useAgentStore } from '@/stores/agentStore';
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
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    priority: 'MEDIUM' as TaskPriority,
    agentId: '',
    dueDate: '',
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
        });
      } else {
        setFormData({
          title: '',
          description: '',
          priority: 'MEDIUM',
          agentId: '',
          dueDate: '',
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
      };
      await onSubmit(data);
      onClose();
    } catch (error) {
      console.error('Failed to save task:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!task || !onDelete) return;
    if (!confirm('Are you sure you want to delete this task?')) return;

    setLoading(true);
    try {
      await onDelete(task.id);
      onClose();
    } catch (error) {
      console.error('Failed to delete task:', error);
    } finally {
      setLoading(false);
    }
  };

  const agentOptions = [
    { value: '', label: 'No agent assigned' },
    ...agents.filter((a) => a.isActive).map((a) => ({
      value: a.id.toString(),
      label: `${a.name} (${a.providerName})`,
    })),
  ];

  return (
    <Modal
      open={isOpen}
      onClose={onClose}
      title={task ? 'Edit Task' : 'Create Task'}
      size="md"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Title"
          value={formData.title}
          onChange={(e) => setFormData({ ...formData, title: e.target.value })}
          placeholder="Enter task title"
          required
          autoFocus
        />

        <Textarea
          label="Description"
          value={formData.description}
          onChange={(e) => setFormData({ ...formData, description: e.target.value })}
          placeholder="Enter task description (optional)"
          rows={3}
        />

        <div className="grid grid-cols-2 gap-4">
          <Select
            label="Priority"
            value={formData.priority}
            onChange={(value) =>
              setFormData({ ...formData, priority: value as TaskPriority })
            }
            options={priorityOptions}
          />

          <div className="w-full">
            <label className="block text-sm font-medium text-text-body mb-1">Due Date</label>
            <input
              type="date"
              value={formData.dueDate}
              onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
              className="w-full px-3 py-2 border border-border-default rounded-lg text-text-heading bg-bg-input focus:outline-none focus:ring-2 focus:ring-brand-primary focus:border-transparent"
            />
          </div>
        </div>

        <Select
          label="Assigned Agent"
          value={formData.agentId}
          onChange={(value) => setFormData({ ...formData, agentId: String(value ?? '') })}
          options={agentOptions}
        />

        <div className="flex justify-between pt-4 border-t">
          <div>
            {task && onDelete && (
              <Button
                type="button"
                variant="danger"
                onClick={handleDelete}
                loading={loading}
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
  );
}
