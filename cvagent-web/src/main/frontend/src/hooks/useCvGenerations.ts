import { useState, useCallback, useRef } from 'react';
import * as api from '../api/cvGenerations';
import type { GeneratedCv, CvScoringResult, CvGenerationRecord, PageResult } from '../api/types';
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
  const [scoringResults, setScoringResults] = useState<CvScoringResult[]>([]);
  const [history, setHistory] = useState<CvGenerationRecord[]>([]);
  const [loadingScoring, setLoadingScoring] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [scoring, setScoring] = useState(false);
  const abortRef = useRef<AbortController | null>(null);

  const generate = useCallback(async (workExpId: number, templateId: number, jdId?: number) => {
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

  const loadScoringResults = useCallback(async (cvId: number) => {
    setLoadingScoring(true);
    try {
      const data = await api.listScoringResults(cvId);
      setScoringResults(data);
      return data;
    } catch {
      return [];
    } finally {
      setLoadingScoring(false);
    }
  }, []);

  const loadHistory = useCallback(async (cvId: number, srId: number) => {
    try {
      const data = await api.getScoringResultHistory(cvId, srId);
      setHistory(data);
      return data;
    } catch {
      setHistory([]);
      return [];
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

  const scoreCv = useCallback(async (id: number, jdId: number): Promise<CvScoringResult | null> => {
    setScoring(true);
    try {
      const sr = await api.scoreCv(id, jdId);
      // Poll until scoring completes
      const maxWait = 5 * 60 * 1000;
      const started = Date.now();
      let latest: CvScoringResult = sr;
      while ((latest.status === 'SCORING') && Date.now() - started < maxWait) {
        await new Promise(r => setTimeout(r, 3000));
        const results = await api.listScoringResults(id);
        const updated = results.find(r => r.id === sr.id);
        if (updated) latest = updated;
      }
      // Reload scoring results
      const all = await api.listScoringResults(id);
      setScoringResults(all);
      return latest;
    } catch {
      return null;
    } finally {
      setScoring(false);
    }
  }, []);

  const [optimizing, setOptimizing] = useState(false);

  const optimizeCv = useCallback(async (id: number, srId: number): Promise<string | null> => {
    setOptimizing(true);
    try {
      const resp = await api.optimizeCv(id, srId);
      return resp.optimizedContent;
    } catch {
      return null;
    } finally {
      setOptimizing(false);
    }
  }, []);

  const listCvs = useCallback(async (page: number = 1, size: number = 10): Promise<PageResult<GeneratedCv>> => {
    return api.listGeneratedCvs(page, size);
  }, []);

  const duplicateCv = useCallback(async (id: number): Promise<GeneratedCv | null> => {
    try {
      return await api.duplicateGeneratedCv(id);
    } catch {
      return null;
    }
  }, []);

  const reset = useCallback(() => {
    setState({ status: 'idle', error: null });
    setResult(null);
    setScoringResults([]);
    setHistory([]);
    abortRef.current?.abort();
  }, []);

  return {
    state, result, scoringResults, history,
    loadingScoring, updating, exporting, scoring, optimizing,
    generate, cancel, loadResult, loadScoringResults, loadHistory,
    updateContent, exportCv, scoreCv, optimizeCv, listCvs, duplicateCv, reset,
  };
}
