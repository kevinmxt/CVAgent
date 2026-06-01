import { request, uploadFile } from './client';
import type { JobDescription, PageResult } from './types';

const BASE = '/job-descriptions';

export function listJobDescriptions(page: number, size: number): Promise<PageResult<JobDescription>> {
  return request<PageResult<JobDescription>>(`${BASE}?page=${page}&size=${size}`);
}

export function getJobDescription(id: number): Promise<JobDescription> {
  return request<JobDescription>(`${BASE}/${id}`);
}

export function importJobDescription(file: File): Promise<JobDescription> {
  return uploadFile<JobDescription>(`${BASE}/import`, file);
}

export function updateJobDescription(id: number, data: Partial<JobDescription>): Promise<JobDescription> {
  return request<JobDescription>(`${BASE}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export function deleteJobDescription(id: number): Promise<void> {
  return request<void>(`${BASE}/${id}`, { method: 'DELETE' });
}
