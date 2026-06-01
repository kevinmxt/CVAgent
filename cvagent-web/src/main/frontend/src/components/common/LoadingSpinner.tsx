import React from 'react';

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  text?: string;
}

const SIZES = { sm: 16, md: 32, lg: 48 };

export default function LoadingSpinner({ size = 'md', text }: LoadingSpinnerProps) {
  const px = SIZES[size];
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 'var(--space-md)',
        padding: 'var(--space-2xl)',
        color: 'var(--color-text-secondary)',
      }}
    >
      <div
        style={{
          width: px,
          height: px,
          border: `${px <= 16 ? 2 : 3}px solid var(--color-border)`,
          borderTopColor: 'var(--color-primary)',
          borderRadius: '50%',
          animation: 'spin 0.8s linear infinite',
        }}
      />
      {text && <span style={{ fontSize: 'var(--font-size-sm)' }}>{text}</span>}
    </div>
  );
}
