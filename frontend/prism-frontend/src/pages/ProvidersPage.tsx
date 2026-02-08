import { useEffect, useState, useCallback } from 'react';
import { Modal, Button, Input, Select } from '@portal/design-system-react';
import { useProviderStore } from '@/stores/providerStore';
import type { CreateProviderRequest, ProviderType } from '@/types';

const providerTypeOptions = [
  { value: 'OPENAI', label: 'OpenAI' },
  { value: 'ANTHROPIC', label: 'Anthropic' },
  { value: 'GOOGLE', label: 'Google AI' },
  { value: 'OLLAMA', label: 'Ollama' },
  { value: 'LOCAL', label: 'Local/Custom' },
];

// API Key가 필수인지 판단 (OLLAMA, LOCAL은 필요 없음)
const requiresApiKey = (type: ProviderType): boolean => {
  return !['OLLAMA', 'LOCAL'].includes(type);
};

function ProvidersPage() {
  const { providers, loading, error, fetchProviders, createProvider, deleteProvider } = useProviderStore();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formData, setFormData] = useState<CreateProviderRequest>({
    name: '',
    type: 'OPENAI',
    apiKey: '',
    baseUrl: '',
  });

  useEffect(() => {
    fetchProviders();
  }, [fetchProviders]);

  const handleOpenModal = useCallback(() => {
    setFormData({ name: '', type: 'OPENAI', apiKey: '', baseUrl: '' });
    setIsModalOpen(true);
  }, []);

  const handleSubmit = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    // API Key는 OLLAMA/LOCAL이 아닌 경우에만 필수
    if (!formData.name.trim()) return;
    if (requiresApiKey(formData.type) && !formData.apiKey?.trim()) return;

    setSubmitting(true);
    try {
      await createProvider({
        ...formData,
        apiKey: formData.apiKey || undefined,
        baseUrl: formData.baseUrl || undefined,
      });
      setIsModalOpen(false);
    } catch (error) {
      console.error('Failed to create provider:', error);
    } finally {
      setSubmitting(false);
    }
  }, [formData, createProvider]);

  const handleDelete = useCallback(async (id: number) => {
    if (!confirm('Are you sure you want to delete this provider?')) return;
    await deleteProvider(id);
  }, [deleteProvider]);

  const getProviderIcon = (type: ProviderType) => {
    switch (type) {
      case 'OPENAI':
        return '🟢';
      case 'ANTHROPIC':
        return '🟠';
      case 'GOOGLE':
        return '🔵';
      case 'OLLAMA':
        return '🦙';
      default:
        return '⚪';
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-text-heading">AI Providers</h1>
        <Button onClick={handleOpenModal}>
          <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Add Provider
        </Button>
      </div>

      {error && (
        <div className="mb-4 p-4 bg-status-error/10 text-status-error rounded-lg">{error}</div>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-primary" />
        </div>
      ) : providers.length === 0 ? (
        <div className="text-center py-12 bg-bg-subtle rounded-xl">
          <svg className="mx-auto h-12 w-12 text-text-placeholder" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01" />
          </svg>
          <h3 className="mt-4 text-lg font-medium text-text-heading">No providers yet</h3>
          <p className="mt-2 text-text-body">Add an AI provider to start using agents.</p>
          <Button className="mt-4" onClick={handleOpenModal}>
            Add Provider
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {providers.map((provider) => (
            <div
              key={provider.id}
              className="bg-bg-card rounded-xl shadow-sm border border-border-default p-5"
            >
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <span className="text-2xl">{getProviderIcon(provider.type)}</span>
                  <div>
                    <h3 className="font-semibold text-text-heading">{provider.name}</h3>
                    <p className="text-sm text-text-meta">{provider.type}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <span className={`px-2 py-0.5 text-xs rounded-full ${provider.isActive ? 'bg-status-success/10 text-status-success' : 'bg-bg-muted text-text-meta'}`}>
                    {provider.isActive ? 'Active' : 'Inactive'}
                  </span>
                  <button
                    onClick={() => handleDelete(provider.id)}
                    className="p-1 text-text-muted hover:text-status-error rounded-lg hover:bg-bg-hover"
                  >
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </div>
              {provider.baseUrl && (
                <p className="mt-3 text-xs text-text-muted truncate">
                  Base URL: {provider.baseUrl}
                </p>
              )}
              <div className="mt-3 text-xs text-text-muted">
                Added {new Date(provider.createdAt).toLocaleDateString()}
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Add AI Provider"
        size="sm"
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Provider Name"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            placeholder="e.g., My OpenAI Account"
            required
            autoFocus
          />

          <Select
            label="Provider Type"
            value={formData.type}
            onChange={(value) => setFormData({ ...formData, type: value as ProviderType })}
            options={providerTypeOptions}
          />

          <div>
            <Input
              label={`API Key${requiresApiKey(formData.type) ? '' : ' (Optional)'}`}
              type="password"
              value={formData.apiKey}
              onChange={(e) => setFormData({ ...formData, apiKey: e.target.value })}
              placeholder={requiresApiKey(formData.type) ? 'sk-...' : 'Not required for this provider'}
              required={requiresApiKey(formData.type)}
            />
            {!requiresApiKey(formData.type) && (
              <p className="mt-1 text-sm text-text-meta">
                {formData.type === 'OLLAMA' ? 'Ollama runs locally without API key' : 'Local provider may not require API key'}
              </p>
            )}
          </div>

          <div>
            <Input
              label="Base URL (Optional)"
              value={formData.baseUrl || ''}
              onChange={(e) => setFormData({ ...formData, baseUrl: e.target.value })}
              placeholder="https://api.openai.com/v1"
            />
            <p className="mt-1 text-sm text-text-meta">Leave empty to use default provider URL</p>
          </div>

          <div className="flex justify-end gap-2 pt-4 border-t border-border-default">
            <Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" loading={submitting}>
              Add Provider
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

export default ProvidersPage;
