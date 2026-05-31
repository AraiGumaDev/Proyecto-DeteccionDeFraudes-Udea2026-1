import { useState } from 'react'
import { obtenerVecinos, analizarTransaccion } from '../api/deteccion'
import AlertBadge from '../components/AlertBadge'
import Toast from '../components/Toast'

const fmtCOP = (v) => new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 }).format(v)

const DIST_MAX = 5 // referencia visual para la barra de distancia

function NeighborCard({ vecino, rank }) {
  const pct = Math.min(100, (vecino.distancia_euclidiana / DIST_MAX) * 100)
  const isFraud = vecino.estado_alerta === 3
  return (
    <div className="neighbor-card" style={{ borderColor: isFraud ? 'rgba(220,38,38,.4)' : 'var(--border)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontSize: 11, color: 'var(--text-tertiary)', fontWeight: 600 }}>#{rank}</span>
        <AlertBadge estado={vecino.estado_alerta} />
      </div>
      <div style={{ fontSize: 13, fontFamily: 'monospace', color: 'var(--accent)', marginTop: 4 }}>
        {vecino.id_transaccion}
      </div>
      <div style={{ fontSize: 13, color: 'var(--text-primary)', marginTop: 2 }}>
        {fmtCOP(vecino.monto)}
      </div>
      <div className="neighbor-dist">
        Distancia euclidiana: <span>{vecino.distancia_euclidiana?.toFixed(4)}</span>
      </div>
      <div className="progress-track" style={{ marginTop: 6 }}>
        <div className="progress-fill" style={{
          width: `${pct}%`,
          background: isFraud ? 'var(--fraude)' : 'var(--accent)',
        }} />
      </div>
    </div>
  )
}

export default function MotorKNN() {
  const [id, setId]           = useState('')
  const [k, setK]             = useState(5)
  const [vecinos, setVecinos] = useState(null)
  const [analisis, setAnalisis] = useState(null)
  const [loading, setLoading] = useState(false)
  const [reLoading, setReLoading] = useState(false)
  const [toast, setToast]     = useState(null)

  const buscarVecinos = async (e) => {
    e.preventDefault()
    if (!id.trim()) return
    setLoading(true)
    setVecinos(null)
    setAnalisis(null)
    try {
      const r = await obtenerVecinos(id.trim(), k)
      setVecinos(r)
    } catch (e) {
      setToast({ msg: e.message, type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const reAnalizar = async () => {
    if (!id.trim()) return
    setReLoading(true)
    setAnalisis(null)
    try {
      const r = await analizarTransaccion(id.trim(), k)
      setAnalisis(r)
      setToast({ msg: `Re-análisis completado. Nuevo estado: ${r.estado_alerta_nombre}`, type: 'success' })
    } catch (e) {
      setToast({ msg: e.message, type: 'error' })
    } finally {
      setReLoading(false)
    }
  }

  const fraudeCount = vecinos?.vecinos?.filter(v => v.estado_alerta === 3).length ?? 0

  return (
    <div>
      {toast && <Toast message={toast.msg} type={toast.type} onClose={() => setToast(null)} />}

      <div className="page-header">
        <div>
          <h1 className="page-title">Motor KNN — KD-Tree</h1>
          <p className="page-subtitle">Visualiza los K vecinos más cercanos en el espacio R⁵</p>
        </div>
      </div>

      {/* Info box */}
      <div style={{
        background: 'var(--accent-dim)', border: '1px solid rgba(59,130,246,.3)',
        borderRadius: 10, padding: '12px 16px', marginBottom: 20,
        fontSize: 13, color: 'var(--accent)',
        display: 'flex', gap: 10, alignItems: 'flex-start',
      }}>
        <span>ⓘ</span>
        <span>
          El KD-tree particiona el espacio 5D (monto, hora, frecuencia, tipo, desviación) para encontrar vecinos sin comparar todos los registros.
          Si ≥ 3 de los {k} vecinos son CONFIRMADO_FRAUDE → alerta <strong>ALTA</strong>. Si ≥ 1 → <strong>MEDIA</strong>.
        </span>
      </div>

      {/* Form */}
      <div className="card" style={{ marginBottom: 20 }}>
        <form onSubmit={buscarVecinos}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
            <div className="input-group" style={{ flex: 1 }}>
              <label className="label">ID de transacción</label>
              <input
                className="input"
                value={id}
                onChange={e => setId(e.target.value)}
                placeholder="TXN-000042"
              />
            </div>
            <div className="input-group" style={{ width: 100 }}>
              <label className="label">K vecinos</label>
              <input
                className="input"
                type="number" min={1} max={20}
                value={k}
                onChange={e => setK(+e.target.value)}
              />
            </div>
            <button className="btn btn-primary" type="submit" disabled={loading || !id.trim()}>
              {loading ? <><span className="spinner" style={{ width: 14, height: 14 }} /> Buscando...</> : '⬡ Buscar vecinos'}
            </button>
            <button
              type="button"
              className="btn btn-ghost"
              onClick={reAnalizar}
              disabled={reLoading || !id.trim()}
              title="Re-ejecuta KNN y actualiza el estado_alerta en disco"
            >
              {reLoading ? <span className="spinner" style={{ width: 14, height: 14 }} /> : '↻ Re-analizar'}
            </button>
          </div>
        </form>
      </div>

      {/* Resultado vecinos */}
      {vecinos && (
        <div>
          {/* KPI summary */}
          <div style={{ display: 'flex', gap: 12, marginBottom: 20 }}>
            <div className="card card-sm" style={{ flex: 1 }}>
              <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>Transacción consultada</div>
              <div style={{ fontFamily: 'monospace', color: 'var(--accent)', fontWeight: 600, marginTop: 2 }}>
                {vecinos.id_transaccion_consulta}
              </div>
            </div>
            <div className="card card-sm" style={{ flex: 1 }}>
              <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>K solicitado</div>
              <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--text-primary)' }}>{vecinos.k}</div>
            </div>
            <div className="card card-sm" style={{
              flex: 1,
              borderColor: fraudeCount >= 3 ? 'rgba(239,68,68,.4)' : fraudeCount >= 1 ? 'rgba(245,158,11,.4)' : 'var(--border)',
            }}>
              <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>Vecinos con fraude</div>
              <div style={{
                fontSize: 22, fontWeight: 700,
                color: fraudeCount >= 3 ? 'var(--alta)' : fraudeCount >= 1 ? 'var(--media)' : 'var(--normal)',
              }}>
                {fraudeCount} / {vecinos.k}
              </div>
            </div>
            <div className="card card-sm" style={{ flex: 1 }}>
              <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>Alerta esperada</div>
              <div style={{ marginTop: 4 }}>
                {fraudeCount >= 3
                  ? <span className="badge badge-alta">ALTA</span>
                  : fraudeCount >= 1
                    ? <span className="badge badge-media">MEDIA</span>
                    : <span className="badge badge-normal">NORMAL</span>}
              </div>
            </div>
          </div>

          {/* Grid de vecinos */}
          <p className="section-title">{vecinos.vecinos?.length} vecinos más cercanos en R⁵</p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 10 }}>
            {vecinos.vecinos?.map((v, i) => (
              <NeighborCard key={v.id_transaccion} vecino={v} rank={i + 1} />
            ))}
          </div>

          {/* Visualización distancias */}
          <div className="card" style={{ marginTop: 20 }}>
            <p className="section-title">Distancias euclidianas en R⁵</p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {vecinos.vecinos?.map((v, i) => {
                const pct = Math.min(100, (v.distancia_euclidiana / (vecinos.vecinos[vecinos.vecinos.length - 1]?.distancia_euclidiana || 1)) * 100)
                return (
                  <div key={v.id_transaccion} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <span style={{ fontSize: 11, color: 'var(--text-tertiary)', width: 24, textAlign: 'right' }}>#{i+1}</span>
                    <span style={{ fontSize: 12, fontFamily: 'monospace', color: 'var(--accent)', width: 120 }}>{v.id_transaccion}</span>
                    <div className="progress-track" style={{ flex: 1 }}>
                      <div className="progress-fill" style={{
                        width: `${pct}%`,
                        background: v.estado_alerta === 3 ? 'var(--fraude)' : 'var(--accent)',
                      }} />
                    </div>
                    <span style={{ fontSize: 12, fontFamily: 'monospace', color: 'var(--text-secondary)', width: 60, textAlign: 'right' }}>
                      {v.distancia_euclidiana?.toFixed(4)}
                    </span>
                    <AlertBadge estado={v.estado_alerta} />
                  </div>
                )
              })}
            </div>
          </div>
        </div>
      )}

      {/* Resultado re-análisis */}
      {analisis && (
        <div className="card" style={{ marginTop: 20, borderColor: analisis.estado_alerta >= 2 ? 'rgba(239,68,68,.4)' : 'var(--border)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
            <p style={{ fontWeight: 600 }}>Resultado del re-análisis KNN</p>
            <AlertBadge estado={analisis.estado_alerta_asignado} />
          </div>
          <div className="grid-2">
            {[
              { label: 'ID analizado',    val: analisis.id_transaccion },
              { label: 'K utilizado',     val: analisis.k_utilizado },
              { label: 'Vecinos fraude',  val: `${analisis.vecinos_fraude} de ${analisis.k_utilizado}` },
              { label: 'Estado asignado', val: analisis.estado_alerta_nombre },
            ].map(({ label, val }) => (
              <div key={label} style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <span style={{ fontSize: 11, color: 'var(--text-tertiary)' }}>{label}</span>
                <span style={{ fontFamily: 'monospace', fontWeight: 600 }}>{val}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {!vecinos && !loading && (
        <div className="empty-state">
          <span className="empty-icon">⬡</span>
          <span className="empty-text">Ingresa un ID para explorar el espacio KD-Tree</span>
        </div>
      )}
    </div>
  )
}
