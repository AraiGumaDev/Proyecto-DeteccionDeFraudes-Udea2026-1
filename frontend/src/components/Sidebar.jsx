import { NavLink } from 'react-router-dom'
import Logo from './Logo'
import ThemeToggle from './ThemeToggle'

const NAV = [
  { to: '/dashboard', icon: '◈', label: 'Dashboard' },
  { to: '/registrar', icon: '＋', label: 'Registrar transacción' },
  { to: '/alertas',   icon: '⚠', label: 'Alertas activas' },
  { to: '/knn',       icon: '⬡', label: 'Motor KNN' },
  { to: '/consultas', icon: '⊞', label: 'Consultas 5D' },
  { to: '/buscar',    icon: '⌕', label: 'Buscar por ID' },
  { to: '/estado',    icon: '⧖', label: 'Estado KD-Tree' },
]

const sidebarStyle = {
  width: 'var(--sidebar-w)',
  minWidth: 'var(--sidebar-w)',
  background: 'var(--bg-panel)',
  borderRight: '1px solid var(--border)',
  display: 'flex',
  flexDirection: 'column',
  height: '100vh',
  position: 'sticky',
  top: 0,
  overflow: 'hidden',
}

const logoStyle = {
  padding: '20px 20px 14px',
  borderBottom: '1px solid var(--border)',
  marginBottom: 8,
}

const titleStyle = {
  fontSize: 18,
  fontWeight: 700,
  color: 'var(--text-primary)',
  letterSpacing: '-.03em',
  lineHeight: 1.1,
}

const navStyle = { padding: '0 10px', flex: 1, overflowY: 'auto' }

const sectionLabel = {
  fontSize: 10,
  color: 'var(--text-tertiary)',
  fontWeight: 600,
  letterSpacing: '.08em',
  textTransform: 'uppercase',
  padding: '10px 10px 4px',
}

const footerStyle = {
  padding: '12px 20px',
  borderTop: '1px solid var(--border)',
  fontSize: 11,
  color: 'var(--text-tertiary)',
}

export default function Sidebar() {
  return (
    <aside style={sidebarStyle}>
      <div style={logoStyle}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
          <Logo size={38} />
          <div style={titleStyle}>
            Fraud<span style={{ color: 'var(--accent)' }}>Guard</span>
          </div>
        </div>
      </div>

      <nav style={navStyle}>
        <div style={sectionLabel}>Módulos</div>
        {NAV.map(({ to, icon, label }) => (
          <NavLink
            key={to}
            to={to}
            style={({ isActive }) => ({
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              padding: '9px 12px',
              borderRadius: 8,
              marginBottom: 2,
              textDecoration: 'none',
              fontSize: 13,
              fontWeight: 500,
              color: isActive ? 'var(--accent)' : 'var(--text-secondary)',
              background: isActive ? 'var(--accent-dim)' : 'transparent',
              transition: 'all .15s',
            })}
          >
            <span style={{ fontSize: 14, width: 18, textAlign: 'center' }}>{icon}</span>
            {label}
          </NavLink>
        ))}
      </nav>

      <div style={{ padding: '12px 16px', borderTop: '1px solid var(--border)' }}>
        <ThemeToggle />
      </div>

      <div style={footerStyle}>
        API · localhost:8080/api/v1
      </div>
    </aside>
  )
}
