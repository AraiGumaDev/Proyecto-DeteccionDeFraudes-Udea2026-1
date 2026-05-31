import { useState } from 'react'
import { buscarPorRango } from '../api/deteccion'
import AlertBadge from '../components/AlertBadge'
import DimensionVector from '../components/DimensionVector'
import Toast from '../components/Toast'

const fmtCOP  = (v) => new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 }).format(v)
const fmtDate = (ts) => new Date(ts * 1000).toLocaleString('es-CO')

const DIMS = [
  {
    d: 1, name: 'Monto normalizado',
    minKey: 'd1_monto_min', maxKey: 'd1_monto_max',
    hint: '0.0 – 1.0 (normalizado)',
    minDef: '', maxDef: '',
    step: '0.01', type: 'number',
  },
  {
    d: 2, name: 'Hora del día',
    minKey: 'd2_hora_min', maxKey: 'd2_hora_max',
    hint: '0.0 – 23.99',
    minDef: '', maxDef: '',
    step: '0.1', type: 'number',
  },
  {
    d: 3, name: 'Frecuencia (24h)',
    minKey: 'd3_frecuencia_min', maxKey: 'd3_frecuencia_max',
    hint: 'Transacciones de la cuenta en las últimas 24h',
    minDef: '', maxDef: '',
    step: '1', type: 'number',
  },
  {
    d: 4, name: 'Tipo',
    minKey: 'd4_tipo_min', maxKey: 'd4_tipo_max',
    hint: 'Retiro=0, Depósito=0.5, Transferencia=1',
    minDef: '', maxDef: '',
    step: '0.5', type: 'number',
  },
  {
    d: 5, name: 'Desviación estándar',
    minKey: 'd5_desviacion_min', maxKey: 'd5_desviacion_max',
    hint: 'Desviaciones respecto al promedio de la cuenta',
    minDef: '', maxDef: '',
    step: '0.1', type: 'number',
  },
]

const PRESETS = [
  {
    label: 'Retiros madrugada + alto monto',
    desc: 'Monto alto · Hora 0–4 · Retiro',
    values: { d1_monto_min: 0.7, d1_monto_max: 1.0, d2_hora_min: 0, d2_hora_max: 4, d4_tipo_min: 0, d4_tipo_max: 0 },
  },
  {
    label: 'Alta frecuencia + desviación grande',
    desc: 'Frecuencia ≥ 8 · Desviación ≥ 2σ',
    values: { d3_frecuencia_min: 8, d3_frecuencia_max: 999, d5_desviacion_min: 2, d5_desviacion_max: 999 },
  },
  {
    label: 'Transferencias sospechosas',
    desc: 'Transferencias con monto medio-alto',
    values: { d1_monto_min: 0.5, d1_monto_max: 1.0, d4_tipo_min: 1, d4_tipo_max: 1 },
  },
]

export default function ConsultasMultidimensionales() {
  const [fields, setFields]   = useState({})
  const [result, setResult]   = useState(null)
  const [loading, setLoading] = useState(false)
  const [toast, setToast]     = useState(null)
  const [selected, setSelected] = useState(null)

  const handleChange = (e) => {
    const { name, value } = e.target
    setFields(f => ({ ...f, [name]: value === '' ? undefined : parseFloat(value) }))
  }

  const applyPreset = (preset) => {
    setFields(preset.values)
    setResult(null)
  }

  const clearAll = () => { setFields({}); setResult(null) }

  const handleSearch = async (e) => {
    e.preventDefault()
    setLoading(true)
    setResult(null)
    setSelected(null)
    try {
      const payload = {}
      Object.entries(fields).forEach(([k, v]) => { if (v !== undefined && v !== '') payload[k] = v })
      const r = await buscarPorRango(payload)
      setResult(r)
    } catch (e) {
      setToast({ msg: e.message, type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      {toast && <Toast message={toast.msg} type={toast.type} onClose={() => setToast(null)} />}

      <div className="page-header">
        <div>
          <h1 className="page-title">Consultas 5D — Búsqueda por rango</h1>
          <p className="page-subtitle">Hipercubo en R⁵ — el KD-tree poda ramas irrelevantes sin recorrer todos los registros</p>
        </div>
      </div>

      {/* Info box */}
      <div style={{
        background: 'rgba(167,139,250,.08)', border: '1px solid rgba(167,139,250,.3)',
        borderRadius: 10, padding: '12px 16px', marginBottom: 20,
        fontSize: 13, color: '#a78bfa',
      }}>
        ⬡ Define un hipercubo en el espacio R⁵ dejando vacíos los campos sin restricción. El KD-tree poda ramas del árbol que no intersectan el cubo, logrando búsquedas sub-lineales.
      </div>

      {/* Presets */}
      <div style={{ marginBottom: 16 }}>
        <p className="section-title">Búsquedas predefinidas</p>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          {PRESETS.map(p => (
            <button key={p.label} className="btn btn-ghost btn-sm" onClick={() => applyPreset(p)}>
              {p.label}
            </button>
          ))}
          <button className="btn btn-ghost btn-sm" onClick={clearAll}>✕ Limpiar</button>
        </div>
      </div>

      <form onSubmit={handleSearch}>
        <div className="card" style={{ marginBottom: 20 }}>
          <p className="section-title">Rangos por dimensión (dejar vacío = sin límite)</p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {DIMS.map(dim => (
              <div key={dim.d} style={{
                display: 'grid', gridTemplateColumns: '160px 1fr 1fr',
                gap: 12, alignItems: 'center',
              }}>
                <div>
                  <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--accent)' }}>D{dim.d}</div>
                  <div style={{ fontSize: 12, color: 'var(--text-primary)', fontWeight: 500 }}>{dim.name}</div>
                  <div style={{ fontSize: 10, color: 'var(--text-tertiary)', marginTop: 1 }}>{dim.hint}</div>
                </div>
                <div className="input-group">
                  <label className="label">Mínimo</label>
                  <input
                    className="input"
                    type={dim.type} step={dim.step} name={dim.minKey}
                    value={fields[dim.minKey] ?? ''}
                    onChange={handleChange}
                    placeholder="sin límite"
                  />
                </div>
                <div className="input-group">
                  <label className="label">Máximo</label>
                  <input
                    className="input"
                    type={dim.type} step={dim.step} name={dim.maxKey}
                    value={fields[dim.maxKey] ?? ''}
                    onChange={handleChange}
                    placeholder="sin límite"
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? <><span className="spinner" style={{ width: 14, height: 14 }} /> Buscando en KD-Tree...</> : '⊞ Ejecutar búsqueda por rango'}
        </button>
      </form>

      {/* Resultado */}
      {result && (
        <div style={{ marginTop: 24 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
            <div>
              <p className="page-title" style={{ fontSize: 16 }}>
                {result.total} transacción{result.total !== 1 ? 'es' : ''} encontrada{result.total !== 1 ? 's' : ''}
              </p>
              <p style={{ fontSize: 12, color: 'var(--text-secondary)' }}>
                El KD-tree podó ramas del árbol que no intersectan el hipercubo definido
              </p>
            </div>
          </div>

          {result.total === 0 ? (
            <div className="empty-state card">
              <span className="empty-icon">⊡</span>
              <span className="empty-text">Ninguna transacción dentro del rango especificado</span>
            </div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: selected ? '1fr 360px' : '1fr', gap: 16 }}>
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
                    {result.transacciones.map(t => (
                      <tr
                        key={t.id_transaccion}
                        style={{ cursor: 'pointer', background: selected?.id_transaccion === t.id_transaccion ? 'var(--bg-hover)' : '' }}
                        onClick={() => setSelected(s => s?.id_transaccion === t.id_transaccion ? null : t)}
                      >
                        <td><span className="font-mono" style={{ color: 'var(--accent)', fontSize: 12 }}>{t.id_transaccion}</span></td>
                        <td style={{ color: 'var(--text-secondary)' }}>{t.num_cuenta}</td>
                        <td style={{ fontWeight: 600 }}>{fmtCOP(t.monto)}</td>
                        <td><span className="badge" style={{ background: 'var(--bg-input)', color: 'var(--text-secondary)' }}>{t.tipo}</span></td>
                        <td style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{fmtDate(t.timestamp)}</td>
                        <td><AlertBadge estado={t.estado_alerta} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {selected && (
                <div className="card" style={{ position: 'sticky', top: 0, alignSelf: 'start' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
                    <p style={{ fontWeight: 600, fontFamily: 'monospace', color: 'var(--accent)' }}>{selected.id_transaccion}</p>
                    <button className="btn btn-ghost btn-sm" onClick={() => setSelected(null)}>✕</button>
                  </div>
                  <AlertBadge estado={selected.estado_alerta} />
                  <div style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 8 }}>
                    {[
                      { label: 'Monto',  val: fmtCOP(selected.monto) },
                      { label: 'Cuenta', val: selected.num_cuenta },
                      { label: 'Tipo',   val: selected.tipo },
                      { label: 'Fecha',  val: fmtDate(selected.timestamp) },
                    ].map(({ label, val }) => (
                      <div key={label} style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13 }}>
                        <span style={{ color: 'var(--text-secondary)' }}>{label}</span>
                        <span style={{ fontFamily: 'monospace' }}>{val}</span>
                      </div>
                    ))}
                  </div>
                  <div style={{ marginTop: 14 }}>
                    <p className="section-title">Vector R⁵</p>
                    <DimensionVector dimensiones={selected.dimensiones} />
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
