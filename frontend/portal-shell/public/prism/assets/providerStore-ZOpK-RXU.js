import { a as api } from './api-Df7DtIvT.js';
import { c as create } from './react-wF4C62jk.js';

const useProviderStore = create((set) => ({
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
        error: error instanceof Error ? error.message : "Failed to fetch providers",
        loading: false
      });
    }
  },
  createProvider: async (data) => {
    set({ loading: true, error: null });
    try {
      const provider = await api.createProvider(data);
      set((state) => ({
        providers: [...state.providers, provider],
        loading: false
      }));
      return provider;
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : "Failed to create provider",
        loading: false
      });
      throw error;
    }
  },
  deleteProvider: async (id) => {
    set({ loading: true, error: null });
    try {
      await api.deleteProvider(id);
      set((state) => ({
        providers: state.providers.filter((p) => p.id !== id),
        loading: false
      }));
    } catch (error) {
      set({
        error: error instanceof Error ? error.message : "Failed to delete provider",
        loading: false
      });
      throw error;
    }
  },
  clearError: () => {
    set({ error: null });
  }
}));

export { useProviderStore as u };
