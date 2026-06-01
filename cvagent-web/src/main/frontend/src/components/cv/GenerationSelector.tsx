import React from 'react';

interface CardSelectorProps<T> {
  items: T[];
  selectedId: number | null;
  onSelect: (id: number) => void;
  loading: boolean;
  title: string;
  renderItem: (item: T) => { id: number; title: string; subtitle: string; badge?: string };
  emptyText: string;
}

export default function CardSelector<T>({ items, selectedId, onSelect, loading, title, renderItem, emptyText }: CardSelectorProps<T>) {
  return (
    <div className="selector-panel">
      <h3 className="selector-title">{title}</h3>
      <div className="selector-list">
        {loading ? (
          <div className="selector-loading">
            {[1, 2, 3].map((i) => <div key={i} className="skeleton-row" style={{ height: 64 }} />)}
          </div>
        ) : items.length === 0 ? (
          <div className="selector-empty">{emptyText}</div>
        ) : (
          items.map((item) => {
            const card = renderItem(item);
            const selected = card.id === selectedId;
            return (
              <div
                key={card.id}
                className={`selector-card ${selected ? 'selected' : ''}`}
                onClick={() => onSelect(card.id)}
              >
                <div className="selector-card-header">
                  <span className="selector-card-title">{card.title}</span>
                  {card.badge && <span className="selector-card-badge">{card.badge}</span>}
                </div>
                <span className="selector-card-sub">{card.subtitle}</span>
                {selected && <div className="selector-check">✓</div>}
              </div>
            );
          })
        )}
      </div>
      <style>{`
        .selector-panel {
          flex: 1;
          min-width: 0;
          display: flex;
          flex-direction: column;
          border: 1px solid var(--color-border);
          border-radius: var(--radius-lg);
          background: var(--color-surface);
          overflow: hidden;
        }
        .selector-title {
          padding: 12px 16px;
          font-size: var(--font-size-sm);
          font-weight: 600;
          color: var(--color-text-secondary);
          border-bottom: 1px solid var(--color-border-light);
          background: var(--color-surface-alt);
        }
        .selector-list {
          flex: 1;
          overflow-y: auto;
          padding: var(--space-sm);
          max-height: 400px;
        }
        .selector-card {
          position: relative;
          padding: 12px 16px;
          border: 1px solid var(--color-border-light);
          border-radius: var(--radius-md);
          margin-bottom: 6px;
          cursor: pointer;
          transition: all var(--transition-fast);
        }
        .selector-card:hover {
          border-color: var(--color-primary);
          background: var(--color-primary-light);
        }
        .selector-card.selected {
          border-color: var(--color-primary);
          background: var(--color-primary-light);
          box-shadow: 0 0 0 2px var(--color-primary-light);
        }
        .selector-card-header {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 4px;
        }
        .selector-card-title {
          font-size: var(--font-size-sm);
          font-weight: 600;
          color: var(--color-text);
        }
        .selector-card-badge {
          font-size: var(--font-size-xs);
          padding: 1px 6px;
          border-radius: var(--radius-full);
          background: var(--color-surface-alt);
          color: var(--color-text-muted);
          font-weight: 500;
        }
        .selector-card-sub {
          font-size: var(--font-size-xs);
          color: var(--color-text-muted);
        }
        .selector-check {
          position: absolute;
          top: 8px;
          right: 8px;
          width: 20px;
          height: 20px;
          display: flex;
          align-items: center;
          justify-content: center;
          background: var(--color-primary);
          color: #fff;
          border-radius: 50%;
          font-size: 10px;
          font-weight: 700;
        }
        .selector-empty {
          text-align: center;
          padding: var(--space-xl);
          color: var(--color-text-muted);
          font-size: var(--font-size-sm);
        }
        .selector-loading {
          display: flex;
          flex-direction: column;
          gap: 6px;
        }
      `}</style>
    </div>
  );
}
