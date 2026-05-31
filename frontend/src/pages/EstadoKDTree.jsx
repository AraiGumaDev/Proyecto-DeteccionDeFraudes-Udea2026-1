import { useState, useEffect } from 'react'
import { obtenerSalud } from '../api/sistema'
import { obtenerEstadisticas } from '../api/sistema'
import Toast from '../components/Toast'

function Metric({ label, val, color }) {
  return (
    <div style={{
      background: 'var(--bg-input)', border: '1px solid var(--border)',
      borderRadius: 10, padding: '14px 16px', display: 'flex',
      flexDirection: 'column', gap: 4,
    }}>
      <span style={{ fontSize: 11, color: 'var(--text-tertiary)' }}>{label}</span>
      <span style={{ fontSize: 22, fontWeight: 700, fontFamily: 'monospace', color: color || 'var(--text-primary)' }}>
        {val ?? '—'}
      </span>
    </div>
  )
}

function TreeNode({ depth = 0, maxDepth = 14, isLeft }) {
  if (depth > 4) return null
  const size = Math.max(6, 18 - depth * 3)
  const gap  = Math.max(4, 24 - depth * 4)
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
      <div style={{
        width: size, height: size, borderRadius: '50%',
        background: depth === 0 ? 'var(--accent)' : depth === 1 ? '#7c3aed' : depth === 2 ? '#db2777' : 'var(--media)',
        boxShadow: depth === 0 ? '0 0 12px rgba(59,130,246,.5)' : 'none',
        flexShrink: 0,
      }} />
      {depth < 4 && (
        <div style={{ display: 'flex', gap, alignItems: 'flex-start' }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
            <div style={{ width: 1, height: 12, background: 'var(--border)' }} />
            <TreeNode depth={depth + 1} maxDepth={maxDepth} isLeft={true} />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
            <div style={{ width: 1, height: 12, background: 'var(--border)' }} />
            <TreeNode depth={depth + 1} maxDepth={maxDepth} isLeft={false} />
          </div>
        </div>
      )}
    </div>
  )
}

export default function EstadoKDTree() {
  const [salud, setSalud]   = useState(null)
  const [stats, setStats]   = useState(null)
  const [loading, setLoading] = useState(true)
  const [toast, setToast]   = useState(null)

  const load = async () => {
    setLoading(true)
    try {
      const [s, st] = await Promise.all([obtenerSalud(), obtenerEstadisticas()])
      setSalud(s)
      setStats(st)
    } catch (e) {
      setToast({ msg: e.message, type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const isOk = salud?.estado === 'OK'
  const carga = salud?.hash_table_carga ?? 0

  return (
    <div>
      {toast && <Toast message={toast.msg} type={toast.type} onClose={() => setToast(null)} />}

      <div className="page-header">
        <div>
          <h1 className="page-title">Estado del KD-Tree</h1>
          <p className="page-subtitle">Diagnóstico interno — KD-Tree en memoria · Hash Table · Archivo en disco</p>
        </div>
        <button className="btn btn-ghost btn-sm" onClick={load}>↺ Refrescar</button>
      </div>

      {/* Estado general */}
      <div className="card" style={{
        marginBottom: 20,
        borderColor: isOk ? 'rgba(16,185,129,.4)' : 'rgba(239,68,68,.4)',
        background: isOk ? 'rgba(16,185,129,.05)' : 'rgba(239,68,68,.05)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{
            width: 44, height: 44, borderRadius: '50%',
            background: isOk ? 'var(--normal-bg)' : 'var(--alta-bg)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 22,
          }}>
            {loading ? '…' : isOk ? '✓' : '✕'}
          </div>
          <div>
            <p style={{ fontWeight: 700, fontSize: 16, color: isOk ? 'var(--normal)' : 'var(--alta)' }}>
              Sistema {loading ? 'cargando…' : isOk ? 'OPERATIVO' : 'CON ERRORES'}
            </p>
            <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 2 }}>
              {isOk ? 'KD-Tree, Hash Table y archivo en disco funcionan correctamente' : 'Revisa el estado del archivo binario y las estructuras en memoria'}
            </p>
          </div>
        </div>
      </div>

      {!loading && (
        <>
          {/* KD-Tree métricas */}
          <div style={{ marginBottom: 24 }}>
            <p className="section-title">KD-Tree en memoria (R⁵ — 5 dimensiones)</p>
            <div className="grid-4">
              <Metric label="Nodos activos" val={salud?.kdtree_nodos?.toLocaleString()} color="var(--accent)" />
              <Metric label="Profundidad máx." val={stats?.kdtree?.profundidad_max} color="var(--accent)" />
              <Metric label="Dimensiones" val="5" color="var(--accent)" />
              <Metric label="Algoritmo" val="KNN" color="var(--accent)" />
            </div>

            {/* Visualización árbol */}
            <div className="card" style={{ marginTop: 14, display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '30px 20px' }}>
              <p className="section-title" style={{ alignSelf: 'flex-start', marginBottom: 20 }}>
                Visualización del árbol (primeros 4 niveles, dimensiones D1–D5 alternantes)
              </p>
              <TreeNode depth={0} maxDepth={stats?.kdtree?.profundidad_max ?? 14} />
              <div style={{ marginTop: 20, display: 'flex', gap: 16, flexWrap: 'wrap', justifyContent: 'center' }}>
                {[
                  { color: 'var(--accent)',  label: 'Nivel 0 — Raíz (split por D1: monto)' },
                  { color: '#7c3aed',        label: 'Nivel 1 — Split por D2: hora' },
                  { color: '#db2777',        label: 'Nivel 2 — Split por D3: frecuencia' },
                  { color: 'var(--media)',   label: 'Nivel 3+ — D4, D5 y ciclo' },
                ].map(({ color, label }) => (
                  <div key={label} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 11 }}>
                    <span style={{ width: 8, height: 8, borderRadius: '50%', background: color, display: 'inline-block', flexShrink: 0 }} />
                    <span style={{ color: 'var(--text-secondary)' }}>{label}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Hash Table */}
          <div style={{ marginBottom: 24 }}>
            <p className="section-title">Hash Table (índice por id_transaccion → acceso O(1))</p>
            <div className="grid-4">
              <Metric label="Buckets totales" val={stats?.hash_table?.buckets_totales?.toLocaleString()} />
              <Metric label="Factor de carga" val={stats?.hash_table?.factor_carga?.toFixed(3)}
                color={stats?.hash_table?.factor_carga > 0.8 ? 'var(--alta)' : 'var(--normal)'} />
              <Metric label="Colisiones" val={stats?.hash_table?.colisiones?.toLocaleString()}
                color={stats?.hash_table?.colisiones > 100 ? 'var(--media)' : 'var(--text-primary)'} />
              <Metric label="Entradas activas" val={stats?.total_transacciones?.toLocaleString()} />
            </div>

            <div className="card" style={{ marginTop: 14 }}>
              <p style={{ fontSize: 13, fontWeight: 500, marginBottom: 10 }}>Factor de carga de la tabla hash</p>
              <div className="progress-track" style={{ height: 10 }}>
                <div className="progress-fill" style={{
                  width: `${Math.min(100, (carga * 100).toFixed(0))}%`,
                  background: carga > 0.8 ? 'var(--alta)' : carga > 0.6 ? 'var(--media)' : 'var(--normal)',
                }} />
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6, fontSize: 11, color: 'var(--text-secondary)' }}>
                <span>0%</span>
                <span style={{ fontWeight: 600, color: carga > 0.8 ? 'var(--alta)' : 'var(--text-primary)' }}>
                  {(carga * 100).toFixed(1)}% de carga
                </span>
                <span>100%</span>
              </div>
              {carga > 0.75 && (
                <p style={{ fontSize: 12, color: 'var(--media)', marginTop: 8 }}>
                  ⚠ Factor de carga elevado. El sistema podría requerir rehashing.
                </p>
              )}
            </div>
          </div>

          {/* Archivo en disco */}
          <div style={{ marginBottom: 24 }}>
            <p className="section-title">Archivo binario en disco (transacciones.dat)</p>
            <div className="grid-3">
              <Metric label="Tamaño total" val={salud?.archivo_bytes ? `${(salud.archivo_bytes / 1024).toFixed(1)} KB` : '—'} />
              <Metric label="Registros (128 B c/u)" val={salud?.archivo_bytes ? Math.floor(salud.archivo_bytes / 128).toLocaleString() : '—'} />
              <Metric label="Eliminados lógicos" val={stats?.total_eliminadas?.toLocaleString()} color="var(--falso)" />
            </div>
            <div className="card" style={{ marginTop: 14, fontFamily: 'monospace', fontSize: 12, color: 'var(--text-secondary)', lineHeight: 1.8 }}>
              <p style={{ color: 'var(--text-primary)', fontWeight: 600, marginBottom: 8 }}>Estructura del registro binario (128 bytes fijos)</p>
              {[
                ['Bytes 0–18',   'id_transaccion   (char[19])'],
                ['Bytes 19–37',  'num_cuenta       (char[19])'],
                ['Bytes 38–45',  'monto            (double, 8B)'],
                ['Bytes 46–53',  'timestamp        (int64, 8B)'],
                ['Bytes 54–61',  'd1_monto_norm    (double, 8B)'],
                ['Bytes 62–69',  'd2_hora          (double, 8B)'],
                ['Bytes 70–77',  'd3_frecuencia    (double, 8B)'],
                ['Bytes 78–85',  'd4_tipo          (double, 8B)'],
                ['Bytes 86–93',  'd5_desviacion    (double, 8B)'],
                ['Bytes 94–95',  'tipo             (enum/short, 2B)'],
                ['Byte  96',     'estado_alerta    (uint8)'],
                ['Byte  97',     'deleted          (uint8, 0/1)'],
                ['Bytes 98–127', 'padding          (30B reservados)'],
              ].map(([pos, desc]) => (
                <div key={pos} style={{ display: 'flex', gap: 16 }}>
                  <span style={{ color: 'var(--accent)', minWidth: 110 }}>{pos}</span>
                  <span>{desc}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Espacio 5D */}
          <div>
            <p className="section-title">Espacio R⁵ — Dimensiones del KD-Tree</p>
            <div className="grid-3">
              {[
                { d: 'D1', name: 'Monto normalizado', range: '[0.0, 1.0]', desc: 'Valor en pesos normalizado sobre el rango histórico' },
                { d: 'D2', name: 'Hora del día',      range: '[0.0, 23.99]', desc: 'Transacciones de madrugada = señal de alerta' },
                { d: 'D3', name: 'Frecuencia 24h',    range: '[0, ∞)',     desc: 'Nº de transacciones de la cuenta en las últimas 24h' },
                { d: 'D4', name: 'Tipo',               range: '{0, 0.5, 1}', desc: 'Retiro=0, Depósito=0.5, Transferencia=1' },
                { d: 'D5', name: 'Desviación σ',       range: '[0, ∞)',     desc: 'Desviaciones estándar respecto al promedio de la cuenta' },
              ].map(({ d, name, range, desc }) => (
                <div key={d} style={{
                  background: 'var(--bg-input)', border: '1px solid var(--border)',
                  borderRadius: 10, padding: '14px 16px',
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                    <span style={{ fontFamily: 'monospace', fontWeight: 700, color: 'var(--accent)', fontSize: 14 }}>{d}</span>
                    <span style={{ fontFamily: 'monospace', fontSize: 11, color: 'var(--text-tertiary)' }}>{range}</span>
                  </div>
                  <p style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-primary)', marginBottom: 4 }}>{name}</p>
                  <p style={{ fontSize: 11, color: 'var(--text-secondary)', lineHeight: 1.5 }}>{desc}</p>
                </div>
              ))}
            </div>
          </div>
        </>
      )}

      {loading && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 80 }}>
          <div className="spinner" style={{ width: 32, height: 32 }} />
        </div>
      )}
    </div>
  )
}
