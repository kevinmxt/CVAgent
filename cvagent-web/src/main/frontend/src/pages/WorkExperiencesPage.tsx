import React, { useState } from 'react';
import DataTable from '../components/common/DataTable';
import Pagination from '../components/common/Pagination';
import FileUpload from '../components/common/FileUpload';
import Modal from '../components/common/Modal';
import ConfirmDialog from '../components/common/ConfirmDialog';
import ErrorState from '../components/common/ErrorState';
import { useWorkExperiences } from '../hooks/useWorkExperiences';
import { usePagination } from '../hooks/usePagination';
import { useToast } from '../context/ToastContext';
import { formatDateTime } from '../utils/format';
import type { WorkExperience } from '../api/types';

export default function WorkExperiencesPage() {
  const { page, setPage } = usePagination();
  const { data, loading, error, refetch, importFile, update, remove } = useWorkExperiences(page, 10);
  const { addToast } = useToast();
  const [uploading, setUploading] = useState(false);
  const [editItem, setEditItem] = useState<WorkExperience | null>(null);
  const [deleteItem, setDeleteItem] = useState<WorkExperience | null>(null);
  const [editForm, setEditForm] = useState<Partial<WorkExperience>>({});
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

  const openEdit = (item: WorkExperience) => {
    setEditItem(item);
    setEditForm({
      personName: item.personName,
      personEmail: item.personEmail,
      personPhone: item.personPhone,
      summary: item.summary,
      skills: item.skills,
      professionalExp: item.professionalExp,
      education: item.education,
    });
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
    { key: 'personName', header: '姓名', render: (r: WorkExperience) => r.personName || '-' },
    { key: 'personEmail', header: '邮箱', render: (r: WorkExperience) => r.personEmail || '-' },
    { key: 'rawFileName', header: '来源文件', render: (r: WorkExperience) => r.rawFileName || '-' },
    { key: 'createdAt', header: '导入时间', render: (r: WorkExperience) => formatDateTime(r.createdAt) },
    {
      key: 'actions', header: '操作',
      render: (r: WorkExperience) => (
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-outline btn-sm" onClick={() => openEdit(r)}>编辑</button>
          <button className="btn btn-danger btn-sm" onClick={() => setDeleteItem(r)}>删除</button>
        </div>
      ),
    },
  ];

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">工作经历</h1>
        <div />
      </div>

      <div className="card" style={{ marginBottom: 'var(--space-lg)' }}>
        <div className="card-body">
          <FileUpload onUpload={handleImport} uploading={uploading} />
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          {error ? (
            <ErrorState message={error} onRetry={refetch} />
          ) : (
            <>
              <DataTable columns={columns} data={data} loading={loading} emptyText="暂无工作经历，请导入简历文件" />
              {data && <Pagination page={data.page} pages={data.pages} total={data.total} onPageChange={setPage} />}
            </>
          )}
        </div>
      </div>

      {/* Edit Modal */}
      <Modal
        open={!!editItem}
        onClose={() => setEditItem(null)}
        title="编辑工作经历"
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
              <label className="form-label">姓名</label>
              <input className="form-input" value={editForm.personName || ''} onChange={(e) => setEditForm({ ...editForm, personName: e.target.value })} />
            </div>
            <div className="form-group">
              <label className="form-label">邮箱</label>
              <input className="form-input" value={editForm.personEmail || ''} onChange={(e) => setEditForm({ ...editForm, personEmail: e.target.value })} />
            </div>
            <div className="form-group">
              <label className="form-label">电话</label>
              <input className="form-input" value={editForm.personPhone || ''} onChange={(e) => setEditForm({ ...editForm, personPhone: e.target.value })} />
            </div>
            <div className="form-group">
              <label className="form-label">技能</label>
              <input className="form-input" value={editForm.skills || ''} onChange={(e) => setEditForm({ ...editForm, skills: e.target.value })} />
            </div>
          </div>
          <div className="form-group">
            <label className="form-label">个人简介</label>
            <textarea className="form-textarea" rows={3} value={editForm.summary || ''} onChange={(e) => setEditForm({ ...editForm, summary: e.target.value })} />
          </div>
          <div className="form-group">
            <label className="form-label">工作经历详情</label>
            <textarea className="form-textarea" rows={5} value={editForm.professionalExp || ''} onChange={(e) => setEditForm({ ...editForm, professionalExp: e.target.value })} />
          </div>
          <div className="form-group">
            <label className="form-label">教育背景</label>
            <textarea className="form-textarea" rows={3} value={editForm.education || ''} onChange={(e) => setEditForm({ ...editForm, education: e.target.value })} />
          </div>
        </div>
      </Modal>

      {/* Delete Confirmation */}
      <ConfirmDialog
        open={!!deleteItem}
        onClose={() => setDeleteItem(null)}
        onConfirm={handleDelete}
        title="删除工作经历"
        message={`确定要删除「${deleteItem?.personName || '未命名'}」的工作经历吗？此操作不可撤销。`}
      />
    </div>
  );
}
