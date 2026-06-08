import { useState, useEffect, useCallback } from 'react';
import * as api from '../api/cvTemplates';
import type { CvTemplate } from '../api/types';

export function useCvTemplates() {
  const [templates, setTemplates] = useState<CvTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(() => {
    setLoading(true);
    setError(null);
    api.listTemplates()
      .then(setTemplates)
      .catch((e) => setError(e.message || '加载失败'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { fetch(); }, [fetch]);

  const create = useCallback(async (data: Partial<CvTemplate>) => {
    await api.createTemplate(data);
    fetch();
  }, [fetch]);

  const update = useCallback(async (id: number, data: Partial<CvTemplate>) => {
    await api.updateTemplate(id, data);
    fetch();
  }, [fetch]);

  const remove = useCallback(async (id: number) => {
    await api.deleteTemplate(id);
    fetch();
  }, [fetch]);

  const importFile = useCallback(async (file: File) => {
    await api.importTemplate(file);
    fetch();
  }, [fetch]);

  const duplicate = useCallback(async (id: number) => {
    await api.duplicateTemplate(id);
    fetch();
  }, [fetch]);

  return { templates, loading, error, refetch: fetch, create, update, remove, importFile, duplicate };
}
