import React from 'react';

interface ScoreGaugeProps {
  score: number;       // 0-1
  threshold: number;   // pass score threshold (e.g., 0.8)
  size?: 'sm' | 'md' | 'lg';
  label?: string;
}

const SIZES = { sm: 80, md: 140, lg: 200 };

export default function ScoreGauge({ score, threshold, size = 'lg', label }: ScoreGaugeProps) {
  const dim = SIZES[size];
  const strokeW = size === 'sm' ? 6 : size === 'md' ? 10 : 14;
  const r = (dim - strokeW) / 2;
  const circumference = 2 * Math.PI * r;
  const offset = circumference * (1 - score);

  const color = score >= threshold
    ? 'var(--score-pass)'
    : score >= 0.5
      ? 'var(--score-mid)'
      : 'var(--score-fail)';

  const passed = score >= threshold;

  return (
    <div className="score-gauge" style={{ width: dim }}>
      <svg viewBox={`0 0 ${dim} ${dim}`} width={dim} height={dim}>
        <circle
          cx={dim / 2} cy={dim / 2} r={r}
          fill="none"
          stroke="var(--color-border-light)"
          strokeWidth={strokeW}
        />
        <circle
          cx={dim / 2} cy={dim / 2} r={r}
          fill="none"
          stroke={color}
          strokeWidth={strokeW}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
          transform={`rotate(-90 ${dim / 2} ${dim / 2})`}
          style={{ transition: 'stroke-dashoffset 1.2s cubic-bezier(0.16, 1, 0.3, 1)' }}
        />
        {/* Threshold marker */}
        {size !== 'sm' && (
          <line
            x1={dim / 2} y1={strokeW / 2}
            x2={dim / 2} y2={strokeW / 2 + 8}
            stroke="var(--color-text-muted)"
            strokeWidth={2}
            transform={`rotate(${-90 + threshold * 360} ${dim / 2} ${dim / 2})`}
            style={{ opacity: 0.6 }}
          />
        )}
      </svg>
      <div className="score-gauge-center">
        <span className="score-value" style={{ color }}>{Math.round(score * 100)}</span>
        <span className="score-unit">分</span>
        {label && <span className="score-label">{label}</span>}
        {size !== 'sm' && (
          <span className={`score-status ${passed ? 'pass' : 'fail'}`}>
            {passed ? '✓ 通过' : '需改进'}
          </span>
        )}
      </div>
      <style>{`
        .score-gauge {
          position: relative;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .score-gauge-center {
          position: absolute;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 2px;
        }
        .score-value { font-size: var(--font-size-3xl); font-weight: 800; line-height: 1; }
        .score-unit { font-size: var(--font-size-xs); color: var(--color-text-muted); }
        .score-label { font-size: var(--font-size-xs); color: var(--color-text-secondary); margin-top: 2px; }
        .score-status {
          font-size: var(--font-size-sm);
          font-weight: 600;
          padding: 2px 12px;
          border-radius: var(--radius-full);
          margin-top: 4px;
        }
        .score-status.pass { background: var(--color-success-light); color: var(--color-success); }
        .score-status.fail { background: var(--color-warning-light); color: var(--color-warning); }
      `}</style>
    </div>
  );
}
