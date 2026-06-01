import React, { Suspense, lazy } from 'react';
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ToastProvider } from './context/ToastContext';
import AppLayout from './components/layout/AppLayout';
import LoadingSpinner from './components/common/LoadingSpinner';

const WorkExperiencesPage = lazy(() => import('./pages/WorkExperiencesPage'));
const CvTemplatesPage = lazy(() => import('./pages/CvTemplatesPage'));
const JobDescriptionsPage = lazy(() => import('./pages/JobDescriptionsPage'));
const CvGeneratePage = lazy(() => import('./pages/CvGenerations/CvGeneratePage'));
const CvResultPage = lazy(() => import('./pages/CvGenerations/CvResultPage'));

function PageFallback() {
  return <LoadingSpinner size="lg" text="加载中..." />;
}

export default function App() {
  return (
    <ToastProvider>
      <HashRouter>
        <Suspense fallback={<PageFallback />}>
          <Routes>
            <Route element={<AppLayout />}>
              <Route path="/" element={<Navigate to="/cv-generate" replace />} />
              <Route path="/cv-generate" element={<CvGeneratePage />} />
              <Route path="/cv-result/:id" element={<CvResultPage />} />
              <Route path="/work-experiences" element={<WorkExperiencesPage />} />
              <Route path="/templates" element={<CvTemplatesPage />} />
              <Route path="/job-descriptions" element={<JobDescriptionsPage />} />
              <Route path="*" element={
                <div style={{ textAlign: 'center', padding: 'var(--space-2xl)', color: 'var(--color-text-muted)' }}>
                  <div style={{ fontSize: '4rem', marginBottom: 'var(--space-lg)', opacity: 0.3 }}>404</div>
                  <p>页面未找到</p>
                </div>
              } />
            </Route>
          </Routes>
        </Suspense>
      </HashRouter>
    </ToastProvider>
  );
}
