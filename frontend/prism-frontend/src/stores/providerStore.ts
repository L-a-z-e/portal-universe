import { create } from 'zustand';
import { api } from '@/services/api';
import type { Provider, CreateProviderRequest } from '@/types';

interface ProviderState {
  providers: Provider[];
  loading: boolean;
  error: string | null;

  fetchProviders: () => Promise<void>;
  createProvider: (data: CreateProviderRequest) => Promise<Provider>;
  deleteProvider: (id: number) => Promise<void>;
  clearError: () => void;
}

export const useProviderStore = create<ProviderState>((set) => ({
  providers: [],
  loading: false,
  error: null,

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

  clearError: () => {
    set({ error: null });
  },
}));
