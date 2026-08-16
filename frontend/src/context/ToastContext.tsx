import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import { CheckCircle2, XCircle, AlertTriangle, Info, X } from "lucide-react";
import { cn } from "@/lib/utils";

type ToastVariant = "success" | "error" | "warning" | "info";

interface ToastItem {
  id: number;
  title: string;
  description?: string;
  variant: ToastVariant;
}

interface ToastContextValue {
  toast: (item: Omit<ToastItem, "id">) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

const VARIANT_ICON: Record<ToastVariant, typeof Info> = {
  success: CheckCircle2,
  error: XCircle,
  warning: AlertTriangle,
  info: Info,
};

const VARIANT_STYLE: Record<ToastVariant, string> = {
  success: "border-status-resolved/30 bg-status-resolved/10 text-status-resolved",
  error: "border-destructive/30 bg-destructive/10 text-destructive",
  warning: "border-status-pending/30 bg-status-pending/10 text-status-pending",
  info: "border-status-assigned/30 bg-status-assigned/10 text-status-assigned",
};

/** Success/error/warning/info toasts, top-right, auto-dismiss after 5s (ui-rules.md). */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((t) => t.id !== id));
  }, []);

  const toast = useCallback(
    (item: Omit<ToastItem, "id">) => {
      const id = Date.now() + Math.random();
      setToasts((current) => [...current, { ...item, id }]);
      setTimeout(() => dismiss(id), 5000);
    },
    [dismiss],
  );

  return (
    <ToastContext.Provider value={{ toast }}>
      {children}
      <div className="pointer-events-none fixed right-4 top-4 z-[100] flex w-full max-w-sm flex-col gap-2">
        {toasts.map((t) => {
          const Icon = VARIANT_ICON[t.variant];
          return (
            <div
              key={t.id}
              role="status"
              className={cn(
                "pointer-events-auto flex items-start gap-3 rounded-lg border bg-surface p-4 shadow-lg",
                VARIANT_STYLE[t.variant],
              )}
            >
              <Icon className="mt-0.5 h-5 w-5 shrink-0" />
              <div className="flex-1">
                <p className="text-sm font-medium text-text">{t.title}</p>
                {t.description && <p className="text-sm text-text-muted">{t.description}</p>}
              </div>
              <button
                onClick={() => dismiss(t.id)}
                className="text-text-muted hover:text-text"
                aria-label="Dismiss notification"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast must be used within a ToastProvider");
  return ctx;
}
