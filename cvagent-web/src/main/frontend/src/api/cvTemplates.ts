import { request, uploadFile } from './client';
import type { CvTemplate } from './types';

const BASE = '/cv-templates';

export function listTemplates(): Promise<CvTemplate[]> {
  return request<CvTemplate[]>(BASE);
}

export function getTemplate(id: number): Promise<CvTemplate> {
  return request<CvTemplate>(`${BASE}/${id}`);
}

export function createTemplate(data: Partial<CvTemplate>): Promise<CvTemplate> {
  return request<CvTemplate>(BASE, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updateTemplate(id: number, data: Partial<CvTemplate>): Promise<CvTemplate> {
  return request<CvTemplate>(`${BASE}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export function deleteTemplate(id: number): Promise<void> {
  return request<void>(`${BASE}/${id}`, { method: 'DELETE' });
}

export function importTemplate(file: File): Promise<CvTemplate> {
  return uploadFile<CvTemplate>(`${BASE}/import`, file);
}
