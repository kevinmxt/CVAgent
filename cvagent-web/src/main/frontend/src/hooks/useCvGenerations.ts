import { useState, useCallback, useRef } from 'react';
import * as api from '../api/cvGenerations';
import type { GeneratedCv, CvGenerationRecord, PageResult } from '../api/types';
import { ApiError } from '../api/client';
import { GENERATION_TIMEOUT_SECONDS } from '../utils/constants';

export type GenerationStatus = 'idle' | 'generating' | 'success' | 'error';

export interface GenerationState {
  status: GenerationStatus;
  error: string | null;
}

export function useCvGenerations() {
  const [state, setState] = useState<GenerationState>({ status: 'idle', error: null });
  const [result, setResult] = useState<GeneratedCv | null>(null);
  const [history, setHistory] = useState<CvGenerationRecord[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [scoring, setScoring] = useState(false);
  const abortRef = useRef<AbortController | null>(null);

  const generate = useCallback(async (workExpId: number, templateId: number, jdId: number) => {
    const controller = new AbortController();
    abortRef.current = controller;

    setState({ status: 'generating', error: null });
    setResult(null);

    try {
      const timeout = setTimeout(() => controller.abort(), GENERATION_TIMEOUT_SECONDS * 1000);
      const data = await api.generateCv({ workExpId, templateId, jdId }, controller.signal);
      clearTimeout(timeout);
      setResult(data);
      setState({ status: 'success', error: null });
      return data;
    } catch (e) {
      if (e instanceof DOMException && e.name === 'AbortError') {
        setState({ status: 'error', error: '生成已取消或超时' });
      } else if (e instanceof ApiError) {
        setState({ status: 'error', error: e.message });
      } else {
        setState({ status: 'error', error: '生成失败，请重试' });
      }
      return null;
    } finally {
      abortRef.current = null;
    }
  }, []);

  const cancel = useCallback(() => {
    abortRef.current?.abort();
  }, []);

  const loadResult = useCallback(async (id: number) => {
    setState({ status: 'idle', error: null });
    try {
      const data = await api.getGeneratedCv(id);
      setResult(data);
      setState({ status: 'success', error: null });
      return data;
    } catch (e) {
      setState({ status: 'error', error: e instanceof ApiError ? e.message : '加载失败' });
      return null;
    }
  }, []);

  const loadHistory = useCallback(async (id: number) => {
    setLoadingHistory(true);
    try {
      const data = await api.getGenerationHistory(id);
      setHistory(data);
    } catch {
      // silently fail for history
    } finally {
      setLoadingHistory(false);
    }
  }, []);

  const updateContent = useCallback(async (id: number, finalContent: string) => {
    setUpdating(true);
    try {
      const data = await api.updateCvContent(id, { finalContent });
      setResult(data);
      return data;
    } catch (e) {
      throw e;
    } finally {
      setUpdating(false);
    }
  }, []);

  const exportCv = useCallback(async (id: number) => {
    setExporting(true);
    try {
      await api.exportCv(id);
      if (result) setResult({ ...result, status: 'EXPORTED' });
    } finally {
      setExporting(false);
    }
  }, [result]);

  /** Trigger async scoring, then poll until complete */
  const scoreCv = useCallback(async (id: number): Promise<GeneratedCv | null> => {
    setScoring(true);
    try {
      await api.scoreCv(id);
      // Poll every 3s until scoring finishes (finalScore != null)
      let data: GeneratedCv = await api.getGeneratedCv(id);
      const maxWait = 5 * 60 * 1000; // 5 min max
      const started = Date.now();
      while ((data.finalScore == null || data.status === 'SCORING') && Date.now() - started < maxWait) {
        await new Promise(r => setTimeout(r, 3000));
        data = await api.getGeneratedCv(id);
      }
      setResult(data);
      return data;
    } catch (e) {
      // Scoring triggered, but may have failed server-side; reload to get current state
      try {
        const latest = await api.getGeneratedCv(id);
        setResult(latest);
      } catch {}
      return null;
    } finally {
      setScoring(false);
    }
  }, []);

  /** Load paginated list of generated CVs */
  const listCvs = useCallback(async (page: number = 1, size: number = 10): Promise<PageResult<GeneratedCv>> => {
    return api.listGeneratedCvs(page, size);
  }, []);

  const reset = useCallback(() => {
    setState({ status: 'idle', error: null });
    setResult(null);
    setHistory([]);
    abortRef.current?.abort();
  }, []);

  return {
    state,
    result,
    history,
    loadingHistory,
    updating,
    exporting,
    scoring,
    generate,
    cancel,
    loadResult,
    loadHistory,
    updateContent,
    exportCv,
    scoreCv,
    listCvs,
    reset,
  };
}
