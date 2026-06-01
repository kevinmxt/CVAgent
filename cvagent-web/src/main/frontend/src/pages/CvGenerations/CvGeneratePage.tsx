import React, { useState, useCallback, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import CardSelector from '../../components/cv/GenerationSelector';
import GenerationProgress from '../../components/cv/GenerationProgress';
import { useWorkExperiences } from '../../hooks/useWorkExperiences';
import { useCvTemplates } from '../../hooks/useCvTemplates';
import { useJobDescriptions } from '../../hooks/useJobDescriptions';
import { useCvGenerations } from '../../hooks/useCvGenerations';
import { useToast } from '../../context/ToastContext';
import { formatDate } from '../../utils/format';
import type { WorkExperience, CvTemplate, JobDescription } from '../../api/types';

export default function CvGeneratePage() {
  const navigate = useNavigate();
  const { addToast } = useToast();

  // Fetch lists for selectors
  const { data: weData, loading: weLoading } = useWorkExperiences(1, 50);
  const { templates, loading: tplLoading } = useCvTemplates();
  const { data: jdData, loading: jdLoading } = useJobDescriptions(1, 50);

  // Selection state
  const [selectedWE, setSelectedWE] = useState<number | null>(null);
  const [selectedTPL, setSelectedTPL] = useState<number | null>(null);
  const [selectedJD, setSelectedJD] = useState<number | null>(null);

  // Generation state
  const { state, generate, cancel } = useCvGenerations();

  const canGenerate = selectedWE && selectedTPL && selectedJD;
  const isGenerating = state.status === 'generating';

  const handleGenerate = useCallback(async () => {
    if (!canGenerate) return;
    const result = await generate(selectedWE, selectedTPL, selectedJD);
    if (result) {
      addToast('success', '简历生成完成');
      navigate(`/cv-result/${result.id}`);
    }
  }, [canGenerate, selectedWE, selectedTPL, selectedJD, generate, navigate, addToast]);

  if (isGenerating) {
    return (
      <div className="page">
        <div className="card">
          <GenerationProgress onCancel={cancel} />
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">生成简历</h1>
      </div>

      {state.status === 'error' && (
        <div className="card" style={{ marginBottom: 'var(--space-lg)', borderColor: 'var(--color-danger)' }}>
          <div className="card-body" style={{ color: 'var(--color-danger)', fontSize: 'var(--font-size-sm)' }}>
            {state.error}
          </div>
        </div>
      )}

      <div style={{ display: 'flex', gap: 'var(--space-lg)', marginBottom: 'var(--space-lg)' }}>
        <CardSelector<WorkExperience>
          title={`工作经历 (${weData?.items.length || 0})`}
          items={weData?.items || []}
          selectedId={selectedWE}
          onSelect={setSelectedWE}
          loading={weLoading}
          emptyText="暂无工作经历，请先导入"
          renderItem={(we) => ({
            id: we.id,
            title: we.personName || '未命名',
            subtitle: `导入于 ${formatDate(we.createdAt)}`,
          })}
        />
        <CardSelector<CvTemplate>
          title={`简历模板 (${templates.length})`}
          items={templates}
          selectedId={selectedTPL}
          onSelect={setSelectedTPL}
          loading={tplLoading}
          emptyText="暂无模板"
          renderItem={(tpl) => ({
            id: tpl.id,
            title: tpl.name,
            subtitle: tpl.description || tpl.fileName || '',
            badge: tpl.isPreset ? '预置' : undefined,
          })}
        />
        <CardSelector<JobDescription>
          title={`岗位描述 (${jdData?.items.length || 0})`}
          items={jdData?.items || []}
          selectedId={selectedJD}
          onSelect={setSelectedJD}
          loading={jdLoading}
          emptyText="暂无岗位描述，请先导入"
          renderItem={(jd) => ({
            id: jd.id,
            title: jd.title || '未命名',
            subtitle: jd.company || `导入于 ${formatDate(jd.createdAt)}`,
          })}
        />
      </div>

      <div style={{ textAlign: 'center', padding: 'var(--space-lg)' }}>
        <button
          className="btn btn-primary btn-lg"
          disabled={!canGenerate}
          onClick={handleGenerate}
          style={{ minWidth: 200 }}
        >
          生成简历
        </button>
        {!canGenerate && (
          <p style={{ marginTop: 'var(--space-sm)', fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
            请选择工作经历、简历模板和岗位描述
          </p>
        )}
      </div>
    </div>
  );
}
