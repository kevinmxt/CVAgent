import { useState, useEffect, useCallback } from 'react';
import * as api from '../api/jobDescriptions';
import type { JobDescription, PageResult } from '../api/types';

export function useJobDescriptions(page: number, size: number) {
  const [data, setData] = useState<PageResult<JobDescription> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(() => {
    setLoading(true);
    setError(null);
    api.listJobDescriptions(page, size)
      .then(setData)
      .catch((e) => setError(e.message || '加载失败'))
      .finally(() => setLoading(false));
  }, [page, size]);

  useEffect(() => { fetch(); }, [fetch]);

  const importFile = useCallback(async (file: File) => {
    await api.importJobDescription(file);
    fetch();
  }, [fetch]);

  const update = useCallback(async (id: number, d: Partial<JobDescription>) => {
    await api.updateJobDescription(id, d);
    fetch();
  }, [fetch]);

  const remove = useCallback(async (id: number) => {
    await api.deleteJobDescription(id);
    fetch();
  }, [fetch]);

  return { data, loading, error, refetch: fetch, importFile, update, remove };
}
