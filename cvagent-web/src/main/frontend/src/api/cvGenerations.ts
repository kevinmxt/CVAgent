import { request, downloadBlob, triggerDownload } from './client';
import type { GeneratedCv, CvScoringResult, CvGenerationRecord, CvGenerateRequest, CvContentUpdateRequest, PageResult } from './types';

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

export function scoreCv(id: number, jdId: number): Promise<CvScoringResult> {
  return request<CvScoringResult>(`${BASE}/${id}/score?jdId=${jdId}`, { method: 'POST' });
}

export function listScoringResults(cvId: number): Promise<CvScoringResult[]> {
  return request<CvScoringResult[]>(`${BASE}/${cvId}/scoring-results`);
}

export function getScoringResultHistory(cvId: number, srId: number): Promise<CvGenerationRecord[]> {
  return request<CvGenerationRecord[]>(`${BASE}/${cvId}/scoring-results/${srId}/history`);
}

export function optimizeCv(id: number, srId: number): Promise<{ optimizedContent: string }> {
  return request<{ optimizedContent: string }>(`${BASE}/${id}/optimize?srId=${srId}`, { method: 'POST' });
}

export function getGeneratedCv(id: number): Promise<GeneratedCv> {
  return request<GeneratedCv>(`${BASE}/${id}`);
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

export function duplicateGeneratedCv(id: number): Promise<GeneratedCv> {
  return request<GeneratedCv>(`${BASE}/${id}/duplicate`, { method: 'POST' });
}

export function deleteGeneratedCv(id: number): Promise<void> {
  return request<void>(`${BASE}/${id}`, { method: 'DELETE' });
}
