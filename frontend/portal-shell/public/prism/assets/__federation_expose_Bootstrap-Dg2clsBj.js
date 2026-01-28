import { importShared } from './__federation_fn_import-BuQQgUXV.js';
import { j as jsxRuntimeExports } from './jsx-runtime-XI9uIe3W.js';
import { R as ReactDOM, A as App, r as resetRouter, s as setAppActive, n as navigateTo } from './index-Dgi6He6c.js';

const React = await importShared('react');
const instanceRegistry = /* @__PURE__ */ new WeakMap();
function mountPrismApp(el, options = {}) {
  console.group("🚀 [Prism] Mounting app in EMBEDDED mode");
  window.__POWERED_BY_PORTAL_SHELL__ = true;
  if (!el) {
    console.error("❌ [Prism] Mount element is null!");
    console.groupEnd();
    throw new Error("[Prism] Mount element is required");
  }
  const existingInstance = instanceRegistry.get(el);
  if (existingInstance) {
    console.log("⚠️ [Prism] Cleaning up existing instance...");
    try {
      existingInstance.styleObserver?.disconnect();
      existingInstance.root.unmount();
    } catch (err) {
      console.warn("⚠️ [Prism] Existing instance cleanup warning:", err);
    }
    instanceRegistry.delete(el);
  }
  console.log("📍 Mount target:", el.tagName, el.className || "(no class)");
  const { initialPath = "/", onNavigate, theme = "light" } = options;
  console.log("📍 Initial path:", initialPath);
  console.log("📍 Theme:", theme);
  try {
    const root = ReactDOM.createRoot(el);
    let navigateCallback = onNavigate || null;
    let currentTheme = theme;
    const styleObserver = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        mutation.addedNodes.forEach((node) => {
          if (node.nodeName === "STYLE" && !node.hasAttribute("data-mf-app")) {
            node.setAttribute("data-mf-app", "prism");
          }
        });
      });
    });
    styleObserver.observe(document.head, { childList: true });
    const getCurrentProps = () => ({
      initialPath,
      theme: currentTheme,
      onNavigate: (path) => {
        const instance = instanceRegistry.get(el);
        if (instance?.isActive) {
          console.log(`📍 [Prism] Route changed to: ${path}`);
          instance.navigateCallback?.(path);
        }
      }
    });
    const rerender = () => {
      root.render(
        /* @__PURE__ */ jsxRuntimeExports.jsx(React.StrictMode, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(App, { ...getCurrentProps() }) })
      );
    };
    instanceRegistry.set(el, {
      root,
      navigateCallback,
      styleObserver,
      isActive: true,
      currentTheme,
      rerender
    });
    document.documentElement.setAttribute("data-service", "prism");
    console.log('[Prism] Set data-service="prism"');
    rerender();
    console.log("✅ [Prism] App mounted successfully");
    console.groupEnd();
    return {
      onParentNavigate: (path) => {
        const instance = instanceRegistry.get(el);
        if (!instance?.isActive) {
          console.log(`⏸️ [Prism] Skipping navigation (inactive): ${path}`);
          return;
        }
        console.log(`📥 [Prism] Received navigation from parent: ${path}`);
        navigateTo(path);
      },
      onActivated: () => {
        console.log("🔄 [Prism] App activated (keep-alive)");
        const instance = instanceRegistry.get(el);
        if (instance) {
          instance.isActive = true;
          document.documentElement.setAttribute("data-service", "prism");
          setTimeout(() => {
            setAppActive(true);
          }, 100);
        }
      },
      onDeactivated: () => {
        console.log("⏸️ [Prism] App deactivated (keep-alive)");
        const instance = instanceRegistry.get(el);
        if (instance) {
          instance.isActive = false;
          setAppActive(false);
        }
      },
      onThemeChange: (newTheme) => {
        console.log(`🎨 [Prism] Theme changed to: ${newTheme}`);
        const instance = instanceRegistry.get(el);
        if (instance) {
          currentTheme = newTheme;
          instance.currentTheme = newTheme;
          instance.rerender();
        }
      },
      unmount: () => {
        console.group("🔄 [Prism] Unmounting app");
        const instance = instanceRegistry.get(el);
        if (instance?.styleObserver) {
          instance.styleObserver.disconnect();
        }
        try {
          if (instance?.root) {
            instance.root.unmount();
          }
          console.log("✅ [Prism] App unmounted successfully");
        } catch (err) {
          console.error("❌ [Prism] App unmount failed:", err);
        }
        try {
          el.innerHTML = "";
          if (document.documentElement.getAttribute("data-service") === "prism") {
            document.documentElement.removeAttribute("data-service");
          }
          resetRouter();
          console.log("✅ [Prism] Cleanup completed");
        } catch (err) {
          console.error("❌ [Prism] Cleanup failed:", err);
        }
        instanceRegistry.delete(el);
        console.groupEnd();
      }
    };
  } catch (error) {
    console.error("❌ [Prism] Mount failed:", error);
    console.groupEnd();
    throw error;
  }
}
if (!window.__FEDERATION__ && !window.__POWERED_BY_PORTAL_SHELL__) {
  const container = document.getElementById("root");
  if (container) {
    const root = ReactDOM.createRoot(container);
    root.render(
      /* @__PURE__ */ jsxRuntimeExports.jsx(React.StrictMode, { children: /* @__PURE__ */ jsxRuntimeExports.jsx(App, {}) })
    );
  }
}
const bootstrap = { mountPrismApp };

export { bootstrap as default, mountPrismApp };
