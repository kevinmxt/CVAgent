export const API_BASE = '/api/v1';
export const DEFAULT_PAGE_SIZE = 10;
export const DEFAULT_PASS_SCORE = 0.8;
export const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
export const ALLOWED_FILE_TYPES = ['.txt', '.docx', '.html', '.pdf'];
export const ALLOWED_MIME_TYPES = [
  'text/plain',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/html',
  'application/pdf',
];
export const GENERATION_TIMEOUT_SECONDS = 180;
export const GENERATION_WARNING_SECONDS = 30;
