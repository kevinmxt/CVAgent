import React, { useEffect, useRef, type ReactNode } from 'react';

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  footer?: ReactNode;
  width?: string;
}

export default function Modal({ open, onClose, title, children, footer, width = '520px' }: ModalProps) {
  const overlayRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handler);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handler);
      document.body.style.overflow = '';
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      ref={overlayRef}
      className="modal-overlay"
      onClick={(e) => { if (e.target === overlayRef.current) onClose(); }}
    >
      <div className="modal-panel animate-slide-up" style={{ maxWidth: width }}>
        <div className="modal-header">
          <h2 className="modal-title">{title}</h2>
          <button className="modal-close btn-ghost" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
      <style>{`
        .modal-overlay {
          position: fixed;
          inset: 0;
          z-index: 9000;
          display: flex;
          align-items: center;
          justify-content: center;
          background: rgba(0, 0, 0, 0.4);
          backdrop-filter: blur(4px);
          padding: var(--space-lg);
        }
        .modal-panel {
          background: var(--color-surface);
          border-radius: var(--radius-xl);
          box-shadow: var(--shadow-xl);
          width: 100%;
          max-height: 85vh;
          display: flex;
          flex-direction: column;
          overflow: hidden;
        }
        .modal-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: var(--space-lg) var(--space-xl);
          border-bottom: 1px solid var(--color-border-light);
        }
        .modal-title {
          font-size: var(--font-size-lg);
          font-weight: 700;
          color: var(--color-text);
        }
        .modal-close {
          font-size: 1.2rem;
          width: 32px;
          height: 32px;
          display: flex;
          align-items: center;
          justify-content: center;
          border-radius: var(--radius-md);
          color: var(--color-text-muted);
        }
        .modal-close:hover {
          background: var(--color-surface-alt);
          color: var(--color-text);
        }
        .modal-body {
          padding: var(--space-xl);
          overflow-y: auto;
          flex: 1;
        }
        .modal-footer {
          display: flex;
          justify-content: flex-end;
          gap: var(--space-sm);
          padding: var(--space-md) var(--space-xl);
          border-top: 1px solid var(--color-border-light);
          background: var(--color-surface-alt);
        }
      `}</style>
    </div>
  );
}
