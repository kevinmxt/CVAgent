import { request, uploadFile } from './client';
import type { WorkExperience, PageResult } from './types';

const BASE = '/work-experiences';

export function listWorkExperiences(page: number, size: number): Promise<PageResult<WorkExperience>> {
  return request<PageResult<WorkExperience>>(`${BASE}?page=${page}&size=${size}`);
}

export function getWorkExperience(id: number): Promise<WorkExperience> {
  return request<WorkExperience>(`${BASE}/${id}`);
}

export function importWorkExperience(file: File): Promise<WorkExperience> {
  return uploadFile<WorkExperience>(`${BASE}/import`, file);
}

export function updateWorkExperience(id: number, data: Partial<WorkExperience>): Promise<WorkExperience> {
  return request<WorkExperience>(`${BASE}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export function duplicateWorkExperience(id: number): Promise<WorkExperience> {
  return request<WorkExperience>(`${BASE}/${id}/duplicate`, { method: 'POST' });
}

export function deleteWorkExperience(id: number): Promise<void> {
  return request<void>(`${BASE}/${id}`, { method: 'DELETE' });
}
