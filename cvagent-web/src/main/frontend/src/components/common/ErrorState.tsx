import React from 'react';

interface ErrorStateProps {
  message: string;
  onRetry?: () => void;
}

export default function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div style={{
      textAlign: 'center',
      padding: 'var(--space-2xl)',
      color: 'var(--color-text-muted)',
    }}>
      <div style={{ fontSize: '2.5rem', marginBottom: 'var(--space-md)', opacity: 0.4 }}>⚠</div>
      <p style={{ fontSize: 'var(--font-size-base)', color: 'var(--color-danger)', marginBottom: 'var(--space-md)', fontWeight: 500 }}>
        {message}
      </p>
      {onRetry && (
        <button className="btn btn-outline btn-sm" onClick={onRetry}>
          重试
        </button>
      )}
    </div>
  );
}
