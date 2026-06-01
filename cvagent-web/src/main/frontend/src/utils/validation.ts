import { ALLOWED_MIME_TYPES, MAX_FILE_SIZE } from './constants';

export interface ValidationResult {
  valid: boolean;
  error?: string;
}

export function validateFile(file: File): ValidationResult {
  if (file.size === 0) {
    return { valid: false, error: '文件为空' };
  }

  if (file.size > MAX_FILE_SIZE) {
    return { valid: false, error: `文件过大，最大支持 ${MAX_FILE_SIZE / 1024 / 1024}MB` };
  }

  if (!ALLOWED_MIME_TYPES.includes(file.type) && file.type !== '') {
    return { valid: false, error: `不支持的文件类型: ${file.type || '未知'}` };
  }

  return { valid: true };
}

export function getFileExtension(filename: string): string {
  const idx = filename.lastIndexOf('.');
  return idx >= 0 ? filename.slice(idx).toLowerCase() : '';
}

export function isAllowedFileType(filename: string): boolean {
  const ext = getFileExtension(filename);
  return ['.txt', '.docx', '.html', '.pdf'].includes(ext);
}
