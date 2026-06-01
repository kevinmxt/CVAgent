import React from 'react';

interface EmptyStateProps {
  icon?: string;
  title: string;
  description?: string;
  action?: React.ReactNode;
}

export default function EmptyState({ icon = '📋', title, description, action }: EmptyStateProps) {
  return (
    <div style={{
      textAlign: 'center',
      padding: 'var(--space-2xl)',
      color: 'var(--color-text-muted)',
    }}>
      <div style={{ fontSize: '3rem', marginBottom: 'var(--space-md)', opacity: 0.3 }}>{icon}</div>
      <h3 style={{ fontSize: 'var(--font-size-lg)', fontWeight: 600, color: 'var(--color-text-secondary)', marginBottom: 'var(--space-sm)' }}>
        {title}
      </h3>
      {description && (
        <p style={{ fontSize: 'var(--font-size-sm)', marginBottom: 'var(--space-lg)', maxWidth: 400, margin: '0 auto var(--space-lg)' }}>
          {description}
        </p>
      )}
      {action}
    </div>
  );
}
