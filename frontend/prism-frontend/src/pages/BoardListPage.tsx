import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Modal, Button, Input, Textarea } from '@/components/common';
import { useBoardStore } from '@/stores/boardStore';
import type { CreateBoardRequest } from '@/types';

function BoardListPage() {
  const navigate = useNavigate();
  const { boards, loading, error, fetchBoards, createBoard, deleteBoard } = useBoardStore();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState<CreateBoardRequest>({ name: '', description: '' });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchBoards();
  }, [fetchBoards]);

  const handleCreateBoard = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) return;

    setSubmitting(true);
    try {
      const board = await createBoard(formData);
      setIsModalOpen(false);
      setFormData({ name: '', description: '' });
      navigate(`/boards/${board.id}`);
    } catch (error) {
      console.error('Failed to create board:', error);
    } finally {
      setSubmitting(false);
    }
  }, [formData, createBoard, navigate]);

  const handleDeleteBoard = useCallback(
    async (id: number, e: React.MouseEvent) => {
      e.stopPropagation();
      if (!confirm('Are you sure you want to delete this board?')) return;
      await deleteBoard(id);
    },
    [deleteBoard]
  );

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-text-heading">My Boards</h1>
        <Button onClick={() => setIsModalOpen(true)}>
          <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          New Board
        </Button>
      </div>

      {error && (
        <div className="mb-4 p-4 bg-status-error/10 text-status-error rounded-lg">
          {error}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-primary" />
        </div>
      ) : boards.length === 0 ? (
        <div className="text-center py-12 bg-bg-subtle rounded-xl">
          <svg
            className="mx-auto h-12 w-12 text-text-placeholder"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7m0 10a2 2 0 002 2h2a2 2 0 002-2V7a2 2 0 00-2-2h-2a2 2 0 00-2 2"
            />
          </svg>
          <h3 className="mt-4 text-lg font-medium text-text-heading">No boards yet</h3>
          <p className="mt-2 text-text-body">Create your first board to start managing AI tasks.</p>
          <Button className="mt-4" onClick={() => setIsModalOpen(true)}>
            Create Board
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {boards.map((board) => (
            <div
              key={board.id}
              onClick={() => navigate(`/boards/${board.id}`)}
              className="bg-bg-card rounded-xl shadow-sm border border-border-default p-5 hover:shadow-md transition-shadow cursor-pointer group"
            >
              <div className="flex items-start justify-between">
                <h3 className="font-semibold text-text-heading group-hover:text-brand-primary">
                  {board.name}
                </h3>
                <button
                  onClick={(e) => handleDeleteBoard(board.id, e)}
                  className="opacity-0 group-hover:opacity-100 p-1 text-text-meta hover:text-status-error rounded transition-all"
                >
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                    />
                  </svg>
                </button>
              </div>
              {board.description && (
                <p className="mt-2 text-sm text-text-body line-clamp-2">{board.description}</p>
              )}
              <div className="mt-4 text-xs text-text-meta">
                Created {new Date(board.createdAt).toLocaleDateString()}
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
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
            <Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" loading={submitting}>
              Create Board
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

export default BoardListPage;
