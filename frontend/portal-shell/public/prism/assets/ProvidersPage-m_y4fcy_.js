import { importShared } from './__federation_fn_import-BuQQgUXV.js';
import { j as jsxRuntimeExports } from './jsx-runtime-XI9uIe3W.js';
import { B as Button, M as Modal, I as Input, S as Select } from './api-Df7DtIvT.js';
import { u as useProviderStore } from './providerStore-ZOpK-RXU.js';

const {useEffect,useState,useCallback} = await importShared('react');
const providerTypeOptions = [
  { value: "OPENAI", label: "OpenAI" },
  { value: "ANTHROPIC", label: "Anthropic" },
  { value: "GOOGLE", label: "Google AI" },
  { value: "LOCAL", label: "Local/Custom" }
];
function ProvidersPage() {
  const { providers, loading, error, fetchProviders, createProvider, deleteProvider } = useProviderStore();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    name: "",
    type: "OPENAI",
    apiKey: "",
    baseUrl: ""
  });
  useEffect(() => {
    fetchProviders();
  }, [fetchProviders]);
  const handleOpenModal = useCallback(() => {
    setFormData({ name: "", type: "OPENAI", apiKey: "", baseUrl: "" });
    setIsModalOpen(true);
  }, []);
  const handleSubmit = useCallback(async (e) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.apiKey.trim()) return;
    setSubmitting(true);
    try {
      await createProvider({
        ...formData,
        baseUrl: formData.baseUrl || void 0
      });
      setIsModalOpen(false);
    } catch (error2) {
      console.error("Failed to create provider:", error2);
    } finally {
      setSubmitting(false);
    }
  }, [formData, createProvider]);
  const handleDelete = useCallback(async (id) => {
    if (!confirm("Are you sure you want to delete this provider?")) return;
    await deleteProvider(id);
  }, [deleteProvider]);
  const getProviderIcon = (type) => {
    switch (type) {
      case "OPENAI":
        return "🟢";
      case "ANTHROPIC":
        return "🟠";
      case "GOOGLE":
        return "🔵";
      default:
        return "⚪";
    }
  };
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between mb-6", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("h1", { className: "text-2xl font-bold text-gray-900", children: "AI Providers" }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { onClick: handleOpenModal, children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("svg", { className: "w-4 h-4 mr-2", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor", children: /* @__PURE__ */ jsxRuntimeExports.jsx("path", { strokeLinecap: "round", strokeLinejoin: "round", strokeWidth: 2, d: "M12 4v16m8-8H4" }) }),
        "Add Provider"
      ] })
    ] }),
    error && /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "mb-4 p-4 bg-red-50 text-red-600 rounded-lg", children: error }),
    loading ? /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex items-center justify-center h-64", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "animate-spin rounded-full h-8 w-8 border-b-2 border-prism-500" }) }) : providers.length === 0 ? /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center py-12 bg-gray-50 rounded-xl", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("svg", { className: "mx-auto h-12 w-12 text-gray-400", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor", children: /* @__PURE__ */ jsxRuntimeExports.jsx("path", { strokeLinecap: "round", strokeLinejoin: "round", strokeWidth: 1.5, d: "M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01" }) }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("h3", { className: "mt-4 text-lg font-medium text-gray-900", children: "No providers yet" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "mt-2 text-gray-500", children: "Add an AI provider to start using agents." }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(Button, { className: "mt-4", onClick: handleOpenModal, children: "Add Provider" })
    ] }) : /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4", children: providers.map((provider) => /* @__PURE__ */ jsxRuntimeExports.jsxs(
      "div",
      {
        className: "bg-white rounded-xl shadow-sm border border-gray-200 p-5",
        children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-start justify-between", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-3", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: "text-2xl", children: getProviderIcon(provider.type) }),
              /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
                /* @__PURE__ */ jsxRuntimeExports.jsx("h3", { className: "font-semibold text-gray-900", children: provider.name }),
                /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "text-sm text-gray-500", children: provider.type })
              ] })
            ] }),
            /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center gap-2", children: [
              /* @__PURE__ */ jsxRuntimeExports.jsx("span", { className: `px-2 py-0.5 text-xs rounded-full ${provider.isActive ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-500"}`, children: provider.isActive ? "Active" : "Inactive" }),
              /* @__PURE__ */ jsxRuntimeExports.jsx(
                "button",
                {
                  onClick: () => handleDelete(provider.id),
                  className: "p-1 text-gray-400 hover:text-red-500 rounded-lg hover:bg-gray-100",
                  children: /* @__PURE__ */ jsxRuntimeExports.jsx("svg", { className: "w-4 h-4", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor", children: /* @__PURE__ */ jsxRuntimeExports.jsx("path", { strokeLinecap: "round", strokeLinejoin: "round", strokeWidth: 2, d: "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" }) })
                }
              )
            ] })
          ] }),
          provider.baseUrl && /* @__PURE__ */ jsxRuntimeExports.jsxs("p", { className: "mt-3 text-xs text-gray-400 truncate", children: [
            "Base URL: ",
            provider.baseUrl
          ] }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "mt-3 text-xs text-gray-400", children: [
            "Added ",
            new Date(provider.createdAt).toLocaleDateString()
          ] })
        ]
      },
      provider.id
    )) }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(
      Modal,
      {
        isOpen: isModalOpen,
        onClose: () => setIsModalOpen(false),
        title: "Add AI Provider",
        size: "sm",
        children: /* @__PURE__ */ jsxRuntimeExports.jsxs("form", { onSubmit: handleSubmit, className: "space-y-4", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            Input,
            {
              label: "Provider Name",
              value: formData.name,
              onChange: (e) => setFormData({ ...formData, name: e.target.value }),
              placeholder: "e.g., My OpenAI Account",
              required: true,
              autoFocus: true
            }
          ),
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            Select,
            {
              label: "Provider Type",
              value: formData.type,
              onChange: (e) => setFormData({ ...formData, type: e.target.value }),
              options: providerTypeOptions
            }
          ),
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            Input,
            {
              label: "API Key",
              type: "password",
              value: formData.apiKey,
              onChange: (e) => setFormData({ ...formData, apiKey: e.target.value }),
              placeholder: "sk-...",
              required: true
            }
          ),
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            Input,
            {
              label: "Base URL (Optional)",
              value: formData.baseUrl || "",
              onChange: (e) => setFormData({ ...formData, baseUrl: e.target.value }),
              placeholder: "https://api.openai.com/v1",
              helperText: "Leave empty to use default provider URL"
            }
          ),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-end gap-2 pt-4 border-t", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Button, { type: "button", variant: "secondary", onClick: () => setIsModalOpen(false), children: "Cancel" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(Button, { type: "submit", loading: submitting, children: "Add Provider" })
          ] })
        ] })
      }
    )
  ] });
}

export { ProvidersPage as default };
