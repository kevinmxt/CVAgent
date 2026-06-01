import React from 'react';
import type { PageResult } from '../../api/types';

interface Column<T> {
  key: string;
  header: string;
  render: (item: T) => React.ReactNode;
  width?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: PageResult<T> | null;
  loading: boolean;
  emptyText?: string;
}

export default function DataTable<T extends { id: number }>({
  columns, data, loading, emptyText = '暂无数据',
}: DataTableProps<T>) {
  if (loading) {
    return (
      <div className="table-loading">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="skeleton-row" style={{ animationDelay: `${i * 0.1}s` }} />
        ))}
        <style>{`
          .table-loading { display: flex; flex-direction: column; gap: 8px; }
          .skeleton-row {
            height: 48px;
            background: linear-gradient(90deg, var(--color-surface-alt) 25%, #e8eaef 50%, var(--color-surface-alt) 75%);
            background-size: 200% 100%;
            animation: shimmer 1.5s infinite;
            border-radius: var(--radius-md);
          }
        `}</style>
      </div>
    );
  }

  if (!data || data.items.length === 0) {
    return (
      <div className="table-empty">
        <div className="table-empty-icon">📋</div>
        <p>{emptyText}</p>
        <style>{`
          .table-empty {
            text-align: center;
            padding: var(--space-2xl);
            color: var(--color-text-muted);
          }
          .table-empty-icon { font-size: 2.5rem; margin-bottom: var(--space-md); opacity: 0.4; }
        `}</style>
      </div>
    );
  }

  return (
    <div style={{ overflowX: 'auto' }}>
      <table className="data-table">
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key} style={col.width ? { width: col.width } : undefined}>{col.header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.items.map((item) => (
            <tr key={item.id}>
              {columns.map((col) => (
                <td key={col.key}>{col.render(item)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      <style>{`
        .data-table { width: 100%; border-collapse: collapse; }
        .data-table th {
          text-align: left;
          padding: 12px 16px;
          font-size: var(--font-size-xs);
          font-weight: 600;
          color: var(--color-text-muted);
          text-transform: uppercase;
          letter-spacing: 0.05em;
          border-bottom: 1px solid var(--color-border);
          background: var(--color-surface-alt);
          white-space: nowrap;
        }
        .data-table td {
          padding: 12px 16px;
          font-size: var(--font-size-sm);
          color: var(--color-text);
          border-bottom: 1px solid var(--color-border-light);
          vertical-align: middle;
        }
        .data-table tbody tr { transition: background var(--transition-fast); }
        .data-table tbody tr:hover { background: var(--color-primary-light); }
      `}</style>
    </div>
  );
}
