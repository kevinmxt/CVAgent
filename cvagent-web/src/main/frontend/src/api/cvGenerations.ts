import { request, downloadBlob, triggerDownload } from './client';
import type { GeneratedCv, CvGenerationRecord, CvGenerateRequest, CvContentUpdateRequest, PageResult } from './types';

const BASE = '/cv-generations';

export function generateCv(data: CvGenerateRequest, signal?: AbortSignal): Promise<GeneratedCv> {
  return request<GeneratedCv>(`${BASE}/generate`, {
    method: 'POST',
    body: JSON.stringify(data),
    signal,
  });
}

export function listGeneratedCvs(page: number = 1, size: number = 10): Promise<PageResult<GeneratedCv>> {
  return request<PageResult<GeneratedCv>>(`${BASE}?page=${page}&size=${size}`);
}

export function scoreCv(id: number): Promise<GeneratedCv> {
  return request<GeneratedCv>(`${BASE}/${id}/score`, { method: 'POST' });
}

export function getGeneratedCv(id: number): Promise<GeneratedCv> {
  return request<GeneratedCv>(`${BASE}/${id}`);
}

export function getGenerationHistory(id: number): Promise<CvGenerationRecord[]> {
  return request<CvGenerationRecord[]>(`${BASE}/${id}/history`);
}

export function updateCvContent(id: number, data: CvContentUpdateRequest): Promise<GeneratedCv> {
  return request<GeneratedCv>(`${BASE}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function exportCv(id: number): Promise<void> {
  const { blob, filename } = await downloadBlob(`${BASE}/${id}/export`, 'POST');
  triggerDownload(blob, filename);
}

export function deleteGeneratedCv(id: number): Promise<void> {
  return request<void>(`${BASE}/${id}`, { method: 'DELETE' });
}
