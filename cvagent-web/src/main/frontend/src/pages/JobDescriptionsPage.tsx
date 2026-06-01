import React, { useState } from 'react';
import DataTable from '../components/common/DataTable';
import Pagination from '../components/common/Pagination';
import FileUpload from '../components/common/FileUpload';
import Modal from '../components/common/Modal';
import ConfirmDialog from '../components/common/ConfirmDialog';
import ErrorState from '../components/common/ErrorState';
import { useJobDescriptions } from '../hooks/useJobDescriptions';
import { usePagination } from '../hooks/usePagination';
import { useToast } from '../context/ToastContext';
import { formatDateTime } from '../utils/format';
import type { JobDescription } from '../api/types';

export default function JobDescriptionsPage() {
  const { page, setPage } = usePagination();
  const { data, loading, error, refetch, importFile, update, remove } = useJobDescriptions(page, 10);
  const { addToast } = useToast();
  const [uploading, setUploading] = useState(false);
  const [editItem, setEditItem] = useState<JobDescription | null>(null);
  const [deleteItem, setDeleteItem] = useState<JobDescription | null>(null);
  const [editForm, setEditForm] = useState<Partial<JobDescription>>({});
  const [saving, setSaving] = useState(false);

  const handleImport = async (file: File) => {
    setUploading(true);
    try {
      await importFile(file);
      addToast('success', '导入成功');
    } catch (e: any) {
      addToast('error', e.message || '导入失败');
    } finally {
      setUploading(false);
    }
  };

  const openEdit = (item: JobDescription) => {
    setEditItem(item);
    setEditForm({ title: item.title, company: item.company, content: item.content });
  };

  const handleSave = async () => {
    if (!editItem) return;
    setSaving(true);
    try {
      await update(editItem.id, editForm);
      addToast('success', '更新成功');
      setEditItem(null);
    } catch (e: any) {
      addToast('error', e.message || '更新失败');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteItem) return;
    try {
      await remove(deleteItem.id);
      addToast('success', '删除成功');
      setDeleteItem(null);
    } catch (e: any) {
      addToast('error', e.message || '删除失败');
    }
  };

  const columns = [
    { key: 'title', header: '职位', render: (j: JobDescription) => j.title || '-' },
    { key: 'company', header: '公司', render: (j: JobDescription) => j.company || '-' },
    { key: 'rawFileName', header: '来源文件', render: (j: JobDescription) => j.rawFileName || '-' },
    { key: 'createdAt', header: '导入时间', render: (j: JobDescription) => formatDateTime(j.createdAt) },
    {
      key: 'actions', header: '操作',
      render: (j: JobDescription) => (
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-outline btn-sm" onClick={() => openEdit(j)}>编辑</button>
          <button className="btn btn-danger btn-sm" onClick={() => setDeleteItem(j)}>删除</button>
        </div>
      ),
    },
  ];

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">岗位描述</h1>
      </div>

      <div className="card" style={{ marginBottom: 'var(--space-lg)' }}>
        <div className="card-body">
          <FileUpload onUpload={handleImport} uploading={uploading} label="导入岗位描述" />
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          {error ? (
            <ErrorState message={error} onRetry={refetch} />
          ) : (
            <>
              <DataTable columns={columns} data={data} loading={loading} emptyText="暂无岗位描述，请导入 JD 文件" />
              {data && <Pagination page={data.page} pages={data.pages} total={data.total} onPageChange={setPage} />}
            </>
          )}
        </div>
      </div>

      <Modal
        open={!!editItem}
        onClose={() => setEditItem(null)}
        title="编辑岗位描述"
        width="640px"
        footer={
          <>
            <button className="btn btn-outline" onClick={() => setEditItem(null)}>取消</button>
            <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
              {saving ? '保存中...' : '保存'}
            </button>
          </>
        }
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <div className="form-group">
              <label className="form-label">职位名称</label>
              <input className="form-input" value={editForm.title || ''} onChange={(e) => setEditForm({ ...editForm, title: e.target.value })} />
            </div>
            <div className="form-group">
              <label className="form-label">公司</label>
              <input className="form-input" value={editForm.company || ''} onChange={(e) => setEditForm({ ...editForm, company: e.target.value })} />
            </div>
          </div>
          <div className="form-group">
            <label className="form-label">JD 内容</label>
            <textarea className="form-textarea" rows={12} value={editForm.content || ''} onChange={(e) => setEditForm({ ...editForm, content: e.target.value })} />
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        open={!!deleteItem}
        onClose={() => setDeleteItem(null)}
        onConfirm={handleDelete}
        title="删除岗位描述"
        message={`确定要删除「${deleteItem?.title || '未命名'}」的岗位描述吗？此操作不可撤销。`}
      />
    </div>
  );
}
