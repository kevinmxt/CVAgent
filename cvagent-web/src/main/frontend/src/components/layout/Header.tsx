import React from 'react';
import { useLocation } from 'react-router-dom';

const PAGE_TITLES: Record<string, string> = {
  '/work-experiences': '工作经历管理',
  '/templates': '简历模板管理',
  '/job-descriptions': '岗位描述管理',
  '/cv-generate': '生成简历',
  '/cv-result': '生成结果',
};

export default function Header() {
  const location = useLocation();
  const basePath = '/' + location.pathname.split('/')[1];
  const isResultPage = location.pathname.startsWith('/cv-result');
  const title = isResultPage ? PAGE_TITLES['/cv-result'] : (PAGE_TITLES[basePath] || 'CVAgent');

  return (
    <header className="app-header">
      <h1 className="header-title">{title}</h1>
      <style>{`
        .app-header {
          height: 60px;
          display: flex;
          align-items: center;
          padding: 0 var(--space-2xl);
          background: var(--color-surface);
          border-bottom: 1px solid var(--color-border);
          position: sticky;
          top: 0;
          z-index: 50;
        }
        .header-title {
          font-size: var(--font-size-lg);
          font-weight: 600;
          color: var(--color-text);
        }
      `}</style>
    </header>
  );
}
