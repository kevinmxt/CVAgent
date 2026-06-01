import React from 'react';
import { NavLink } from 'react-router-dom';

interface NavItem {
  to: string;
  label: string;
  icon: string;
}

const navItems: NavItem[] = [
  { to: '/cv-generate', label: '生成简历', icon: '✦' },
  { to: '/work-experiences', label: '工作经历', icon: '☷' },
  { to: '/templates', label: '简历模板', icon: '⊞' },
  { to: '/job-descriptions', label: '岗位描述', icon: '⚲' },
];

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <span className="sidebar-logo">CV</span>
        <span className="sidebar-brand-text">CVAgent</span>
      </div>
      <nav className="sidebar-nav">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
          >
            <span className="sidebar-link-icon">{item.icon}</span>
            <span className="sidebar-link-label">{item.label}</span>
          </NavLink>
        ))}
      </nav>
      <div className="sidebar-footer">
        <span className="sidebar-version">v1.0.0</span>
      </div>
      <style>{`
        .sidebar {
          position: fixed;
          top: 0;
          left: 0;
          bottom: 0;
          width: var(--sidebar-width);
          background: var(--sidebar-bg);
          display: flex;
          flex-direction: column;
          z-index: 100;
        }
        .sidebar-brand {
          display: flex;
          align-items: center;
          gap: var(--space-sm);
          padding: var(--space-lg) var(--space-lg);
          border-bottom: 1px solid rgba(255,255,255,0.06);
        }
        .sidebar-logo {
          width: 36px;
          height: 36px;
          display: flex;
          align-items: center;
          justify-content: center;
          background: var(--color-primary);
          color: white;
          font-weight: 800;
          font-size: var(--font-size-sm);
          border-radius: var(--radius-md);
          letter-spacing: -0.05em;
        }
        .sidebar-brand-text {
          font-size: var(--font-size-lg);
          font-weight: 700;
          color: var(--sidebar-text-active);
          letter-spacing: -0.02em;
        }
        .sidebar-nav {
          flex: 1;
          padding: var(--space-md);
          display: flex;
          flex-direction: column;
          gap: 2px;
        }
        .sidebar-link {
          display: flex;
          align-items: center;
          gap: var(--space-sm);
          padding: 10px 12px;
          border-radius: var(--radius-md);
          color: var(--sidebar-text);
          font-size: var(--font-size-sm);
          font-weight: 500;
          transition: all var(--transition-fast);
          text-decoration: none;
        }
        .sidebar-link:hover {
          background: var(--sidebar-item-hover);
          color: var(--sidebar-text-active);
        }
        .sidebar-link.active {
          background: var(--sidebar-item-active);
          color: var(--sidebar-text-active);
        }
        .sidebar-link-icon {
          width: 20px;
          text-align: center;
          font-size: 1rem;
        }
        .sidebar-footer {
          padding: var(--space-md) var(--space-lg);
          border-top: 1px solid rgba(255,255,255,0.06);
        }
        .sidebar-version {
          font-size: var(--font-size-xs);
          color: rgba(255,255,255,0.2);
        }
      `}</style>
    </aside>
  );
}
