import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid } from 'recharts'
import { obtenerEstadisticas } from '../api/sistema'
import { listarAlertas } from '../api/alertas'
import { escaneoMasivo } from '../api/deteccion'
import AlertBadge from '../components/AlertBadge'
import Toast from '../components/Toast'

const COLORS = { normal: '#10b981', media: '#f59e0b', alta: '#ef4444', confirmado_fraude: '#dc2626', falso_positivo: '#6b7280' }

const fmtCOP = (v) => new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 }).format(v)
const fmtDate = (ts) => new Date(ts * 1000).toLocaleString('es-CO')

export default function Dashboard() {
  const [stats, setStats]     = useState(null)
  const [alertas, setAlertas] = useState(null)
  const [loading, setLoading] = useState(true)
  const [scanning, setScanning] = useState(false)
  const [toast, setToast]     = useState(null)
  const navigate = useNavigate()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [s, a] = await Promise.all([obtenerEstadisticas(), listarAlertas(1)])
      setStats(s)
      setAlertas(a)
    } catch (e) {
      setToast({ msg: e.message, type: 'error' })
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  const handleEscaneo = async () => {
    setScanning(true)
    try {
      const r = await escaneoMasivo()
      setToast({ msg: `Escaneo completado: ${r.transacciones_analizadas} analizadas, ${r.alertas_nuevas_alta} nuevas alertas ALTA`, type: 'success' })
      load()
    } catch (e) {
      setToast({ msg: e.message, type: 'error' })
    } finally {
      setScanning(false)
    }
  }

  const pieData = stats ? [
    { name: 'Normal',          value: stats.distribucion_alertas?.normal || 0,             color: COLORS.normal },
    { name: 'Media',           value: stats.distribucion_alertas?.media || 0,              color: COLORS.media },
    { name: 'Alta',            value: stats.distribucion_alertas?.alta || 0,               color: COLORS.alta },
    { name: 'Fraude conf.',    value: stats.distribucion_alertas?.confirmado_fraude || 0,   color: COLORS.confirmado_fraude },
    { name: 'Falso positivo',  value: stats.distribucion_alertas?.falso_positivo || 0,     color: COLORS.falso_positivo },
  ] : []

  const barData = stats ? [
    { name: 'Normal',    total: stats.distribucion_alertas?.normal || 0,           fill: COLORS.normal },
    { name: 'Media',     total: stats.distribucion_alertas?.media || 0,            fill: COLORS.media },
    { name: 'Alta',      total: stats.distribucion_alertas?.alta || 0,             fill: COLORS.alta },
    { name: 'Fraude',    total: stats.distribucion_alertas?.confirmado_fraude || 0, fill: COLORS.confirmado_fraude },
    { name: 'F.Pos.',    total: stats.distribucion_alertas?.falso_positivo || 0,   fill: COLORS.falso_positivo },
  ] : []

  if (loading) return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 300 }}>
      <div className="spinner" style={{ width: 32, height: 32 }} />
    </div>
  )

  return (
    <div>
      {toast && <Toast message={toast.msg} type={toast.type} onClose={() => setToast(null)} />}

      <div className="page-header">
        <div>
          <h1 className="page-title">Dashboard</h1>
          <p className="page-subtitle">Resumen global del sistema de detección</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button className="btn btn-ghost btn-sm" onClick={load}>↺ Refrescar</button>
          <button className="btn btn-primary btn-sm" onClick={handleEscaneo} disabled={scanning}>
            {scanning ? <><span className="spinner" style={{ width: 14, height: 14 }} /> Escaneando...</> : '⬡ Escaneo masivo'}
          </button>
        </div>
      </div>

      {/* KPIs */}
      <div className="grid-4" style={{ marginBottom: 24 }}>
        <div className="stat-card">
          <span className="stat-label">Total transacciones</span>
          <span className="stat-value">{stats?.total_transacciones?.toLocaleString() ?? '—'}</span>
          <span className="stat-sub">{stats?.total_eliminadas ?? 0} eliminadas lógicamente</span>
        </div>
        <div className="stat-card" style={{ borderColor: 'rgba(245,158,11,.3)' }}>
          <span className="stat-label">Alertas activas</span>
          <span className="stat-value" style={{ color: 'var(--media)' }}>
            {alertas ? alertas.total_alertas : '—'}
          </span>
          <span className="stat-sub">{alertas?.alertas_alta ?? 0} ALTA · {alertas?.alertas_media ?? 0} MEDIA</span>
        </div>
        <div className="stat-card" style={{ borderColor: 'rgba(220,38,38,.3)' }}>
          <span className="stat-label">Fraudes confirmados</span>
          <span className="stat-value" style={{ color: 'var(--fraude)' }}>
            {stats?.distribucion_alertas?.confirmado_fraude ?? '—'}
          </span>
          <span className="stat-sub">{stats?.distribucion_alertas?.falso_positivo ?? 0} falsos positivos</span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Nodos en KD-Tree</span>
          <span className="stat-value" style={{ color: 'var(--accent)' }}>
            {stats?.kdtree?.total_nodos?.toLocaleString() ?? '—'}
          </span>
          <span className="stat-sub">Profundidad máx. {stats?.kdtree?.profundidad_max ?? '—'}</span>
        </div>
      </div>

      {/* Charts */}
      <div className="grid-2" style={{ marginBottom: 24 }}>
        <div className="card">
          <p className="section-title">Distribución por estado de alerta</p>
          <ResponsiveContainer width="100%" height={200}>
            <PieChart>
              <Pie data={pieData} cx="50%" cy="50%" innerRadius={55} outerRadius={85} dataKey="value" paddingAngle={3}>
                {pieData.map((entry, i) => <Cell key={i} fill={entry.color} />)}
              </Pie>
              <Tooltip
                contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 8, fontSize: 12 }}
                formatter={(v) => [v.toLocaleString(), '']}
              />
            </PieChart>
          </ResponsiveContainer>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginTop: 8 }}>
            {pieData.map(d => (
              <div key={d.name} style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 11 }}>
                <span style={{ width: 8, height: 8, borderRadius: '50%', background: d.color, display: 'inline-block' }} />
                <span style={{ color: 'var(--text-secondary)' }}>{d.name}</span>
                <span style={{ color: 'var(--text-primary)', fontWeight: 600 }}>{d.value}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="card">
          <p className="section-title">Conteo por nivel</p>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={barData} margin={{ top: 5, right: 5, left: -20, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis dataKey="name" tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} />
              <YAxis tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} />
              <Tooltip
                contentStyle={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: 8, fontSize: 12 }}
              />
              <Bar dataKey="total" radius={[4, 4, 0, 0]}>
                {barData.map((entry, i) => <Cell key={i} fill={entry.fill} />)}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Hash Table + KD-Tree metrics */}
      <div className="grid-2" style={{ marginBottom: 24 }}>
        <div className="card">
          <p className="section-title">Hash Table</p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {[
              { label: 'Buckets totales', val: stats?.hash_table?.buckets_totales?.toLocaleString() },
              { label: 'Factor de carga',  val: stats?.hash_table?.factor_carga?.toFixed(2) },
              { label: 'Colisiones',       val: stats?.hash_table?.colisiones?.toLocaleString() },
            ].map(({ label, val }) => (
              <div key={label} style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13 }}>
                <span style={{ color: 'var(--text-secondary)' }}>{label}</span>
                <span style={{ fontFamily: 'monospace', color: 'var(--text-primary)', fontWeight: 600 }}>{val ?? '—'}</span>
              </div>
            ))}
            {stats?.hash_table?.factor_carga !== undefined && (
              <div>
                <div className="progress-track">
                  <div className="progress-fill" style={{
                    width: `${(stats.hash_table.factor_carga * 100).toFixed(0)}%`,
                    background: stats.hash_table.factor_carga > 0.8 ? 'var(--alta)' : 'var(--accent)',
                  }} />
                </div>
                <div style={{ fontSize: 11, color: 'var(--text-tertiary)', marginTop: 4 }}>
                  Carga: {(stats.hash_table.factor_carga * 100).toFixed(0)}%
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="card">
          <p className="section-title">KD-Tree</p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {[
              { label: 'Nodos activos',    val: stats?.kdtree?.total_nodos?.toLocaleString() },
              { label: 'Profundidad máx.', val: stats?.kdtree?.profundidad_max },
              { label: 'Dimensiones',      val: '5D (R⁵)' },
            ].map(({ label, val }) => (
              <div key={label} style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13 }}>
                <span style={{ color: 'var(--text-secondary)' }}>{label}</span>
                <span style={{ fontFamily: 'monospace', color: 'var(--accent)', fontWeight: 600 }}>{val ?? '—'}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Alertas recientes */}
      {alertas?.transacciones?.length > 0 && (
        <div className="card">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
            <p className="section-title" style={{ marginBottom: 0 }}>Alertas recientes</p>
            <button className="btn btn-ghost btn-sm" onClick={() => navigate('/alertas')}>Ver todas →</button>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Cuenta</th>
                  <th>Monto</th>
                  <th>Tipo</th>
                  <th>Fecha</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {alertas.transacciones.slice(0, 5).map(t => (
                  <tr key={t.id_transaccion} style={{ cursor: 'pointer' }} onClick={() => navigate('/buscar')}>
                    <td><span className="font-mono" style={{ color: 'var(--accent)', fontSize: 12 }}>{t.id_transaccion}</span></td>
                    <td style={{ color: 'var(--text-secondary)' }}>{t.num_cuenta}</td>
                    <td style={{ fontWeight: 600 }}>{fmtCOP(t.monto)}</td>
                    <td><span className="badge" style={{ background: 'var(--bg-input)', color: 'var(--text-secondary)' }}>{t.tipo}</span></td>
                    <td style={{ color: 'var(--text-secondary)', fontSize: 12 }}>{fmtDate(t.timestamp)}</td>
                    <td><AlertBadge estado={t.estado_alerta} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
