import React from 'react';

interface PaginationProps {
  page: number;
  pages: number;
  total: number;
  onPageChange: (page: number) => void;
}

export default function Pagination({ page, pages, total, onPageChange }: PaginationProps) {
  if (pages <= 1) return null;

  const items: (number | '...')[] = [];
  for (let i = 1; i <= pages; i++) {
    if (i === 1 || i === pages || (i >= page - 1 && i <= page + 1)) {
      items.push(i);
    } else if (items[items.length - 1] !== '...') {
      items.push('...');
    }
  }

  return (
    <div className="pagination-bar">
      <span className="pagination-info">共 {total} 条记录</span>
      <div className="pagination-buttons">
        <button className="btn btn-outline btn-sm" disabled={page <= 1} onClick={() => onPageChange(page - 1)}>
          上一页
        </button>
        {items.map((item, idx) =>
          item === '...' ? (
            <span key={`ellipsis-${idx}`} className="pagination-ellipsis">...</span>
          ) : (
            <button
              key={item}
              className={`pagination-num ${item === page ? 'active' : ''}`}
              onClick={() => onPageChange(item)}
            >
              {item}
            </button>
          )
        )}
        <button className="btn btn-outline btn-sm" disabled={page >= pages} onClick={() => onPageChange(page + 1)}>
          下一页
        </button>
      </div>
      <style>{`
        .pagination-bar {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: var(--space-md) 0;
        }
        .pagination-info {
          font-size: var(--font-size-sm);
          color: var(--color-text-muted);
        }
        .pagination-buttons {
          display: flex;
          align-items: center;
          gap: 4px;
        }
        .pagination-num {
          min-width: 32px;
          height: 32px;
          display: flex;
          align-items: center;
          justify-content: center;
          border: 1px solid var(--color-border);
          border-radius: var(--radius-sm);
          background: var(--color-surface);
          color: var(--color-text-secondary);
          font-size: var(--font-size-sm);
          cursor: pointer;
          transition: all var(--transition-fast);
        }
        .pagination-num:hover {
          background: var(--color-surface-alt);
          border-color: var(--color-text-muted);
        }
        .pagination-num.active {
          background: var(--color-primary);
          border-color: var(--color-primary);
          color: #fff;
        }
        .pagination-ellipsis {
          width: 32px;
          text-align: center;
          color: var(--color-text-muted);
        }
      `}</style>
    </div>
  );
}
