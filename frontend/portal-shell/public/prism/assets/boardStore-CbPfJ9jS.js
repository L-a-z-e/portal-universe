import { a as api } from './api-Df7DtIvT.js';
import { c as create } from './react-wF4C62jk.js';

const useBoardStore = create((set) => ({
  boards: [],
  currentBoard: null,
  loading: false,
  error: null,
  fetchBoards: async () => {
    set({ loading: true, error: null });
    try {
      const boards = await api.getBoards();
      set({ boards, loading: false });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : "Failed to fetch boards",
        loading: false
      });
    }
  },
  fetchBoard: async (id) => {
    set({ loading: true, error: null });
    try {
      const board = await api.getBoard(id);
      set({ currentBoard: board, loading: false });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : "Failed to fetch board",
        loading: false
      });
    }
  },
  createBoard: async (data) => {
    set({ loading: true, error: null });
    try {
      const board = await api.createBoard(data);
      set((state) => ({
        boards: [...state.boards, board],
        loading: false
      }));
      return board;
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : "Failed to create board",
        loading: false
      });
      throw error;
    }
  },
  updateBoard: async (id, data) => {
    set({ loading: true, error: null });
    try {
      const updated = await api.updateBoard(id, data);
      set((state) => ({
        boards: state.boards.map((b) => b.id === id ? updated : b),
        currentBoard: state.currentBoard?.id === id ? updated : state.currentBoard,
        loading: false
      }));
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : "Failed to update board",
        loading: false
      });
      throw error;
    }
  },
  deleteBoard: async (id) => {
    set({ loading: true, error: null });
    try {
      await api.deleteBoard(id);
      set((state) => ({
        boards: state.boards.filter((b) => b.id !== id),
        currentBoard: state.currentBoard?.id === id ? null : state.currentBoard,
        loading: false
      }));
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : "Failed to delete board",
        loading: false
      });
      throw error;
    }
  },
  setCurrentBoard: (board) => {
    set({ currentBoard: board });
  },
  clearError: () => {
    set({ error: null });
  }
}));

export { useBoardStore as u };
