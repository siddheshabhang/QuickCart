import { createContext, useContext, useState, useCallback, ReactNode } from "react";
import type { CSSProperties } from "react";

export type ToastType = "success" | "error" | "info";

interface ToastMessage {
  id: number;
  message: string;
  type: ToastType;
}

interface ToastContextType {
  showToast: (message: string, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const showToast = useCallback((message: string, type: ToastType = "success") => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { id, message, type }]);

    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 3000);
  }, []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      {/* Toast Container */}
      <div style={styles.container}>
        {toasts.map((toast) => (
          <div key={toast.id} style={{ ...styles.toast, ...styles[toast.type] }}>
            {toast.type === "success" && "✅ "}
            {toast.type === "error" && "❌ "}
            {toast.type === "info" && "ℹ️ "}
            {toast.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within a ToastProvider");
  }
  return context;
}

const styles: Record<string, CSSProperties> = {
  container: {
    position: "fixed",
    bottom: "20px",
    right: "20px",
    display: "flex",
    flexDirection: "column",
    gap: "10px",
    zIndex: 9999,
  },
  toast: {
    padding: "12px 20px",
    borderRadius: "8px",
    background: "#333",
    color: "#fff",
    boxShadow: "0 4px 6px rgba(0,0,0,0.1)",
    fontSize: "0.9rem",
    fontWeight: 500,
    animation: "slideIn 0.3s ease-out forwards",
    display: "flex",
    alignItems: "center",
    gap: "8px",
  },
  success: {
    background: "var(--color-success-bg, #ECFDF5)",
    color: "var(--color-success, #10B981)",
    border: "1px solid #A7F3D0",
  },
  error: {
    background: "var(--color-error-bg, #FEF2F2)",
    color: "var(--color-error, #EF4444)",
    border: "1px solid #FECACA",
  },
  info: {
    background: "var(--color-primary-light, #EEF2FF)",
    color: "var(--color-primary, #4F46E5)",
    border: "1px solid #C7D2FE",
  },
};
