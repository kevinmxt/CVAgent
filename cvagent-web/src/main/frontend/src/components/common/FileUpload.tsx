import React, { useRef, useState, useCallback } from 'react';
import { validateFile } from '../../utils/validation';
import { formatFileSize } from '../../utils/format';

interface FileUploadProps {
  onUpload: (file: File) => Promise<void>;
  accept?: string;
  label?: string;
  uploading?: boolean;
}

export default function FileUpload({ onUpload, accept = '.txt,.docx,.html,.pdf', label = '导入文件', uploading = false }: FileUploadProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragOver, setDragOver] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const handleFile = useCallback(async (file: File) => {
    const result = validateFile(file);
    if (!result.valid) {
      setError(result.error!);
      setSelectedFile(null);
      return;
    }
    setError(null);
    setSelectedFile(file);
    try {
      await onUpload(file);
      setSelectedFile(null);
    } catch {
      // error handled by parent via toast
      setSelectedFile(null);
    }
  }, [onUpload]);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  }, [handleFile]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleFile(file);
  };

  return (
    <div className="file-upload-wrapper">
      <div
        className={`file-upload-zone ${dragOver ? 'drag-over' : ''} ${uploading ? 'uploading' : ''}`}
        onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
        onDragLeave={() => setDragOver(false)}
        onDrop={handleDrop}
        onClick={() => !uploading && inputRef.current?.click()}
      >
        <input ref={inputRef} type="file" accept={accept} onChange={handleChange} style={{ display: 'none' }} />
        {uploading ? (
          <>
            <div className="upload-spinner" />
            <span>正在导入 {selectedFile?.name}...</span>
          </>
        ) : (
          <>
            <div className="upload-icon">⬆</div>
            <span className="upload-text">{label}</span>
            <span className="upload-hint">支持 TXT、DOCX、HTML、PDF 格式，拖拽文件到此处</span>
          </>
        )}
      </div>
      {error && <div className="form-error" style={{ marginTop: 8 }}>{error}</div>}
      <style>{`
        .file-upload-wrapper { width: 100%; }
        .file-upload-zone {
          border: 2px dashed var(--color-border);
          border-radius: var(--radius-lg);
          padding: var(--space-xl);
          text-align: center;
          cursor: pointer;
          transition: all var(--transition-fast);
          background: var(--color-surface);
        }
        .file-upload-zone:hover {
          border-color: var(--color-primary);
          background: var(--color-primary-light);
        }
        .file-upload-zone.drag-over {
          border-color: var(--color-primary);
          background: var(--color-primary-light);
          box-shadow: 0 0 0 3px var(--color-primary-light);
        }
        .file-upload-zone.uploading {
          cursor: not-allowed;
          opacity: 0.7;
        }
        .upload-icon { font-size: 1.5rem; margin-bottom: var(--space-sm); opacity: 0.6; }
        .upload-text { display: block; font-weight: 600; color: var(--color-text); margin-bottom: var(--space-xs); }
        .upload-hint { display: block; font-size: var(--font-size-xs); color: var(--color-text-muted); }
        .upload-spinner {
          width: 24px; height: 24px;
          border: 2px solid var(--color-border);
          border-top-color: var(--color-primary);
          border-radius: 50%;
          animation: spin 0.8s linear infinite;
          margin: 0 auto var(--space-sm);
        }
      `}</style>
    </div>
  );
}
