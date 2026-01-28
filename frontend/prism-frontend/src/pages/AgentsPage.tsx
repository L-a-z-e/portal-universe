import { useEffect, useState, useCallback } from 'react';
import { Modal, Button, Input, Select, Textarea } from '@/components/common';
import { useAgentStore } from '@/stores/agentStore';
import { useProviderStore } from '@/stores/providerStore';
import type { Agent, CreateAgentRequest } from '@/types';

function AgentsPage() {
  const { agents, loading, error, fetchAgents, createAgent, updateAgent, deleteAgent } = useAgentStore();
  const { providers, fetchProviders } = useProviderStore();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedAgent, setSelectedAgent] = useState<Agent | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formData, setFormData] = useState<CreateAgentRequest>({
    name: '',
    description: '',
    providerId: 0,
    model: 'gpt-4o',
    systemPrompt: '',
    temperature: 0.7,
    maxTokens: 4096,
  });

  useEffect(() => {
    fetchAgents();
    fetchProviders();
  }, [fetchAgents, fetchProviders]);

  const handleOpenModal = useCallback((agent?: Agent) => {
    if (agent) {
      setSelectedAgent(agent);
      setFormData({
        name: agent.name,
        description: agent.description || '',
        providerId: agent.providerId,
        model: agent.model,
        systemPrompt: agent.systemPrompt,
        temperature: agent.temperature,
        maxTokens: agent.maxTokens,
      });
    } else {
      setSelectedAgent(null);
      setFormData({
        name: '',
        description: '',
        providerId: providers[0]?.id || 0,
        model: 'gpt-4o',
        systemPrompt: 'You are a helpful AI assistant.',
        temperature: 0.7,
        maxTokens: 4096,
      });
    }
    setIsModalOpen(true);
  }, [providers]);

  const handleSubmit = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.providerId) return;

    setSubmitting(true);
    try {
      if (selectedAgent) {
        await updateAgent(selectedAgent.id, formData);
      } else {
        await createAgent(formData);
      }
      setIsModalOpen(false);
    } catch (error) {
      console.error('Failed to save agent:', error);
    } finally {
      setSubmitting(false);
    }
  }, [formData, selectedAgent, createAgent, updateAgent]);

  const handleDelete = useCallback(async (id: number) => {
    if (!confirm('Are you sure you want to delete this agent?')) return;
    await deleteAgent(id);
  }, [deleteAgent]);

  const providerOptions = providers
    .filter((p) => p.isActive)
    .map((p) => ({ value: p.id, label: `${p.name} (${p.type})` }));

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">AI Agents</h1>
        <Button onClick={() => handleOpenModal()}>
          <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          New Agent
        </Button>
      </div>

      {error && (
        <div className="mb-4 p-4 bg-red-50 text-red-600 rounded-lg">{error}</div>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-prism-500" />
        </div>
      ) : agents.length === 0 ? (
        <div className="text-center py-12 bg-gray-50 rounded-xl">
          <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
          </svg>
          <h3 className="mt-4 text-lg font-medium text-gray-900">No agents yet</h3>
          <p className="mt-2 text-gray-500">Create your first AI agent to automate tasks.</p>
          <Button className="mt-4" onClick={() => handleOpenModal()}>
            Create Agent
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {agents.map((agent) => (
            <div
              key={agent.id}
              className="bg-white rounded-xl shadow-sm border border-gray-200 p-5"
            >
              <div className="flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-semibold text-gray-900">{agent.name}</h3>
                    <span className={`px-2 py-0.5 text-xs rounded-full ${agent.isActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                      {agent.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                  <p className="text-sm text-gray-500 mt-1">{agent.providerName} / {agent.model}</p>
                </div>
                <div className="flex gap-1">
                  <button
                    onClick={() => handleOpenModal(agent)}
                    className="p-2 text-gray-400 hover:text-prism-600 rounded-lg hover:bg-gray-100"
                  >
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                  </button>
                  <button
                    onClick={() => handleDelete(agent.id)}
                    className="p-2 text-gray-400 hover:text-red-500 rounded-lg hover:bg-gray-100"
                  >
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </div>
              {agent.description && (
                <p className="mt-3 text-sm text-gray-600">{agent.description}</p>
              )}
              <div className="mt-4 flex gap-4 text-xs text-gray-500">
                <span>Temp: {agent.temperature}</span>
                <span>Max Tokens: {agent.maxTokens}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={selectedAgent ? 'Edit Agent' : 'Create Agent'}
        size="lg"
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Agent Name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              placeholder="e.g., Content Writer"
              required
              autoFocus
            />
            <Select
              label="Provider"
              value={formData.providerId}
              onChange={(e) => setFormData({ ...formData, providerId: parseInt(e.target.value) })}
              options={providerOptions}
              placeholder="Select a provider"
            />
          </div>

          <Input
            label="Description"
            value={formData.description || ''}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            placeholder="What does this agent do? (optional)"
          />

          <div className="grid grid-cols-3 gap-4">
            <Input
              label="Model"
              value={formData.model}
              onChange={(e) => setFormData({ ...formData, model: e.target.value })}
              placeholder="gpt-4o"
              required
            />
            <Input
              label="Temperature"
              type="number"
              min="0"
              max="2"
              step="0.1"
              value={formData.temperature}
              onChange={(e) => setFormData({ ...formData, temperature: parseFloat(e.target.value) })}
            />
            <Input
              label="Max Tokens"
              type="number"
              min="1"
              max="128000"
              value={formData.maxTokens}
              onChange={(e) => setFormData({ ...formData, maxTokens: parseInt(e.target.value) })}
            />
          </div>

          <Textarea
            label="System Prompt"
            value={formData.systemPrompt}
            onChange={(e) => setFormData({ ...formData, systemPrompt: e.target.value })}
            placeholder="You are a helpful AI assistant..."
            rows={4}
            required
          />

          <div className="flex justify-end gap-2 pt-4 border-t">
            <Button type="button" variant="secondary" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" loading={submitting}>
              {selectedAgent ? 'Update' : 'Create'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

export default AgentsPage;
