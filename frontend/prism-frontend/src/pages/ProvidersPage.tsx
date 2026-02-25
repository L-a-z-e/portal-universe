import { useEffect, useState, useCallback } from 'react';
import { Modal, Button, Input, Select, Alert, Skeleton, Tag, useApiError, useToast } from '@portal/design-react';
import { ConfirmDialog } from '@/components/common/ConfirmDialog';
import { useProviderStore } from '@/stores/providerStore';
import { api } from '@/services/api';
import type { CreateProviderRequest, UpdateProviderRequest, ProviderType, Provider } from '@/types';

const providerTypeOptions = [
  { value: 'OPENAI', label: 'OpenAI' },
  { value: 'ANTHROPIC', label: 'Anthropic' },
  { value: 'GOOGLE', label: 'Google AI' },
  { value: 'OLLAMA', label: 'Ollama' },
  { value: 'LOCAL', label: 'Local/Custom' },
];

const requiresApiKey = (type: ProviderType): boolean => {
  return !['OLLAMA', 'LOCAL'].includes(type);
};

function ProvidersPage() {
  const { providers, loading, error, fetchProviders, createProvider, updateProvider, deleteProvider, verifyProvider, verifyingIds, clearError } = useProviderStore();
  const { handleError } = useApiError();
  const toast = useToast();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedProvider, setSelectedProvider] = useState<Provider | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formData, setFormData] = useState<CreateProviderRequest>({
    name: '',
    type: 'OPENAI',
    apiKey: '',
    baseUrl: '',
  });

  // Delete confirm dialog
  const [deleteTarget, setDeleteTarget] = useState<Provider | null>(null);

  // Models preview per provider
  const [modelsMap, setModelsMap] = useState<Record<number, string[]>>({});
  const [modelsLoadingIds, setModelsLoadingIds] = useState<Set<number>>(new Set());

  useEffect(() => {
    fetchProviders();
  }, [fetchProviders]);

  // Lazy load models for each provider
  useEffect(() => {
    let cancelled = false;
    providers.forEach((p) => {
      if (p.isActive && !modelsMap[p.id] && !modelsLoadingIds.has(p.id)) {
        setModelsLoadingIds((prev) => new Set(prev).add(p.id));
        api.getProviderModels(p.id)
          .then((models) => {
            if (!cancelled) setModelsMap((prev) => ({ ...prev, [p.id]: models }));
          })
          .catch(() => {
            if (!cancelled) setModelsMap((prev) => ({ ...prev, [p.id]: [] }));
          })
          .finally(() => {
            if (!cancelled) setModelsLoadingIds((prev) => {
              const next = new Set(prev);
              next.delete(p.id);
              return next;
            });
          });
      }
    });
    return () => { cancelled = true; };
  }, [providers]);

  const handleOpenModal = useCallback((provider?: Provider) => {
    if (provider) {
      setSelectedProvider(provider);
      setFormData({
        name: provider.name,
        type: provider.type,
        apiKey: '',
        baseUrl: provider.baseUrl || '',
      });
    } else {
      setSelectedProvider(null);
      setFormData({ name: '', type: 'OPENAI', apiKey: '', baseUrl: '' });
    }
    setIsModalOpen(true);
  }, []);

  const handleSubmit = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) return;
    if (!selectedProvider && requiresApiKey(formData.type) && !formData.apiKey?.trim()) return;

    setSubmitting(true);
    try {
      if (selectedProvider) {
        const updateData: UpdateProviderRequest = {
          name: formData.name,
          baseUrl: formData.baseUrl || undefined,
        };
        if (formData.apiKey?.trim()) {
          updateData.apiKey = formData.apiKey;
        }
        await updateProvider(selectedProvider.id, updateData);
      } else {
        await createProvider({
          ...formData,
          apiKey: formData.apiKey || undefined,
          baseUrl: formData.baseUrl || undefined,
        });
      }
      setIsModalOpen(false);
    } catch (err) {
      handleError(err, `Failed to ${selectedProvider ? 'update' : 'create'} provider`);
    } finally {
      setSubmitting(false);
    }
  }, [formData, selectedProvider, createProvider, updateProvider]);

  const handleDelete = useCallback(async () => {
    if (!deleteTarget) return;
    try {
      await deleteProvider(deleteTarget.id);
      setDeleteTarget(null);
    } catch (err) {
      handleError(err, 'Failed to delete provider');
    }
  }, [deleteTarget, deleteProvider, handleError]);

  const handleVerify = useCallback(async (provider: Provider) => {
    try {
      const result = await verifyProvider(provider.id);
      if (result.success) {
        toast.success(`${provider.name}: Connection successful`);
      } else {
        toast.error(`${provider.name}: ${result.message || 'Connection failed'}`);
      }
    } catch (err) {
      handleError(err, 'Connection test failed');
    }
  }, [verifyProvider, toast]);

  const getProviderIcon = (type: ProviderType) => {
    switch (type) {
      case 'OPENAI': return '🟢';
      case 'ANTHROPIC': return '🟠';
      case 'GOOGLE': return '🔵';
      case 'OLLAMA': return '🦙';
      case 'LOCAL': return '💻';
      default: return '⚪';
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-text-heading">AI Providers</h1>
        <Button onClick={() => handleOpenModal()}>
          <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Add Provider
        </Button>
      </div>

      {error && (
        <Alert variant="error" dismissible onDismiss={clearError} className="mb-4">
          {error}
        </Alert>
      )}

      {loading && providers.length === 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-bg-card rounded-xl shadow-sm border border-border-default p-5">
              <div className="flex items-center gap-3 mb-3">
                <Skeleton variant="circular" width="40px" height="40px" />
                <div className="flex-1">
                  <Skeleton variant="text" width="60%" />
                  <Skeleton variant="text" width="30%" />
                </div>
              </div>
              <Skeleton variant="text" width="80%" />
              <Skeleton variant="text" width="50%" />
            </div>
          ))}
        </div>
      ) : providers.length === 0 ? (
        <div className="text-center py-12 bg-bg-subtle rounded-xl">
          <svg className="mx-auto h-12 w-12 text-text-placeholder" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01" />
          </svg>
          <h3 className="mt-4 text-lg font-medium text-text-heading">No providers yet</h3>
          <p className="mt-2 text-text-body">Add an AI provider to start using agents.</p>
          <Button className="mt-4" onClick={() => handleOpenModal()}>
            Add Provider
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {providers.map((provider) => {
            const models = modelsMap[provider.id];
            const isVerifying = verifyingIds.has(provider.id);
            return (
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
                  <div className="flex items-center gap-1">
                    <span className={`px-2 py-0.5 text-xs rounded-full ${provider.isActive ? 'bg-status-success/10 text-status-success' : 'bg-bg-muted text-text-meta'}`}>
                      {provider.isActive ? 'Active' : 'Inactive'}
                    </span>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleOpenModal(provider)}
                      className="p-1 text-text-muted hover:text-brand-primary"
                    >
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setDeleteTarget(provider)}
                      className="p-1 text-text-muted hover:text-status-error"
                    >
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </Button>
                  </div>
                </div>

                {provider.baseUrl && (
                  <p className="mt-3 text-xs text-text-muted truncate">
                    Base URL: {provider.baseUrl}
                  </p>
                )}

                {/* P3: Models preview */}
                {models && models.length > 0 && (
                  <div className="mt-3 flex flex-wrap gap-1">
                    {models.slice(0, 3).map((m) => (
                      <Tag key={m} size="sm">{m}</Tag>
                    ))}
                    {models.length > 3 && (
                      <Tag size="sm" variant="default">+{models.length - 3} more</Tag>
                    )}
                  </div>
                )}

                <div className="mt-3 flex items-center justify-between">
                  <span className="text-xs text-text-muted">
                    Added {new Date(provider.createdAt).toLocaleDateString()}
                  </span>
                  <Button
                    variant="ghost"
                    size="xs"
                    onClick={() => handleVerify(provider)}
                    loading={isVerifying}
                    className="text-xs"
                  >
                    Test
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Create/Edit Modal */}
      <Modal
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={selectedProvider ? 'Edit Provider' : 'Add AI Provider'}
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

          {!selectedProvider && (
            <Select
              label="Provider Type"
              value={formData.type}
              onChange={(value) => setFormData({ ...formData, type: value as ProviderType })}
              options={providerTypeOptions}
            />
          )}

          <div>
            <Input
              label={selectedProvider
                ? `API Key${requiresApiKey(formData.type) ? '' : ' (Optional)'}`
                : `API Key${requiresApiKey(formData.type) ? '' : ' (Optional)'}`
              }
              type="password"
              value={formData.apiKey}
              onChange={(e) => setFormData({ ...formData, apiKey: e.target.value })}
              placeholder={selectedProvider
                ? 'Enter new key to change'
                : (requiresApiKey(formData.type) ? 'sk-...' : 'Not required for this provider')
              }
              required={!selectedProvider && requiresApiKey(formData.type)}
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
              {selectedProvider ? 'Update' : 'Add Provider'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirm */}
      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete Provider"
        message={`Are you sure you want to delete "${deleteTarget?.name}"? This action cannot be undone.`}
        confirmLabel="Delete"
        variant="danger"
      />
    </div>
  );
}

export default ProvidersPage;
