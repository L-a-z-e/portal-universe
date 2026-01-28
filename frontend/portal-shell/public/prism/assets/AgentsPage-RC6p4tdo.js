import { importShared } from './__federation_fn_import-BuQQgUXV.js';
import { j as jsxRuntimeExports } from './jsx-runtime-XI9uIe3W.js';
import { B as Button, M as Modal, I as Input, S as Select, T as Textarea } from './api-Df7DtIvT.js';
import { u as useAgentStore } from './agentStore-CqzecZph.js';
import { u as useProviderStore } from './providerStore-ZOpK-RXU.js';

const {useEffect,useState,useCallback} = await importShared('react');
function AgentsPage() {
  const { agents, loading, error, fetchAgents, createAgent, updateAgent, deleteAgent } = useAgentStore();
  const { providers, fetchProviders } = useProviderStore();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedAgent, setSelectedAgent] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    name: "",
    description: "",
    providerId: 0,
    model: "gpt-4o",
    systemPrompt: "",
    temperature: 0.7,
    maxTokens: 4096
  });
  useEffect(() => {
    fetchAgents();
    fetchProviders();
  }, [fetchAgents, fetchProviders]);
  const handleOpenModal = useCallback((agent) => {
    if (agent) {
      setSelectedAgent(agent);
      setFormData({
        name: agent.name,
        description: agent.description || "",
        providerId: agent.providerId,
        model: agent.model,
        systemPrompt: agent.systemPrompt,
        temperature: agent.temperature,
        maxTokens: agent.maxTokens
      });
    } else {
      setSelectedAgent(null);
      setFormData({
        name: "",
        description: "",
        providerId: providers[0]?.id || 0,
        model: "gpt-4o",
        systemPrompt: "You are a helpful AI assistant.",
        temperature: 0.7,
        maxTokens: 4096
      });
    }
    setIsModalOpen(true);
  }, [providers]);
  const handleSubmit = useCallback(async (e) => {
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
    } catch (error2) {
      console.error("Failed to save agent:", error2);
    } finally {
      setSubmitting(false);
    }
  }, [formData, selectedAgent, createAgent, updateAgent]);
  const handleDelete = useCallback(async (id) => {
    if (!confirm("Are you sure you want to delete this agent?")) return;
    await deleteAgent(id);
  }, [deleteAgent]);
  const providerOptions = providers.filter((p) => p.isActive).map((p) => ({ value: p.id, label: `${p.name} (${p.type})` }));
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between mb-6", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("h1", { className: "text-2xl font-bold text-gray-900", children: "AI Agents" }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { onClick: () => handleOpenModal(), children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("svg", { className: "w-4 h-4 mr-2", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor", children: /* @__PURE__ */ jsxRuntimeExports.jsx("path", { strokeLinecap: "round", strokeLinejoin: "round", strokeWidth: 2, d: "M12 4v16m8-8H4" }) }),
        "New Agent"
      ] })
    ] }),
    error && /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "mb-4 p-4 bg-red-50 text-red-600 rounded-lg", children: error }),
    loading ? /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex items-center justify-center h-64", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "animate-spin rounded-full h-8 w-8 border-b-2 border-prism-500" }) }) : agents.length === 0 ? /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center py-12 bg-gray-50 rounded-xl", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("svg", { className: "mx-auto h-12 w-12 text-gray-400", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor", children: /* @__PURE__ */ jsxRuntimeExports.jsx("path", { strokeLinecap: "round", strokeLinejoin: "round", strokeWidth: 1.5, d: "M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("h3", { className: "mt-4 text-lg font-medium text-gray-900", children: "No agents yet" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "mt-2 text-gray-500", children: "Create your first AI agent to automate tasks." }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(Button, { className: "mt-4", onClick: () => handleOpenModal(), children: "Create Agent" })
    ] }) : /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "grid grid-cols-1 md:grid-cols-2 gap-4", children: agents.map((agent) => /* @__PURE__ */ jsxRuntimeExports.jsxs(
      "div",
      {
        className: "bg-white rounded-xl shadow-sm border border-gray-200 p-5",
        children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-start justify-between", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("h3", { className: "font-semibold text-gray-900", children: agent.name }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: `px-2 py-0.5 text-xs rounded-full ${agent.isActive ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-500"}`, children: agent.isActive ? "Active" : "Inactive" })
              ] }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "text-sm text-gray-500 mt-1", children: [
                agent.providerName,
                " / ",
                agent.model
              ] })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex gap-1", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx(
                "button",
                {
                  onClick: () => handleOpenModal(agent),
                  className: "p-2 text-gray-400 hover:text-prism-600 rounded-lg hover:bg-gray-100",
                  children: /* @__PURE__ */ jsxRuntimeExports.jsx("svg", { className: "w-4 h-4", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor", children: /* @__PURE__ */ jsxRuntimeExports.jsx("path", { strokeLinecap: "round", strokeLinejoin: "round", strokeWidth: 2, d: "M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" }) })
                }
              ),
              /* @__PURE__ */ jsxRuntimeExports.jsx(
                "button",
                {
                  onClick: () => handleDelete(agent.id),
                  className: "p-2 text-gray-400 hover:text-red-500 rounded-lg hover:bg-gray-100",
                  children: /* @__PURE__ */ jsxRuntimeExports.jsx("svg", { className: "w-4 h-4", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor", children: /* @__PURE__ */ jsxRuntimeExports.jsx("path", { strokeLinecap: "round", strokeLinejoin: "round", strokeWidth: 2, d: "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" }) })
                }
              )
            ] })
          ] }),
          agent.description && /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "mt-3 text-sm text-gray-600", children: agent.description }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "mt-4 flex gap-4 text-xs text-gray-500", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { children: [
              "Temp: ",
              agent.temperature
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("span", { children: [
              "Max Tokens: ",
              agent.maxTokens
            ] })
          ] })
        ]
      },
      agent.id
    )) }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(
      Modal,
      {
        isOpen: isModalOpen,
        onClose: () => setIsModalOpen(false),
        title: selectedAgent ? "Edit Agent" : "Create Agent",
        size: "lg",
        children: /* @__PURE__ */ jsxRuntimeExports.jsxs("form", { onSubmit: handleSubmit, className: "space-y-4", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid grid-cols-2 gap-4", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                label: "Agent Name",
                value: formData.name,
                onChange: (e) => setFormData({ ...formData, name: e.target.value }),
                placeholder: "e.g., Content Writer",
                required: true,
                autoFocus: true
              }
            ),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Select,
              {
                label: "Provider",
                value: formData.providerId,
                onChange: (e) => setFormData({ ...formData, providerId: parseInt(e.target.value) }),
                options: providerOptions,
                placeholder: "Select a provider"
              }
            )
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            Input,
            {
              label: "Description",
              value: formData.description || "",
              onChange: (e) => setFormData({ ...formData, description: e.target.value }),
              placeholder: "What does this agent do? (optional)"
            }
          ),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "grid grid-cols-3 gap-4", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                label: "Model",
                value: formData.model,
                onChange: (e) => setFormData({ ...formData, model: e.target.value }),
                placeholder: "gpt-4o",
                required: true
              }
            ),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                label: "Temperature",
                type: "number",
                min: "0",
                max: "2",
                step: "0.1",
                value: formData.temperature,
                onChange: (e) => setFormData({ ...formData, temperature: parseFloat(e.target.value) })
              }
            ),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              Input,
              {
                label: "Max Tokens",
                type: "number",
                min: "1",
                max: "128000",
                value: formData.maxTokens,
                onChange: (e) => setFormData({ ...formData, maxTokens: parseInt(e.target.value) })
              }
            )
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            Textarea,
            {
              label: "System Prompt",
              value: formData.systemPrompt,
              onChange: (e) => setFormData({ ...formData, systemPrompt: e.target.value }),
              placeholder: "You are a helpful AI assistant...",
              rows: 4,
              required: true
            }
          ),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-end gap-2 pt-4 border-t", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Button, { type: "button", variant: "secondary", onClick: () => setIsModalOpen(false), children: "Cancel" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(Button, { type: "submit", loading: submitting, children: selectedAgent ? "Update" : "Create" })
          ] })
        ] })
      }
    )
  ] });
}

export { AgentsPage as default };
