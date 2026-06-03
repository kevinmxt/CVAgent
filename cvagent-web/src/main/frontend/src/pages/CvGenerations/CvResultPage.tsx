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
import { useToast } from '../../context/ToastContext';
import { DEFAULT_PASS_SCORE } from '../../utils/constants';

export default function CvResultPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { addToast } = useToast();
  const { state, result, history, loadingHistory, updating, exporting, scoring, loadResult, loadHistory, updateContent, exportCv, scoreCv } = useCvGenerations();
  const [activeTab, setActiveTab] = useState<'preview' | 'edit' | 'history'>('preview');

  useEffect(() => {
    if (id) {
      loadResult(Number(id));
      loadHistory(Number(id));
    }
  }, [id, loadResult, loadHistory]);

  // Poll while scoring
  useEffect(() => {
    if (!result || result.status !== 'SCORING') return;
    const interval = setInterval(async () => {
      if (id) {
        const data = await loadResult(Number(id));
        if (data && data.status !== 'SCORING') {
          clearInterval(interval);
          addToast('success', '评分完成');
        }
      }
    }, 3000);
    return () => clearInterval(interval);
  }, [result?.status, id, loadResult, addToast]);

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
    if (!result) return;
    try {
      await scoreCv(result.id);
      addToast('success', '评分完成');
      // Reload history
      if (id) loadHistory(Number(id));
    } catch (e: any) {
      addToast('error', e.message || '评分失败，请重试');
    }
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

  const hasScore = result.finalScore != null;
  const isScoring = result.status === 'SCORING';

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">生成结果</h1>
        <div className="page-actions">
          <button className="btn btn-outline" onClick={() => navigate('/cv-generate')}>
            重新生成
          </button>
          {!hasScore && !isScoring && (
            <button
              className="btn btn-primary"
              onClick={handleScore}
              disabled={scoring}
            >
              {scoring ? '提交中...' : '开始评分'}
            </button>
          )}
          <button
            className="btn btn-primary"
            onClick={handleExport}
            disabled={exporting}
          >
            {exporting ? '导出中...' : '导出 HTML'}
          </button>
        </div>
      </div>

      {!hasScore && !isScoring ? (
        <div className="card" style={{ marginBottom: 'var(--space-lg)' }}>
          <div className="card-body" style={{ textAlign: 'center', padding: 'var(--space-2xl)' }}>
            <div style={{ fontSize: '3rem', marginBottom: 'var(--space-md)', opacity: 0.3 }}>?</div>
            <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
              尚未评分，请点击上方「开始评分」按钮，系统将根据 JD 要求进行多角色评审
            </p>
          </div>
        </div>
      ) : isScoring ? (
        <div className="card" style={{ marginBottom: 'var(--space-lg)' }}>
          <div className="card-body" style={{ textAlign: 'center', padding: 'var(--space-2xl)' }}>
            <LoadingSpinner size="md" text="AI 正在评审中，请稍候..." />
            <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-xs)', marginTop: 'var(--space-sm)' }}>
              评分将在后台执行，页面会自动刷新显示结果
            </p>
          </div>
        </div>
      ) : (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-lg)', marginBottom: 'var(--space-lg)' }}>
            <div className="card">
              <div className="card-header">综合评分</div>
              <div className="card-body">
                <ScoreOverview
                  finalScore={result.finalScore!}
                  threshold={DEFAULT_PASS_SCORE}
                  status={result.status}
                  iterationCount={result.iterationCount}
                />
              </div>
            </div>

            <div className="card">
              <div className="card-header">角色评分明细</div>
              <div className="card-body">
                <RoleScoresBreakdown roleScoresJson={result.roleScores ?? ''} />
              </div>
            </div>
          </div>

          <div className="card" style={{ marginBottom: 'var(--space-lg)' }}>
            <div className="card-header">
              {result.finalScore! >= DEFAULT_PASS_SCORE ? '评审反馈' : '改进建议'}
            </div>
            <div className="card-body">
              <FeedbackPanel
                feedback={result.finalFeedback ?? ''}
                threshold={DEFAULT_PASS_SCORE}
                finalScore={result.finalScore!}
              />
            </div>
          </div>
        </>
      )}

      {/* Tabs: Preview / Edit / History */}
      <div className="card">
        <div className="tabs">
          <button
            className={`tab ${activeTab === 'preview' ? 'active' : ''}`}
            onClick={() => setActiveTab('preview')}
          >
            预览
          </button>
          <button
            className={`tab ${activeTab === 'edit' ? 'active' : ''}`}
            onClick={() => setActiveTab('edit')}
          >
            编辑
          </button>
          <button
            className={`tab ${activeTab === 'history' ? 'active' : ''}`}
            onClick={() => setActiveTab('history')}
          >
            迭代历史 ({result.iterationCount})
          </button>
        </div>

        <div>
          {activeTab === 'preview' && <CvPreview htmlContent={result.finalContent} />}
          {activeTab === 'edit' && (
            <CvEditor
              content={result.finalContent}
              onSave={handleSaveContent}
              saving={updating}
            />
          )}
          {activeTab === 'history' && (
            <IterationHistory
              history={history}
              loading={loadingHistory}
            />
          )}
        </div>
      </div>

      <style>{`
        .tabs {
          display: flex;
          border-bottom: 1px solid var(--color-border);
          background: var(--color-surface-alt);
          border-radius: var(--radius-lg) var(--radius-lg) 0 0;
        }
        .tab {
          padding: 12px 24px;
          border: none;
          background: transparent;
          font-size: var(--font-size-sm);
          font-weight: 500;
          color: var(--color-text-muted);
          cursor: pointer;
          transition: all var(--transition-fast);
          border-bottom: 2px solid transparent;
          margin-bottom: -1px;
        }
        .tab:hover { color: var(--color-text); }
        .tab.active {
          color: var(--color-primary);
          border-bottom-color: var(--color-primary);
        }
      `}</style>
    </div>
  );
}
