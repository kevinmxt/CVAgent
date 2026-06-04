import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ScoreOverview from '../../components/cv/ScoreOverview';
import RoleScoresBreakdown from '../../components/cv/RoleScoresBreakdown';
import FeedbackPanel from '../../components/cv/FeedbackPanel';
import CvPreview from '../../components/cv/CvPreview';
import CvEditor from '../../components/cv/CvEditor';
import IterationHistory from '../../components/cv/IterationHistory';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import { useCvGenerations } from '../../hooks/useCvGenerations';
import { useJobDescriptions } from '../../hooks/useJobDescriptions';
import { useToast } from '../../context/ToastContext';
import { DEFAULT_PASS_SCORE } from '../../utils/constants';
import type { CvScoringResult, CvGenerationRecord } from '../../api/types';

export default function CvResultPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { addToast } = useToast();
  const { state, result, scoringResults, history, updating, exporting, scoring, optimizing, loadResult, loadScoringResults, loadHistory, updateContent, exportCv, scoreCv, optimizeCv } = useCvGenerations();
  const { data: jdData } = useJobDescriptions(1, 50);
  const [activeTab, setActiveTab] = useState<'preview' | 'edit'>('preview');
  const [selectedJdId, setSelectedJdId] = useState<number | ''>('');
  const [expandedSrId, setExpandedSrId] = useState<number | null>(null);
  const [expandedHistory, setExpandedHistory] = useState<CvGenerationRecord[]>([]);
  const [optimizedContent, setOptimizedContent] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      loadResult(Number(id));
      loadScoringResults(Number(id));
    }
  }, [id, loadResult, loadScoringResults]);

  // Poll scoring results when any are SCORING
  useEffect(() => {
    const hasScoring = scoringResults.some(sr => sr.status === 'SCORING');
    if (!hasScoring) return;
    const interval = setInterval(() => {
      if (id) loadScoringResults(Number(id));
    }, 3000);
    return () => clearInterval(interval);
  }, [scoringResults, id, loadScoringResults]);

  const handleExport = async () => {
    if (!result) return;
    try {
      await exportCv(result.id);
      addToast('success', '简历已导出');
    } catch (e: any) {
      addToast('error', e.message || '导出失败');
    }
  };

  const handleScore = async () => {
    if (!result || selectedJdId === '') return;
    try {
      await scoreCv(result.id, selectedJdId as number);
      addToast('success', '评分完成');
    } catch (e: any) {
      addToast('error', e.message || '评分失败');
    }
  };

  const handleToggleExpand = async (sr: CvScoringResult) => {
    if (expandedSrId === sr.id) {
      setExpandedSrId(null);
      setExpandedHistory([]);
    } else {
      setExpandedSrId(sr.id);
      if (id) {
        const h = await loadHistory(Number(id), sr.id);
        setExpandedHistory(h);
      }
    }
  };

  const handleOptimize = async (srId: number) => {
    if (!result) return;
    const optimized = await optimizeCv(result.id, srId);
    if (optimized) {
      setOptimizedContent(optimized);
    } else {
      addToast('error', '优化失败，请重试');
    }
  };

  const handleSaveOptimized = async () => {
    if (!result || !optimizedContent) return;
    try {
      await updateContent(result.id, optimizedContent);
      setOptimizedContent(null);
      addToast('success', '优化内容已保存');
      loadResult(result.id);
    } catch (e: any) {
      addToast('error', e.message || '保存失败');
    }
  };

  const handleDiscardOptimized = () => {
    setOptimizedContent(null);
  };

  const handleSaveContent = async (content: string) => {
    if (!result) return;
    try {
      await updateContent(result.id, content);
      addToast('success', '简历已定稿');
    } catch (e: any) {
      addToast('error', e.message || '保存失败');
      throw e;
    }
  };

  if (state.status === 'error' && !result) {
    return (
      <div className="page">
        <ErrorState message={state.error || '加载失败'} onRetry={() => id && loadResult(Number(id))} />
      </div>
    );
  }

  if (!result && state.status !== 'error') {
    return (
      <div className="page">
        <LoadingSpinner size="lg" text="加载生成结果..." />
      </div>
    );
  }

  if (!result) return null;

  const jds = jdData?.items || [];

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">生成结果</h1>
        <div className="page-actions">
          <button className="btn btn-outline" onClick={() => navigate('/cv-generate')}>
            重新生成
          </button>
          <button
            className="btn btn-primary"
            onClick={handleExport}
            disabled={exporting}
          >
            {exporting ? '导出中...' : '导出 HTML'}
          </button>
        </div>
      </div>

      {/* Scoring Section */}
      <div className="card" style={{ marginBottom: 'var(--space-lg)' }}>
        <div className="card-header">简历评分</div>
        <div className="card-body">
          {/* JD selector + score button */}
          <div style={{ display: 'flex', gap: 'var(--space-md)', alignItems: 'center', marginBottom: 'var(--space-lg)' }}>
            <select
              className="form-select"
              value={selectedJdId}
              onChange={e => setSelectedJdId(e.target.value ? Number(e.target.value) : '')}
              style={{ flex: 1 }}
            >
              <option value="">-- 选择岗位描述进行评分 --</option>
              {jds.map(jd => (
                <option key={jd.id} value={jd.id}>{jd.title}{jd.company ? ` - ${jd.company}` : ''}</option>
              ))}
            </select>
            <button
              className="btn btn-primary"
              disabled={selectedJdId === '' || scoring}
              onClick={handleScore}
            >
              {scoring ? '评分中...' : '开始评分'}
            </button>
          </div>

          {/* Scoring Results List */}
          {scoringResults.length === 0 ? (
            <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)', textAlign: 'center', padding: 'var(--space-md)' }}>
              暂无评分记录，请选择 JD 后点击「开始评分」
            </p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-sm)' }}>
              {scoringResults.map(sr => (
                <div key={sr.id} className="scoring-result-item" style={{
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-md)',
                  overflow: 'hidden',
                }}>
                  <div
                    style={{
                      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                      padding: '12px 16px', cursor: 'pointer',
                      background: 'var(--color-surface-alt)',
                    }}
                    onClick={() => handleToggleExpand(sr)}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-sm)' }}>
                      <span style={{ fontWeight: 600 }}>{sr.jdTitle || `JD #${sr.jdId}`}</span>
                      {sr.status === 'SCORING' && <span className="badge badge-warning">评分中...</span>}
                      {sr.status === 'COMPLETED' && sr.finalScore != null && (
                        <span className={`badge ${sr.finalScore >= DEFAULT_PASS_SCORE ? 'badge-success' : 'badge-warning'}`}>
                          {Math.round(sr.finalScore * 100)}%
                        </span>
                      )}
                      {sr.status === 'FAILED' && <span className="badge badge-danger">失败</span>}
                      {sr.status === 'COMPLETED' && (
                        <button
                          className="btn btn-sm btn-outline"
                          onClick={(e) => { e.stopPropagation(); handleOptimize(sr.id); }}
                          disabled={optimizing}
                          style={{ marginLeft: 'auto' }}
                        >
                          {optimizing ? '优化中...' : '优化简历'}
                        </button>
                      )}
                    </div>
                    <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
                      {new Date(sr.createdAt).toLocaleString()}
                    </span>
                  </div>

                  {expandedSrId === sr.id && sr.status === 'COMPLETED' && sr.finalScore != null && (
                    <div style={{ padding: 'var(--space-md)' }}>
                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-md)', marginBottom: 'var(--space-md)' }}>
                        <div className="card">
                          <div className="card-header">综合评分</div>
                          <div className="card-body">
                            <ScoreOverview
                              finalScore={sr.finalScore}
                              threshold={DEFAULT_PASS_SCORE}
                              status={sr.status}
                              iterationCount={sr.iterationCount}
                            />
                          </div>
                        </div>
                        <div className="card">
                          <div className="card-header">角色评分明细</div>
                          <div className="card-body">
                            <RoleScoresBreakdown roleScoresJson={sr.roleScores ?? ''} />
                          </div>
                        </div>
                      </div>
                      <div className="card" style={{ marginBottom: 'var(--space-md)' }}>
                        <div className="card-header">
                          {sr.finalScore >= DEFAULT_PASS_SCORE ? '评审反馈' : '改进建议'}
                        </div>
                        <div className="card-body">
                          <FeedbackPanel
                            feedback={sr.finalFeedback ?? ''}
                            threshold={DEFAULT_PASS_SCORE}
                            finalScore={sr.finalScore}
                          />
                        </div>
                      </div>
                      <IterationHistory history={expandedHistory} loading={false} />
                    </div>
                  )}

                  {expandedSrId === sr.id && sr.status === 'SCORING' && (
                    <div style={{ padding: 'var(--space-lg)', textAlign: 'center' }}>
                      <LoadingSpinner size="sm" text="AI 正在评审中..." />
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Optimized Preview */}
      {optimizedContent && (
        <div className="card" style={{ marginBottom: 'var(--space-lg)', borderColor: 'var(--color-primary)' }}>
          <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>优化预览</span>
            <div style={{ display: 'flex', gap: 'var(--space-sm)' }}>
              <button className="btn btn-sm btn-primary" onClick={handleSaveOptimized}>保存</button>
              <button className="btn btn-sm btn-outline" onClick={handleDiscardOptimized}>放弃</button>
            </div>
          </div>
          <div className="card-body">
            <CvPreview htmlContent={optimizedContent} />
          </div>
        </div>
      )}

      {/* Tabs: Preview / Edit */}
      <div className="card">
        <div className="tabs">
          <button className={`tab ${activeTab === 'preview' ? 'active' : ''}`} onClick={() => setActiveTab('preview')}>
            预览
          </button>
          <button className={`tab ${activeTab === 'edit' ? 'active' : ''}`} onClick={() => setActiveTab('edit')}>
            编辑
          </button>
        </div>
        <div>
          {activeTab === 'preview' && <CvPreview htmlContent={result.finalContent} />}
          {activeTab === 'edit' && (
            <CvEditor content={result.finalContent} onSave={handleSaveContent} saving={updating} />
          )}
        </div>
      </div>

      <style>{`
        .tabs {
          display: flex; border-bottom: 1px solid var(--color-border);
          background: var(--color-surface-alt);
          border-radius: var(--radius-lg) var(--radius-lg) 0 0;
        }
        .tab {
          padding: 12px 24px; border: none; background: transparent;
          font-size: var(--font-size-sm); font-weight: 500;
          color: var(--color-text-muted); cursor: pointer;
          transition: all var(--transition-fast);
          border-bottom: 2px solid transparent; margin-bottom: -1px;
        }
        .tab:hover { color: var(--color-text); }
        .tab.active { color: var(--color-primary); border-bottom-color: var(--color-primary); }
        .form-select {
          padding: 8px 12px; border: 1px solid var(--color-border);
          border-radius: var(--radius-md); font-size: var(--font-size-sm);
          background: var(--color-surface); color: var(--color-text);
        }
      `}</style>
    </div>
  );
}
