import { importShared } from './__federation_fn_import-BuQQgUXV.js';
import { j as jsxRuntimeExports } from './jsx-runtime-XI9uIe3W.js';
import { B as Button, M as Modal, I as Input, T as Textarea } from './api-Df7DtIvT.js';
import { u as useBoardStore } from './boardStore-CbPfJ9jS.js';

const {useEffect,useState,useCallback} = await importShared('react');

const {useNavigate} = await importShared('react-router-dom');
function BoardListPage() {
  const navigate = useNavigate();
  const { boards, loading, error, fetchBoards, createBoard, deleteBoard } = useBoardStore();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({ name: "", description: "" });
  const [submitting, setSubmitting] = useState(false);
  useEffect(() => {
    fetchBoards();
  }, [fetchBoards]);
  const handleCreateBoard = useCallback(async (e) => {
    e.preventDefault();
    if (!formData.name.trim()) return;
    setSubmitting(true);
    try {
      const board = await createBoard(formData);
      setIsModalOpen(false);
      setFormData({ name: "", description: "" });
      navigate(`/boards/${board.id}`);
    } catch (error2) {
      console.error("Failed to create board:", error2);
    } finally {
      setSubmitting(false);
    }
  }, [formData, createBoard, navigate]);
  const handleDeleteBoard = useCallback(
    async (id, e) => {
      e.stopPropagation();
      if (!confirm("Are you sure you want to delete this board?")) return;
      await deleteBoard(id);
    },
    [deleteBoard]
  );
  return /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { children: [
    /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-center justify-between mb-6", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx("h1", { className: "text-2xl font-bold text-text-heading", children: "My Boards" }),
      /* @__PURE__ */ jsxRuntimeExports.jsxs(Button, { onClick: () => setIsModalOpen(true), children: [
        /* @__PURE__ */ jsxRuntimeExports.jsx("svg", { className: "w-4 h-4 mr-2", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor", children: /* @__PURE__ */ jsxRuntimeExports.jsx("path", { strokeLinecap: "round", strokeLinejoin: "round", strokeWidth: 2, d: "M12 4v16m8-8H4" }) }),
        "New Board"
      ] })
    ] }),
    error && /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "mb-4 p-4 bg-status-error/10 text-status-error rounded-lg", children: error }),
    loading ? /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "flex items-center justify-center h-64", children: /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "animate-spin rounded-full h-8 w-8 border-b-2 border-brand-primary" }) }) : boards.length === 0 ? /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "text-center py-12 bg-bg-subtle rounded-xl", children: [
      /* @__PURE__ */ jsxRuntimeExports.jsx(
        "svg",
        {
          className: "mx-auto h-12 w-12 text-text-placeholder",
          fill: "none",
          viewBox: "0 0 24 24",
          stroke: "currentColor",
          children: /* @__PURE__ */ jsxRuntimeExports.jsx(
            "path",
            {
              strokeLinecap: "round",
              strokeLinejoin: "round",
              strokeWidth: 1.5,
              d: "M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7m0 10a2 2 0 002 2h2a2 2 0 002-2V7a2 2 0 00-2-2h-2a2 2 0 00-2 2"
            }
          )
        }
      ),
      /* @__PURE__ */ jsxRuntimeExports.jsx("h3", { className: "mt-4 text-lg font-medium text-text-heading", children: "No boards yet" }),
      /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "mt-2 text-text-body", children: "Create your first board to start managing AI tasks." }),
      /* @__PURE__ */ jsxRuntimeExports.jsx(Button, { className: "mt-4", onClick: () => setIsModalOpen(true), children: "Create Board" })
    ] }) : /* @__PURE__ */ jsxRuntimeExports.jsx("div", { className: "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4", children: boards.map((board) => /* @__PURE__ */ jsxRuntimeExports.jsxs(
      "div",
      {
        onClick: () => navigate(`/boards/${board.id}`),
        className: "bg-bg-card rounded-xl shadow-sm border border-border-default p-5 hover:shadow-md transition-shadow cursor-pointer group",
        children: [
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex items-start justify-between", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx("h3", { className: "font-semibold text-text-heading group-hover:text-brand-primary", children: board.name }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(
              "button",
              {
                onClick: (e) => handleDeleteBoard(board.id, e),
                className: "opacity-0 group-hover:opacity-100 p-1 text-text-meta hover:text-status-error rounded transition-all",
                children: /* @__PURE__ */ jsxRuntimeExports.jsx("svg", { className: "w-4 h-4", fill: "none", viewBox: "0 0 24 24", stroke: "currentColor", children: /* @__PURE__ */ jsxRuntimeExports.jsx(
                  "path",
                  {
                    strokeLinecap: "round",
                    strokeLinejoin: "round",
                    strokeWidth: 2,
                    d: "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                  }
                ) })
              }
            )
          ] }),
          board.description && /* @__PURE__ */ jsxRuntimeExports.jsx("p", { className: "mt-2 text-sm text-text-body line-clamp-2", children: board.description }),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "mt-4 text-xs text-text-meta", children: [
            "Created ",
            new Date(board.createdAt).toLocaleDateString()
          ] })
        ]
      },
      board.id
    )) }),
    /* @__PURE__ */ jsxRuntimeExports.jsx(
      Modal,
      {
        isOpen: isModalOpen,
        onClose: () => setIsModalOpen(false),
        title: "Create New Board",
        size: "sm",
        children: /* @__PURE__ */ jsxRuntimeExports.jsxs("form", { onSubmit: handleCreateBoard, className: "space-y-4", children: [
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            Input,
            {
              label: "Board Name",
              value: formData.name,
              onChange: (e) => setFormData({ ...formData, name: e.target.value }),
              placeholder: "e.g., Content Creation",
              required: true,
              autoFocus: true
            }
          ),
          /* @__PURE__ */ jsxRuntimeExports.jsx(
            Textarea,
            {
              label: "Description",
              value: formData.description || "",
              onChange: (e) => setFormData({ ...formData, description: e.target.value }),
              placeholder: "What is this board for? (optional)",
              rows: 3
            }
          ),
          /* @__PURE__ */ jsxRuntimeExports.jsxs("div", { className: "flex justify-end gap-2 pt-4", children: [
            /* @__PURE__ */ jsxRuntimeExports.jsx(Button, { type: "button", variant: "secondary", onClick: () => setIsModalOpen(false), children: "Cancel" }),
            /* @__PURE__ */ jsxRuntimeExports.jsx(Button, { type: "submit", loading: submitting, children: "Create Board" })
          ] })
        ] })
      }
    )
  ] });
}

export { BoardListPage as default };
