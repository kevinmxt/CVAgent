import React, { useState } from 'react';
import DataTable from '../components/common/DataTable';
import FileUpload from '../components/common/FileUpload';
import Modal from '../components/common/Modal';
import ConfirmDialog from '../components/common/ConfirmDialog';
import LoadingSpinner from '../components/common/LoadingSpinner';
import ErrorState from '../components/common/ErrorState';
import CvPreview from '../components/cv/CvPreview';
import { useCvTemplates } from '../hooks/useCvTemplates';
import { useToast } from '../context/ToastContext';
import { formatDateTime } from '../utils/format';
import type { CvTemplate } from '../api/types';

const EMPTY_FORM = { name: '', description: '', templateContent: '', fileName: '' };

export default function CvTemplatesPage() {
  const { templates, loading, error, refetch, create, update, remove, importFile, duplicate } = useCvTemplates();
  const { addToast } = useToast();
  const [showCreate, setShowCreate] = useState(false);
  const [editItem, setEditItem] = useState<CvTemplate | null>(null);
  const [deleteItem, setDeleteItem] = useState<CvTemplate | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [importing, setImporting] = useState(false);
  const [previewTab, setPreviewTab] = useState<'edit' | 'preview'>('edit');

  const presets = templates.filter((t) => t.isPreset);
  const customs = templates.filter((t) => !t.isPreset);

  const openCreate = () => {
    setForm(EMPTY_FORM);
    setPreviewTab('edit');
    setShowCreate(true);
  };

  const openEdit = (item: CvTemplate) => {
    setEditItem(item);
    setPreviewTab('edit');
    setForm({
      name: item.name,
      description: item.description,
      templateContent: item.templateContent,
      fileName: item.fileName,
    });
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      if (editItem) {
        await update(editItem.id, form);
        addToast('success', '模板已更新');
        setEditItem(null);
      } else {
        await create(form);
        addToast('success', '模板已创建');
        setShowCreate(false);
      }
    } catch (e: any) {
      addToast('error', e.message || '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleImport = async (file: File) => {
    setImporting(true);
    try {
      await importFile(file);
      addToast('success', '模板导入成功，AI 已自动生成 HTML 模板');
    } catch (e: any) {
      addToast('error', e.message || '模板导入失败');
    } finally {
      setImporting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteItem) return;
    try {
      await remove(deleteItem.id);
      addToast('success', '模板已删除');
      setDeleteItem(null);
    } catch (e: any) {
      addToast('error', e.message || '删除失败');
      setDeleteItem(null);
    }
  };

  const handleDuplicate = async (item: CvTemplate) => {
    try {
      await duplicate(item.id);
      addToast('success', '已创建副本');
    } catch (e: any) {
      addToast('error', e.message || '复制失败');
    }
  };

  const templateColumns = (isPreset: boolean) => [
    { key: 'name', header: '名称', render: (t: CvTemplate) => t.name },
    { key: 'description', header: '描述', render: (t: CvTemplate) => t.description || '-' },
    { key: 'fileName', header: '文件名', render: (t: CvTemplate) => t.fileName || '-' },
    { key: 'updatedAt', header: '更新时间', render: (t: CvTemplate) => formatDateTime(t.updatedAt) },
    {
      key: 'actions', header: '操作',
      render: (t: CvTemplate) => (
        <div style={{ display: 'flex', gap: 8 }}>
          {isPreset ? (
            <>
              <button className="btn btn-outline btn-sm" onClick={() => openEdit(t)}>查看</button>
              <button className="btn btn-ghost btn-sm" onClick={() => handleDuplicate(t)}>复制</button>
            </>
          ) : (
            <>
              <button className="btn btn-outline btn-sm" onClick={() => openEdit(t)}>编辑</button>
              <button className="btn btn-ghost btn-sm" onClick={() => handleDuplicate(t)}>复制</button>
              <button className="btn btn-danger btn-sm" onClick={() => setDeleteItem(t)}>删除</button>
            </>
          )}
        </div>
      ),
    },
  ];

  const pageResult = { items: presets, page: 1, size: presets.length, total: presets.length, pages: 1 };
  const customResult = { items: customs, page: 1, size: customs.length, total: customs.length, pages: 1 };

  if (loading) return <div className="page"><LoadingSpinner size="lg" text="加载模板..." /></div>;
  if (error) return <div className="page"><ErrorState message={error} onRetry={refetch} /></div>;

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">简历模板</h1>
        <button className="btn btn-primary" onClick={openCreate}>创建模板</button>
      </div>

      {/* Import from file */}
      <div className="card" style={{ marginBottom: 'var(--space-lg)' }}>
        <div className="card-body">
          <h4 style={{ marginBottom: 'var(--space-sm)', fontWeight: 600 }}>从文件导入模板</h4>
          <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)', marginBottom: 'var(--space-md)' }}>
            支持 txt、docx、html、pdf 格式，AI 将自动分析简历结构并生成带占位符的 HTML 模板
          </p>
          <FileUpload onUpload={handleImport} uploading={importing} />
        </div>
      </div>

      {/* Preset Templates */}
      <div style={{ marginBottom: 'var(--space-lg)' }}>
        <h3 style={{ fontSize: 'var(--font-size-base)', fontWeight: 600, marginBottom: 'var(--space-md)', color: 'var(--color-text-secondary)' }}>
          预置模板
        </h3>
        <div className="card">
          <div className="card-body">
            <DataTable columns={templateColumns(true)} data={pageResult} loading={false} emptyText="暂无预置模板" />
          </div>
        </div>
      </div>

      {/* Custom Templates */}
      <div>
        <h3 style={{ fontSize: 'var(--font-size-base)', fontWeight: 600, marginBottom: 'var(--space-md)', color: 'var(--color-text-secondary)' }}>
          自定义模板
        </h3>
        <div className="card">
          <div className="card-body">
            <DataTable columns={templateColumns(false)} data={customResult} loading={false} emptyText="暂无自定义模板，点击上方按钮创建" />
          </div>
        </div>
      </div>

      {/* Create/Edit Modal */}
      <Modal
        open={showCreate || !!editItem}
        onClose={() => { setShowCreate(false); setEditItem(null); setPreviewTab('edit'); }}
        title={editItem ? (editItem.isPreset ? '查看模板' : '编辑模板') : '创建模板'}
        width="720px"
        footer={editItem?.isPreset ? (
          <button className="btn btn-outline" onClick={() => setEditItem(null)}>关闭</button>
        ) : (
          <>
            <button className="btn btn-outline" onClick={() => { setShowCreate(false); setEditItem(null); }}>取消</button>
            <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
              {saving ? '保存中...' : '保存'}
            </button>
          </>
        )}
      >
        <div>
          {/* Tab switcher */}
          <div style={{ display: 'flex', gap: 0, marginBottom: 16, borderBottom: '2px solid var(--color-border)' }}>
            <button
              type="button"
              style={{
                padding: '8px 16px', border: 'none', cursor: 'pointer',
                background: previewTab === 'edit' ? 'var(--color-primary)' : 'transparent',
                color: previewTab === 'edit' ? '#fff' : 'var(--color-text)',
                borderRadius: '4px 4px 0 0', fontWeight: 600,
              }}
              onClick={() => setPreviewTab('edit')}
            >
              编辑
            </button>
            <button
              type="button"
              style={{
                padding: '8px 16px', border: 'none', cursor: 'pointer',
                background: previewTab === 'preview' ? 'var(--color-primary)' : 'transparent',
                color: previewTab === 'preview' ? '#fff' : 'var(--color-text)',
                borderRadius: '4px 4px 0 0', fontWeight: 600,
              }}
              onClick={() => setPreviewTab('preview')}
            >
              预览
            </button>
          </div>

          {previewTab === 'edit' ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                <div className="form-group">
                  <label className="form-label">模板名称</label>
                  <input className="form-input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} disabled={editItem?.isPreset} />
                </div>
                <div className="form-group">
                  <label className="form-label">文件名</label>
                  <input className="form-input" value={form.fileName} onChange={(e) => setForm({ ...form, fileName: e.target.value })} disabled={editItem?.isPreset} />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">描述</label>
                <input className="form-input" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} disabled={editItem?.isPreset} />
              </div>
              <div className="form-group">
                <label className="form-label">
                  HTML 内容
                  <span style={{ fontWeight: 400, color: 'var(--color-text-muted)', marginLeft: 8 }}>
                    支持占位符: {'{{person_name}} {{person_email}} {{summary}} {{skills}} {{professional_exp}} {{education}}'}
                  </span>
                </label>
                <textarea
                  className="form-textarea"
                  rows={16}
                  value={form.templateContent}
                  onChange={(e) => setForm({ ...form, templateContent: e.target.value })}
                  disabled={editItem?.isPreset}
                  style={{ fontFamily: 'var(--font-mono)', fontSize: 'var(--font-size-xs)' }}
                />
              </div>
            </div>
          ) : (
            <div>
              <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)', marginBottom: 'var(--space-md)' }}>
                预览模式：占位符将保留显示，实际生成简历时会替换为真实内容。
              </p>
              <CvPreview htmlContent={form.templateContent || ''} />
            </div>
          )}
        </div>
      </Modal>

      {/* Delete Confirmation */}
      <ConfirmDialog
        open={!!deleteItem}
        onClose={() => setDeleteItem(null)}
        onConfirm={handleDelete}
        title="删除模板"
        message={`确定要删除模板「${deleteItem?.name || '未命名'}」吗？此操作不可撤销。`}
      />
    </div>
  );
}
