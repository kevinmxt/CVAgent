import React, { useEffect, useState } from 'react';
import { GENERATION_WARNING_SECONDS } from '../../utils/constants';

interface GenerationProgressProps {
  onCancel: () => void;
}

export default function GenerationProgress({ onCancel }: GenerationProgressProps) {
  const [elapsed, setElapsed] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => setElapsed((s) => s + 1), 1000);
    return () => clearInterval(timer);
  }, []);

  const isLong = elapsed > GENERATION_WARNING_SECONDS;

  return (
    <div className="generation-progress">
      <div className="progress-pulse">
        <div className="progress-ring">
          <div className="progress-ring-inner">✦</div>
        </div>
      </div>
      <h3 className="progress-title">正在生成简历...</h3>
      <p className="progress-desc">
        AI 正在从多角色视角审阅您的简历，并针对岗位描述进行优化。请耐心等待。
      </p>
      <div className="progress-stages">
        <div className="progress-stage done">分析简历内容</div>
        <div className={`progress-stage ${elapsed > 5 ? 'done' : elapsed > 2 ? 'active' : ''}`}>多角色评审中</div>
        <div className={`progress-stage ${elapsed > 15 ? 'done' : elapsed > 8 ? 'active' : ''}`}>优化生成</div>
      </div>
      {isLong && (
        <div className="progress-warning">
          生成时间较长（已等待 {elapsed} 秒），请耐心等待或取消后重试。
        </div>
      )}
      <button className="btn btn-outline" onClick={onCancel} style={{ marginTop: 16 }}>
        取消生成
      </button>
      <style>{`
        .generation-progress {
          text-align: center;
          padding: var(--space-2xl);
        }
        .progress-pulse {
          display: flex;
          justify-content: center;
          margin-bottom: var(--space-lg);
        }
        .progress-ring {
          width: 72px; height: 72px;
          border-radius: 50%;
          background: var(--color-primary-light);
          display: flex;
          align-items: center;
          justify-content: center;
          animation: pulse 2s ease-in-out infinite;
        }
        .progress-ring-inner {
          font-size: 1.5rem;
          animation: spin 3s linear infinite;
        }
        .progress-title {
          font-size: var(--font-size-xl);
          font-weight: 700;
          margin-bottom: var(--space-sm);
          color: var(--color-text);
        }
        .progress-desc {
          font-size: var(--font-size-sm);
          color: var(--color-text-muted);
          margin-bottom: var(--space-xl);
          max-width: 400px;
          margin-left: auto;
          margin-right: auto;
        }
        .progress-stages {
          display: flex;
          justify-content: center;
          gap: var(--space-xl);
          margin-bottom: var(--space-md);
        }
        .progress-stage {
          font-size: var(--font-size-xs);
          color: var(--color-text-muted);
          padding: 4px 12px;
          border-radius: var(--radius-full);
          background: var(--color-surface-alt);
          transition: all var(--transition-normal);
        }
        .progress-stage.active {
          background: var(--color-primary-light);
          color: var(--color-primary);
          font-weight: 600;
          animation: pulse 1.5s ease-in-out infinite;
        }
        .progress-stage.done {
          background: var(--color-success-light);
          color: var(--color-success);
        }
        .progress-warning {
          margin-top: var(--space-md);
          font-size: var(--font-size-sm);
          color: var(--color-warning);
          background: var(--color-warning-light);
          padding: 8px 16px;
          border-radius: var(--radius-md);
          display: inline-block;
        }
      `}</style>
    </div>
  );
}
