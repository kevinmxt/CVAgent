import React from 'react';
import { parseRoleScores, type RoleScores } from '../../api/types';

interface RoleScoresBreakdownProps {
  roleScoresJson: string;
}

const ROLE_LABELS: Record<string, string> = {
  hr: 'HR 视角',
  techExpert: '技术专家',
  teamLeader: '团队领导',
};

const ROLE_COLORS: Record<string, string> = {
  hr: 'var(--role-hr)',
  techExpert: 'var(--role-tech)',
  teamLeader: 'var(--role-lead)',
};

export default function RoleScoresBreakdown({ roleScoresJson }: RoleScoresBreakdownProps) {
  const scores: RoleScores = parseRoleScores(roleScoresJson);
  const entries = Object.entries(scores).sort(([, a], [, b]) => b - a);

  if (entries.length === 0) return null;

  return (
    <div className="role-scores">
      <h3 className="role-scores-title">各角色评分</h3>
      {entries.map(([key, score]) => {
        const pct = Math.round(score * 100);
        const color = ROLE_COLORS[key] || 'var(--color-primary)';
        return (
          <div key={key} className="role-score-row">
            <span className="role-score-label">{ROLE_LABELS[key] || key}</span>
            <div className="role-score-bar-track">
              <div
                className="role-score-bar-fill animate-fade-in"
                style={{ width: `${pct}%`, background: color }}
              />
            </div>
            <span className="role-score-value" style={{ color }}>{pct} 分</span>
          </div>
        );
      })}
      <style>{`
        .role-scores {
          padding: var(--space-xl);
        }
        .role-scores-title {
          font-size: var(--font-size-base);
          font-weight: 600;
          margin-bottom: var(--space-lg);
          color: var(--color-text);
        }
        .role-score-row {
          display: flex;
          align-items: center;
          gap: var(--space-md);
          margin-bottom: var(--space-md);
        }
        .role-score-label {
          width: 90px;
          font-size: var(--font-size-sm);
          font-weight: 500;
          color: var(--color-text-secondary);
          flex-shrink: 0;
        }
        .role-score-bar-track {
          flex: 1;
          height: 8px;
          background: var(--color-surface-alt);
          border-radius: var(--radius-full);
          overflow: hidden;
        }
        .role-score-bar-fill {
          height: 100%;
          border-radius: var(--radius-full);
          transition: width 1s cubic-bezier(0.16, 1, 0.3, 1);
          min-width: 2px;
        }
        .role-score-value {
          width: 48px;
          text-align: right;
          font-weight: 700;
          font-size: var(--font-size-sm);
          flex-shrink: 0;
        }
      `}</style>
    </div>
  );
}
