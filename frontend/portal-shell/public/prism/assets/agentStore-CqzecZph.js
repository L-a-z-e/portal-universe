import { a as api } from './api-Df7DtIvT.js';
import { c as create } from './react-wF4C62jk.js';

const useAgentStore = create((set) => ({
  agents: [],
  loading: false,
  error: null,
  fetchAgents: async () => {
    set({ loading: true, error: null });
    try {
      const agents = await api.getAgents();
      set({ agents, loading: false });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : "Failed to fetch agents",
        loading: false
      });
    }
  },
  createAgent: async (data) => {
    set({ loading: true, error: null });
    try {
      const agent = await api.createAgent(data);
      set((state) => ({
        agents: [...state.agents, agent],
        loading: false
      }));
      return agent;
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : "Failed to create agent",
        loading: false
      });
      throw error;
    }
  },
  updateAgent: async (id, data) => {
    set({ loading: true, error: null });
    try {
      const updated = await api.updateAgent(id, data);
      set((state) => ({
        agents: state.agents.map((a) => a.id === id ? updated : a),
        loading: false
      }));
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : "Failed to update agent",
        loading: false
      });
      throw error;
    }
  },
  deleteAgent: async (id) => {
    set({ loading: true, error: null });
    try {
      await api.deleteAgent(id);
      set((state) => ({
        agents: state.agents.filter((a) => a.id !== id),
        loading: false
      }));
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : "Failed to delete agent",
        loading: false
      });
      throw error;
    }
  },
  clearError: () => {
    set({ error: null });
  }
}));

export { useAgentStore as u };
