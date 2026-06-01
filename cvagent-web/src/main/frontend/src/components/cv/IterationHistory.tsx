import React from 'react';
import { parseRoleScores } from '../../api/types';
import type { CvGenerationRecord } from '../../api/types';

interface IterationHistoryProps {
  history: CvGenerationRecord[];
  loading: boolean;
}

export default function IterationHistory({ history, loading }: IterationHistoryProps) {
  if (loading) {
    return <div style={{ padding: 'var(--space-xl)', textAlign: 'center', color: 'var(--color-text-muted)' }}>加载中...</div>;
  }

  if (history.length === 0) {
    return <div style={{ padding: 'var(--space-xl)', textAlign: 'center', color: 'var(--color-text-muted)' }}>暂无迭代记录</div>;
  }

  const sorted = [...history].sort((a, b) => a.iteration - b.iteration);

  return (
    <div className="iteration-history">
      <div className="iteration-timeline">
        {sorted.map((record, idx) => {
          const scores = parseRoleScores(record.roleScores);
          const isLast = idx === sorted.length - 1;
          return (
            <div key={record.id || idx} className="iteration-item">
              <div className="iteration-marker">
                <div className="iteration-dot" />
                {!isLast && <div className="iteration-line" />}
              </div>
              <div className="iteration-card">
                <div className="iteration-card-header">
                  <span className="iteration-number">第 {record.iteration} 轮</span>
                  <span className={`iteration-score ${record.overallScore >= 0.8 ? 'pass' : record.overallScore >= 0.5 ? 'mid' : 'fail'}`}>
                    {Math.round(record.overallScore * 100)} 分
                  </span>
                </div>
                {Object.keys(scores).length > 0 && (
                  <div className="iteration-role-scores">
                    {Object.entries(scores).map(([key, val]) => (
                      <span key={key} className="iteration-role-tag">{key}: {Math.round(val * 100)}</span>
                    ))}
                  </div>
                )}
                {record.feedback && (
                  <details className="iteration-feedback-detail">
                    <summary>查看反馈</summary>
                    <p className="iteration-feedback-text">{record.feedback}</p>
                  </details>
                )}
              </div>
            </div>
          );
        })}
      </div>
      <style>{`
        .iteration-history { padding: var(--space-xl); }
        .iteration-timeline { position: relative; }
        .iteration-item { display: flex; gap: var(--space-md); padding-bottom: var(--space-lg); }
        .iteration-marker {
          display: flex;
          flex-direction: column;
          align-items: center;
          width: 24px;
          flex-shrink: 0;
        }
        .iteration-dot {
          width: 12px; height: 12px;
          border-radius: 50%;
          background: var(--color-primary);
          border: 2px solid var(--color-primary-light);
          flex-shrink: 0;
        }
        .iteration-line {
          width: 2px;
          flex: 1;
          background: var(--color-border);
          margin-top: 4px;
        }
        .iteration-card {
          flex: 1;
          background: var(--color-surface);
          border: 1px solid var(--color-border);
          border-radius: var(--radius-md);
          padding: var(--space-md);
        }
        .iteration-card-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: var(--space-sm);
        }
        .iteration-number {
          font-weight: 700;
          font-size: var(--font-size-sm);
          color: var(--color-text);
        }
        .iteration-score {
          font-weight: 700;
          font-size: var(--font-size-sm);
          padding: 2px 10px;
          border-radius: var(--radius-full);
        }
        .iteration-score.pass { background: var(--color-success-light); color: var(--color-success); }
        .iteration-score.mid { background: var(--color-warning-light); color: var(--color-warning); }
        .iteration-score.fail { background: var(--color-danger-light); color: var(--color-danger); }
        .iteration-role-scores {
          display: flex;
          gap: 8px;
          flex-wrap: wrap;
        }
        .iteration-role-tag {
          font-size: var(--font-size-xs);
          padding: 1px 8px;
          background: var(--color-surface-alt);
          border-radius: var(--radius-full);
          color: var(--color-text-secondary);
        }
        .iteration-feedback-detail { margin-top: var(--space-sm); }
        .iteration-feedback-detail summary {
          font-size: var(--font-size-xs);
          color: var(--color-primary);
          cursor: pointer;
          font-weight: 500;
        }
        .iteration-feedback-text {
          margin-top: var(--space-sm);
          font-size: var(--font-size-xs);
          color: var(--color-text-secondary);
          white-space: pre-wrap;
          line-height: 1.6;
        }
      `}</style>
    </div>
  );
}
