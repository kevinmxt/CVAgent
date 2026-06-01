import React from 'react';

interface CvPreviewProps {
  htmlContent: string;
}

export default function CvPreview({ htmlContent }: CvPreviewProps) {
  if (!htmlContent) {
    return (
      <div className="preview-empty">
        <p>暂无简历内容</p>
      </div>
    );
  }

  return (
    <div className="cv-preview-container">
      <iframe
        className="cv-preview-frame"
        srcDoc={htmlContent}
        sandbox="allow-same-origin"
        title="简历预览"
      />
      <style>{`
        .cv-preview-container {
          border: 1px solid var(--color-border);
          border-radius: var(--radius-md);
          overflow: hidden;
          background: #fff;
        }
        .cv-preview-frame {
          width: 100%;
          height: 800px;
          border: none;
        }
        .preview-empty {
          text-align: center;
          padding: var(--space-2xl);
          color: var(--color-text-muted);
        }
      `}</style>
    </div>
  );
}
