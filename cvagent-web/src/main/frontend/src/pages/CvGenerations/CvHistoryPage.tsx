import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import DataTable from '../../components/common/DataTable';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorState from '../../components/common/ErrorState';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { useCvGenerations } from '../../hooks/useCvGenerations';
import { useToast } from '../../context/ToastContext';
import { formatDateTime } from '../../utils/format';
import type { GeneratedCv, PageResult } from '../../api/types';

const SCORE_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  SCORING: '评分中',
  FINAL: '已定稿',
  EXPORTED: '已导出',
};

const SCORE_STYLES: Record<string, string> = {
  DRAFT: 'info',
  SCORING: 'warning',
  FINAL: 'success',
  EXPORTED: 'neutral',
};

export default function CvHistoryPage() {
  const navigate = useNavigate();
  const { addToast } = useToast();
  const { listCvs, exportCv } = useCvGenerations();

  const [data, setData] = useState<PageResult<GeneratedCv> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [deleteTarget, setDeleteTarget] = useState<GeneratedCv | null>(null);
  const pageSize = 10;

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await listCvs(page, pageSize);
      setData(result);
    } catch (e: any) {
      setError(e.message || '加载失败');
    } finally {
      setLoading(false);
    }
  }, [listCvs, page]);

  useEffect(() => { load(); }, [load]);

  const totalPages = data ? Math.max(1, Math.ceil(data.total / pageSize)) : 1;

  const handleExport = async (e: React.MouseEvent, cv: GeneratedCv) => {
    e.stopPropagation();
    try {
      await exportCv(cv.id);
      addToast('success', '简历已导出');
      load();
    } catch (err: any) {
      addToast('error', err.message || '导出失败');
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      const { deleteGeneratedCv } = await import('../../api/cvGenerations');
      await deleteGeneratedCv(deleteTarget.id);
      addToast('success', '简历已删除');
      setDeleteTarget(null);
      load();
    } catch (e: any) {
      addToast('error', e.message || '删除失败');
    }
  };

  const columns = [
    {
      key: 'workExpName',
      header: '姓名',
      render: (cv: GeneratedCv) => cv.workExpName || '-',
    },
    {
      key: 'templateName',
      header: '模板',
      render: (cv: GeneratedCv) => cv.templateName || '-',
    },
    {
      key: 'jdTitle',
      header: 'JD',
      render: (cv: GeneratedCv) => cv.jdTitle || '-',
    },
    {
      key: 'finalScore',
      header: '评分',
      render: (cv: GeneratedCv) => {
        if (cv.status === 'SCORING') return <span className="badge badge-warning">评分中...</span>;
        if (cv.finalScore == null) return <span className="badge badge-info">未评分</span>;
        const pct = Math.round(cv.finalScore * 100);
        return <span style={{ fontWeight: 600, color: cv.finalScore >= 0.8 ? 'var(--color-success)' : 'var(--color-warning)' }}>{pct}%</span>;
      },
    },
    {
      key: 'iterationCount',
      header: '迭代',
      render: (cv: GeneratedCv) => cv.iterationCount != null ? `${cv.iterationCount}轮` : '-',
    },
    {
      key: 'status',
      header: '状态',
      render: (cv: GeneratedCv) => {
        const label = SCORE_LABELS[cv.status] || cv.status;
        const cls = SCORE_STYLES[cv.status] || 'neutral';
        return <span className={`badge badge-${cls}`}>{label}</span>;
      },
    },
    {
      key: 'createdAt',
      header: '时间',
      render: (cv: GeneratedCv) => formatDateTime(cv.createdAt),
    },
    {
      key: 'actions',
      header: '操作',
      render: (cv: GeneratedCv) => (
        <div style={{ display: 'flex', gap: 'var(--space-xs)' }}>
          <button className="btn btn-sm btn-outline" onClick={(e) => { e.stopPropagation(); navigate(`/cv-result/${cv.id}`); }}>
            查看
          </button>
          <button className="btn btn-sm btn-primary" onClick={(e) => handleExport(e, cv)}>
            导出
          </button>
          <button
            className="btn btn-sm btn-ghost"
            style={{ color: 'var(--color-danger)' }}
            onClick={(e) => { e.stopPropagation(); setDeleteTarget(cv); }}
          >
            删除
          </button>
        </div>
      ),
    },
  ];

  if (error) {
    return (
      <div className="page">
        <div className="page-header"><h1 className="page-title">生成记录</h1></div>
        <ErrorState message={error} onRetry={load} />
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">生成记录</h1>
      </div>

      <div className="card">
        <DataTable<GeneratedCv>
          columns={columns}
          data={data}
          loading={loading}
          emptyText="暂无生成记录，请先生成简历"
        />

        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'center', padding: 'var(--space-md)', gap: 'var(--space-xs)' }}>
            <button className="btn btn-sm btn-ghost" disabled={page <= 1} onClick={() => setPage(p => p - 1)}>上一页</button>
            <span style={{ padding: '4px 12px', fontSize: 'var(--font-size-sm)', lineHeight: '28px' }}>
              {page} / {totalPages}
            </span>
            <button className="btn btn-sm btn-ghost" disabled={page >= totalPages} onClick={() => setPage(p => p + 1)}>下一页</button>
          </div>
        )}
      </div>

      <ConfirmDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="确认删除"
        message={`确定要删除「${deleteTarget?.workExpName || deleteTarget?.id}」的生成记录吗？`}
        confirmLabel="删除"
        danger
      />
    </div>
  );
}
