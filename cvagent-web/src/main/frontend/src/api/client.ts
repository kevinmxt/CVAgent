import type { ApiErrorResponse } from './types';

const BASE = '/api/v1';

export class ApiError extends Error {
  code: number;

  constructor(code: number, message: string) {
    super(message);
    this.code = code;
    this.name = 'ApiError';
  }
}

async function parseError(res: Response): Promise<ApiError> {
  try {
    const body: ApiErrorResponse = await res.json();
    return new ApiError(body.code || res.status, body.message || res.statusText);
  } catch {
    return new ApiError(res.status, res.statusText || 'Unknown error');
  }
}

export async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const url = `${BASE}${path}`;
  const res = await fetch(url, {
    headers: {
      'Content-Type': 'application/json; charset=UTF-8',
    },
    ...options,
  });

  if (!res.ok) {
    throw await parseError(res);
  }

  if (res.status === 204) {
    return undefined as T;
  }

  return res.json();
}

export async function uploadFile<T>(path: string, file: File): Promise<T> {
  const formData = new FormData();
  formData.append('file', file);

  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    body: formData,
  });

  if (!res.ok) {
    throw await parseError(res);
  }

  return res.json();
}

export async function downloadBlob(path: string, method: 'GET' | 'POST' = 'GET'): Promise<{ blob: Blob; filename: string }> {
  const res = await fetch(`${BASE}${path}`, { method });

  if (!res.ok) {
    throw await parseError(res);
  }

  const blob = await res.blob();
  const disposition = res.headers.get('Content-Disposition');
  const match = disposition?.match(/filename="?(.+?)"?$/);
  const filename = match?.[1] || 'download';

  return { blob, filename };
}

export function triggerDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
