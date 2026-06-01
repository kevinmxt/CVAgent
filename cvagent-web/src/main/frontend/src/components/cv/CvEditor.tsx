import React, { useState } from 'react';

interface CvEditorProps {
  content: string;
  onSave: (content: string) => Promise<void>;
  saving: boolean;
}

export default function CvEditor({ content, onSave, saving }: CvEditorProps) {
  const [edited, setEdited] = useState(content);
  const [saved, setSaved] = useState(false);

  const handleSave = async () => {
    try {
      await onSave(edited);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch {
      // handled by parent
    }
  };

  return (
    <div className="cv-editor">
      <div className="cv-editor-toolbar">
        <span className="cv-editor-hint">直接编辑 HTML 内容，保存后状态将变为"已定稿"</span>
        <button className="btn btn-primary btn-sm" onClick={handleSave} disabled={saving || edited === content}>
          {saving ? '保存中...' : saved ? '已保存 ✓' : '保存为定稿'}
        </button>
      </div>
      <textarea
        className="cv-editor-textarea"
        value={edited}
        onChange={(e) => setEdited(e.target.value)}
        spellCheck={false}
      />
      <style>{`
        .cv-editor {
          display: flex;
          flex-direction: column;
          height: 800px;
        }
        .cv-editor-toolbar {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 10px 16px;
          background: var(--color-surface-alt);
          border: 1px solid var(--color-border);
          border-bottom: none;
          border-radius: var(--radius-md) var(--radius-md) 0 0;
        }
        .cv-editor-hint {
          font-size: var(--font-size-xs);
          color: var(--color-text-muted);
        }
        .cv-editor-textarea {
          flex: 1;
          padding: var(--space-md);
          font-family: var(--font-mono);
          font-size: var(--font-size-xs);
          line-height: 1.6;
          border: 1px solid var(--color-border);
          border-radius: 0 0 var(--radius-md) var(--radius-md);
          resize: none;
          outline: none;
          background: #fafbfc;
          color: var(--color-text);
          tab-size: 2;
        }
        .cv-editor-textarea:focus {
          border-color: var(--color-primary);
          box-shadow: 0 0 0 3px var(--color-primary-light);
        }
      `}</style>
    </div>
  );
}
