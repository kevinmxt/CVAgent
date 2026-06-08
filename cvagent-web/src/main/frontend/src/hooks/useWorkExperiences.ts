import { useState, useEffect, useCallback } from 'react';
import * as api from '../api/workExperiences';
import type { WorkExperience, PageResult } from '../api/types';

export function useWorkExperiences(page: number, size: number) {
  const [data, setData] = useState<PageResult<WorkExperience> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(() => {
    setLoading(true);
    setError(null);
    api.listWorkExperiences(page, size)
      .then(setData)
      .catch((e) => setError(e.message || '加载失败'))
      .finally(() => setLoading(false));
  }, [page, size]);

  useEffect(() => { fetch(); }, [fetch]);

  const importFile = useCallback(async (file: File) => {
    await api.importWorkExperience(file);
    fetch();
  }, [fetch]);

  const update = useCallback(async (id: number, d: Partial<WorkExperience>) => {
    await api.updateWorkExperience(id, d);
    fetch();
  }, [fetch]);

  const remove = useCallback(async (id: number) => {
    await api.deleteWorkExperience(id);
    fetch();
  }, [fetch]);

  const duplicate = useCallback(async (id: number) => {
    await api.duplicateWorkExperience(id);
    fetch();
  }, [fetch]);

  return { data, loading, error, refetch: fetch, importFile, update, remove, duplicate };
}
