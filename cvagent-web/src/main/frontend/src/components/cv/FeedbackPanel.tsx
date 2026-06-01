import React from 'react';

interface FeedbackPanelProps {
  feedback: string;
  threshold: number;
  finalScore: number;
}

export default function FeedbackPanel({ feedback, threshold, finalScore }: FeedbackPanelProps) {
  if (!feedback) return null;

  const sections = feedback.split(/(?=【)/g).filter(Boolean);

  return (
    <div className="feedback-panel">
      <h3 className="feedback-title">
        {finalScore >= threshold ? '评审反馈' : '改进建议'}
      </h3>
      {finalScore < threshold && (
        <div className="feedback-warning">
          当前评分 {Math.round(finalScore * 100)} 分，未达到通过阈值 {Math.round(threshold * 100)} 分。请根据以下反馈优化简历后重新生成，或在编辑器中直接修改。
        </div>
      )}
      <div className="feedback-content">
        {sections.length > 0 ? (
          sections.map((section, i) => {
            const match = section.match(/^【(.+?)】/);
            const roleName = match ? match[1] : '';
            const body = match ? section.slice(match[0].length) : section;
            return (
              <div key={i} className="feedback-section">
                {roleName && <h4 className="feedback-role">{roleName}</h4>}
                <p className="feedback-body">{body.trim()}</p>
              </div>
            );
          })
        ) : (
          <p className="feedback-body">{feedback}</p>
        )}
      </div>
      <style>{`
        .feedback-panel {
          padding: var(--space-xl);
        }
        .feedback-title {
          font-size: var(--font-size-base);
          font-weight: 600;
          margin-bottom: var(--space-md);
        }
        .feedback-warning {
          background: var(--color-warning-light);
          border: 1px solid #fed7aa;
          border-radius: var(--radius-md);
          padding: 12px 16px;
          font-size: var(--font-size-sm);
          color: #9a3412;
          margin-bottom: var(--space-md);
        }
        .feedback-content {
          font-size: var(--font-size-sm);
          line-height: 1.8;
          color: var(--color-text-secondary);
        }
        .feedback-section {
          margin-bottom: var(--space-md);
          padding: var(--space-md);
          background: var(--color-surface-alt);
          border-radius: var(--radius-md);
          border-left: 3px solid var(--color-primary);
        }
        .feedback-role {
          font-size: var(--font-size-sm);
          font-weight: 700;
          color: var(--color-text);
          margin-bottom: var(--space-xs);
        }
        .feedback-body {
          white-space: pre-wrap;
        }
      `}</style>
    </div>
  );
}
