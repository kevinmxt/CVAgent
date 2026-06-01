import React, { createContext, useCallback, useContext, useState, type ReactNode } from 'react';

export interface ToastMessage {
  id: number;
  type: 'success' | 'error' | 'info';
  message: string;
}

interface ToastContextValue {
  toasts: ToastMessage[];
  addToast: (type: ToastMessage['type'], message: string) => void;
  removeToast: (id: number) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

let nextId = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const removeToast = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const addToast = useCallback((type: ToastMessage['type'], message: string) => {
    const id = nextId++;
    setToasts((prev) => [...prev, { id, type, message }]);
    setTimeout(() => removeToast(id), 4000);
  }, [removeToast]);

  return (
    <ToastContext.Provider value={{ toasts, addToast, removeToast }}>
      {children}
      <div className="toast-container">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={`toast toast-${toast.type} animate-slide-up`}
            onClick={() => removeToast(toast.id)}
          >
            <span className="toast-icon">
              {toast.type === 'success' ? '✓' : toast.type === 'error' ? '✕' : 'ℹ'}
            </span>
            <span className="toast-message">{toast.message}</span>
          </div>
        ))}
      </div>
      <style>{`
        .toast {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 12px 20px;
          border-radius: var(--radius-md);
          font-size: var(--font-size-sm);
          font-weight: 500;
          box-shadow: var(--shadow-lg);
          pointer-events: auto;
          cursor: pointer;
          min-width: 280px;
          max-width: 420px;
          border-left: 3px solid;
        }
        .toast-success {
          background: var(--color-success-light);
          color: #166534;
          border-left-color: var(--color-success);
        }
        .toast-error {
          background: var(--color-danger-light);
          color: #991b1b;
          border-left-color: var(--color-danger);
        }
        .toast-info {
          background: var(--color-primary-light);
          color: #1e40af;
          border-left-color: var(--color-primary);
        }
        .toast-icon {
          font-size: 1rem;
          flex-shrink: 0;
        }
        .toast-message {
          flex: 1;
        }
      `}</style>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
