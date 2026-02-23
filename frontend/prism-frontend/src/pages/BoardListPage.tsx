import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Modal, Button, Input, Textarea, Alert, Skeleton, Progress, useApiError } from '@portal/design-react';
import { ConfirmDialog } from '@/components/common/ConfirmDialog';
import { useBoardStore } from '@/stores/boardStore';
import { api } from '@/services/api';
import type { Board, CreateBoardRequest } from '@/types';

interface BoardSummary {
  total: number;
  done: number;
}

function BoardListPage() {
  const navigate = useNavigate();
  const { boards, loading, error, fetchBoards, createBoard, updateBoard, deleteBoard, clearError } = useBoardStore();
  const { handleError } = useApiError();

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editBoard, setEditBoard] = useState<Board | null>(null);
  const [formData, setFormData] = useState<CreateBoardRequest>({ name: '', description: '' });
  const [submitting, setSubmitting] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Board | null>(null);

  // Task summaries per board
  const [summaries, setSummaries] = useState<Record<number, BoardSummary>>({});

  useEffect(() => {
    fetchBoards();
  }, [fetchBoards]);

  // Fetch task counts for each board
  useEffect(() => {
    if (boards.length === 0) return;
    const fetchSummaries = async () => {
      const results = await Promise.allSettled(
        boards.map(async (b) => {
          const tasks = await api.getTasks(b.id);
          return {
            id: b.id,
            total: tasks.length,
            done: tasks.filter((t) => t.status === 'DONE').length,
          };
        })
      );
      const map: Record<number, BoardSummary> = {};
      results.forEach((r) => {
        if (r.status === 'fulfilled') {
          map[r.value.id] = { total: r.value.total, done: r.value.done };
        }
      });
      setSummaries(map);
    };
    fetchSummaries();
  }, [boards]);

  const handleCreateBoard = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) return;

    setSubmitting(true);
    try {
      const board = await createBoard(formData);
      setIsCreateModalOpen(false);
      setFormData({ name: '', description: '' });
      navigate(`/boards/${board.id}`);
    } catch (err) {
      handleError(err, 'Failed to create board');
    } finally {
      setSubmitting(false);
    }
  }, [formData, createBoard, navigate]);

  const handleEditBoard = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editBoard || !formData.name.trim()) return;

    setSubmitting(true);
    try {
      await updateBoard(editBoard.id, formData);
      setEditBoard(null);
    } catch (err) {
      handleError(err, 'Failed to update board');
    } finally {
      setSubmitting(false);
    }
  }, [editBoard, formData, updateBoard]);

  const handleOpenEdit = useCallback((board: Board, e: React.MouseEvent) => {
    e.stopPropagation();
    setFormData({ name: board.name, description: board.description || '' });
    setEditBoard(board);
  }, []);

  const handleDelete = useCallback(async () => {
    if (!deleteTarget) return;
    try {
      await deleteBoard(deleteTarget.id);
      setDeleteTarget(null);
    } catch (err) {
      handleError(err, 'Failed to delete board');
    }
  }, [deleteTarget, deleteBoard, handleError]);

  const getTimeAgo = (dateStr: string) => {
    const diff = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 60) return `${mins}m ago`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-text-heading">My Boards</h1>
        <Button onClick={() => { setFormData({ name: '', description: '' }); setIsCreateModalOpen(true); }}>
          <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          New Board
        </Button>
      </div>

      {error && (
        <Alert variant="error" dismissible onDismiss={clearError} className="mb-4">
          {error}
        </Alert>
      )}

      {loading && boards.length === 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-bg-card rounded-xl shadow-sm border border-border-default p-5">
              <Skeleton variant="text" width="60%" />
              <Skeleton variant="text" width="90%" />
              <Skeleton variant="rect" height="8px" width="100%" />
            </div>
          ))}
        </div>
      ) : boards.length === 0 ? (
        <div className="text-center py-12 bg-bg-subtle rounded-xl">
          <svg className="mx-auto h-12 w-12 text-text-placeholder" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7m0 10a2 2 0 002 2h2a2 2 0 002-2V7a2 2 0 00-2-2h-2a2 2 0 00-2 2" />
          </svg>
          <h3 className="mt-4 text-lg font-medium text-text-heading">No boards yet</h3>
          <p className="mt-2 text-text-body">Create your first board to start managing AI tasks.</p>
          <Button className="mt-4" onClick={() => setIsCreateModalOpen(true)}>
            Create Board
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {boards.map((board) => {
            const summary = summaries[board.id];
            return (
              <div
                key={board.id}
                onClick={() => navigate(`/boards/${board.id}`)}
                className="bg-bg-card rounded-xl shadow-sm border border-border-default p-5 hover:shadow-md transition-shadow cursor-pointer group"
              >
                <div className="flex items-start justify-between">
                  <h3 className="font-semibold text-text-heading group-hover:text-brand-primary">
                    {board.name}
                  </h3>
                  <div className="flex gap-1">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={(e) => handleOpenEdit(board, e)}
                      className="p-1 text-text-meta hover:text-brand-primary"
                    >
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={(e) => { e.stopPropagation(); setDeleteTarget(board); }}
                      className="p-1 text-text-meta hover:text-status-error"
                    >
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </Button>
                  </div>
                </div>
                {board.description && (
                  <p className="mt-2 text-sm text-text-body line-clamp-2">{board.description}</p>
                )}

                {/* B1: Task count + progress */}
                {summary && summary.total > 0 && (
                  <div className="mt-3">
                    <div className="flex items-center justify-between text-xs text-text-meta mb-1">
                      <span>{summary.done}/{summary.total} tasks</span>
                      <span>{Math.round((summary.done / summary.total) * 100)}%</span>
                    </div>
                    <Progress
                      value={summary.done}
                      max={summary.total}
                      size="sm"
                      variant="success"
                    />
                  </div>
                )}
                {summary && summary.total === 0 && (
                  <p className="mt-3 text-xs text-text-muted">No tasks yet</p>
                )}

                <div className="mt-3 text-xs text-text-meta">
                  Last active: {getTimeAgo(board.updatedAt)}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Create Modal */}
      <Modal
        open={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Create New Board"
        size="sm"
      >
        <form onSubmit={handleCreateBoard} className="space-y-4">
          <Input
            label="Board Name"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            placeholder="e.g., Content Creation"
            required
            autoFocus
          />
          <Textarea
            label="Description"
            value={formData.description || ''}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            placeholder="What is this board for? (optional)"
            rows={3}
          />
          <div className="flex justify-end gap-2 pt-4">
            <Button type="button" variant="secondary" onClick={() => setIsCreateModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" loading={submitting}>
              Create Board
            </Button>
          </div>
        </form>
      </Modal>

      {/* Edit Modal */}
      <Modal
        open={!!editBoard}
        onClose={() => setEditBoard(null)}
        title="Edit Board"
        size="sm"
      >
        <form onSubmit={handleEditBoard} className="space-y-4">
          <Input
            label="Board Name"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            required
            autoFocus
          />
          <Textarea
            label="Description"
            value={formData.description || ''}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            rows={3}
          />
          <div className="flex justify-end gap-2 pt-4">
            <Button type="button" variant="secondary" onClick={() => setEditBoard(null)}>
              Cancel
            </Button>
            <Button type="submit" loading={submitting}>
              Update
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirm */}
      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete Board"
        message={`Are you sure you want to delete "${deleteTarget?.name}"? All tasks in this board will be lost.`}
        confirmLabel="Delete"
        variant="danger"
      />
    </div>
  );
}

export default BoardListPage;
