import { create } from 'zustand';
import { api } from '@/services/api';
import type { Agent, CreateAgentRequest } from '@/types';

interface AgentState {
  agents: Agent[];
  loading: boolean;
  error: string | null;

  fetchAgents: () => Promise<void>;
  createAgent: (data: CreateAgentRequest) => Promise<Agent>;
  updateAgent: (id: number, data: Partial<CreateAgentRequest>) => Promise<void>;
  deleteAgent: (id: number) => Promise<void>;
  clearError: () => void;
}

export const useAgentStore = create<AgentState>((set) => ({
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
        error: error instanceof Error ? error.message : 'Failed to fetch agents',
        loading: false,
      });
    }
  },

  createAgent: async (data: CreateAgentRequest) => {
    set({ loading: true, error: null });
    try {
      const agent = await api.createAgent(data);
      set((state) => ({
        agents: [...state.agents, agent],
        loading: false,
      }));
      return agent;
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to create agent',
        loading: false,
      });
      throw error;
    }
  },

  updateAgent: async (id: number, data: Partial<CreateAgentRequest>) => {
    set({ loading: true, error: null });
    try {
      const updated = await api.updateAgent(id, data);
      set((state) => ({
        agents: state.agents.map((a) => (a.id === id ? updated : a)),
        loading: false,
      }));
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to update agent',
        loading: false,
      });
      throw error;
    }
  },

  deleteAgent: async (id: number) => {
    set({ loading: true, error: null });
    try {
      await api.deleteAgent(id);
      set((state) => ({
        agents: state.agents.filter((a) => a.id !== id),
        loading: false,
      }));
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to delete agent',
        loading: false,
      });
      throw error;
    }
  },

  clearError: () => {
    set({ error: null });
  },
}));
