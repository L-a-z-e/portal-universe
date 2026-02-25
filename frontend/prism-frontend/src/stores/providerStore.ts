import { create } from 'zustand';
import { api } from '@/services/api';
import type { Provider, CreateProviderRequest, UpdateProviderRequest } from '@/types';

interface ProviderState {
  providers: Provider[];
  loading: boolean;
  error: string | null;
  verifyingIds: Set<number>;

  fetchProviders: () => Promise<void>;
  createProvider: (data: CreateProviderRequest) => Promise<Provider>;
  updateProvider: (id: number, data: UpdateProviderRequest) => Promise<Provider>;
  deleteProvider: (id: number) => Promise<void>;
  verifyProvider: (id: number) => Promise<{ success: boolean; message?: string; models?: string[] }>;
  clearError: () => void;
}

export const useProviderStore = create<ProviderState>((set, get) => ({
  providers: [],
  loading: false,
  error: null,
  verifyingIds: new Set<number>(),

  fetchProviders: async () => {
    set({ loading: true, error: null });
    try {
      const providers = await api.getProviders();
      set({ providers, loading: false });
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to fetch providers',
        loading: false,
      });
    }
  },

  createProvider: async (data: CreateProviderRequest) => {
    set({ loading: true, error: null });
    try {
      const provider = await api.createProvider(data);
      set((state) => ({
        providers: [...state.providers, provider],
        loading: false,
      }));
      return provider;
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to create provider',
        loading: false,
      });
      throw error;
    }
  },

  updateProvider: async (id: number, data: UpdateProviderRequest) => {
    set({ error: null });
    try {
      const updated = await api.updateProvider(id, data);
      set((state) => ({
        providers: state.providers.map((p) => (p.id === id ? { ...p, ...updated } : p)),
      }));
      return updated;
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to update provider',
      });
      throw error;
    }
  },

  deleteProvider: async (id: number) => {
    set({ loading: true, error: null });
    try {
      await api.deleteProvider(id);
      set((state) => ({
        providers: state.providers.filter((p) => p.id !== id),
        loading: false,
      }));
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : 'Failed to delete provider',
        loading: false,
      });
      throw error;
    }
  },

  verifyProvider: async (id: number) => {
    const { verifyingIds } = get();
    const newIds = new Set(verifyingIds);
    newIds.add(id);
    set({ verifyingIds: newIds });

    try {
      const result = await api.verifyProvider(id);
      return result;
    } finally {
      set((state) => {
        const ids = new Set(state.verifyingIds);
        ids.delete(id);
        return { verifyingIds: ids };
      });
    }
  },

  clearError: () => {
    set({ error: null });
  },
}));
