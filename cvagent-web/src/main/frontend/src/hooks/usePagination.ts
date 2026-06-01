import { useState, useCallback } from 'react';
import { DEFAULT_PAGE_SIZE } from '../utils/constants';

export function usePagination(initialSize = DEFAULT_PAGE_SIZE) {
  const [page, setPage] = useState(1);
  const [size] = useState(initialSize);

  const resetPage = useCallback(() => setPage(1), []);

  return { page, size, setPage, resetPage };
}
