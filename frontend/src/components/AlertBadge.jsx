const CONFIG = {
  0: { label: 'NORMAL',            cls: 'badge-normal',  dot: '#10b981' },
  1: { label: 'MEDIA',             cls: 'badge-media',   dot: '#f59e0b' },
  2: { label: 'ALTA',              cls: 'badge-alta',    dot: '#ef4444' },
  3: { label: 'CONFIRMADO FRAUDE', cls: 'badge-fraude',  dot: '#dc2626' },
  4: { label: 'FALSO POSITIVO',    cls: 'badge-falso',   dot: '#6b7280' },
}

export default function AlertBadge({ estado }) {
  const cfg = CONFIG[estado] ?? CONFIG[0]
  return (
    <span className={`badge ${cfg.cls}`}>
      <span style={{
        width: 6, height: 6, borderRadius: '50%',
        background: cfg.dot, display: 'inline-block'
      }} />
      {cfg.label}
    </span>
  )
}

export { CONFIG as ALERT_CONFIG }
