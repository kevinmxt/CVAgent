import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';

export default function AppLayout() {
  return (
    <div className="app-layout">
      <Sidebar />
      <div className="app-main">
        <Header />
        <main className="app-content">
          <Outlet />
        </main>
      </div>
      <style>{`
        .app-layout {
          display: flex;
          min-height: 100vh;
        }
        .app-main {
          margin-left: var(--sidebar-width);
          flex: 1;
          display: flex;
          flex-direction: column;
          min-width: 0;
        }
        .app-content {
          flex: 1;
        }
      `}</style>
    </div>
  );
}
