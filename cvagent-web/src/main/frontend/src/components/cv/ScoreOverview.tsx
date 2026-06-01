import React from 'react';
import ScoreGauge from '../common/ScoreGauge';
import { DEFAULT_PASS_SCORE } from '../../utils/constants';

interface ScoreOverviewProps {
  finalScore: number;
  threshold?: number;
  status: string;
  iterationCount: number;
}

export default function ScoreOverview({ finalScore, threshold = DEFAULT_PASS_SCORE, status, iterationCount }: ScoreOverviewProps) {
  const passed = finalScore >= threshold;

  return (
    <div className="score-overview">
      <ScoreGauge score={finalScore} threshold={threshold} size="lg" />
      <div className="score-overview-info">
        <div>
          <span className="score-overview-label">匹配阈值</span>
          <span className="score-overview-value">{Math.round(threshold * 100)} 分</span>
        </div>
        <div>
          <span className="score-overview-label">迭代次数</span>
          <span className="score-overview-value">{iterationCount} 次</span>
        </div>
        <div>
          <span className="score-overview-label">状态</span>
          <span className={`badge ${status === 'DRAFT' ? 'badge-info' : status === 'FINAL' ? 'badge-success' : 'badge-neutral'}`}>
            {status === 'DRAFT' ? '草稿' : status === 'FINAL' ? '已定稿' : '已导出'}
          </span>
        </div>
        <div style={{ marginTop: 8 }}>
          <span className={`badge ${passed ? 'badge-success' : 'badge-warning'}`} style={{ fontSize: 'var(--font-size-base)', padding: '4px 16px' }}>
            {passed ? '通过阈值' : '未达阈值，需改进'}
          </span>
        </div>
      </div>
      <style>{`
        .score-overview {
          display: flex;
          align-items: center;
          gap: var(--space-xl);
          padding: var(--space-xl);
          flex-wrap: wrap;
          justify-content: center;
        }
        .score-overview-info {
          display: flex;
          flex-direction: column;
          gap: var(--space-sm);
        }
        .score-overview-label {
          font-size: var(--font-size-xs);
          color: var(--color-text-muted);
          margin-right: var(--space-sm);
        }
        .score-overview-value {
          font-weight: 600;
          color: var(--color-text);
          font-size: var(--font-size-sm);
        }
      `}</style>
    </div>
  );
}
