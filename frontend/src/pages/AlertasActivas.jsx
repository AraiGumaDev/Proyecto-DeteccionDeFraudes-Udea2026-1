import { useState, useEffect, useCallback } from 'react'
import { listarAlertas, confirmarFraude, descartarAlerta } from '../api/alertas'
import AlertBadge from '../components/AlertBadge'
import DimensionVector from '../components/DimensionVector'
import ConfirmModal from '../components/ConfirmModal'
import Toast from '../components/Toast'

const fmtCOP   = (v) => new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 }).format(v)
const fmtDate  = (ts) => new Date(ts * 1000).toLocaleString('es-CO')

export default function AlertasActivas() {
  const [data, setData]       = useState(null)
  const [loading, setLoading] = useState(true)
  const [nivel, setNivel]     = useState(1)
  const [selected, setSelected] = useState(null)
  const [modal, setModal]     = useState(null) // { id, action: 'confirmar'|'descartar' }
  const [acting, setActing]   = useState(false)
  const [toast, setToast]     = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const d = await listarAlertas(nivel)
      setData(d)
    } catch (e) {
      setToast({ msg: e.message, type: 'error' })
    } finally {
      setLoading(false)
    }
  }, [nivel])

  useEffect(() => { load() }, [load])

  const handleAction = async () => {
    if (!modal) return
    setActing(true)
    try {
      if (modal.action === 'confirmar') {
        await confirmarFraude(modal.id)
        setToast({ msg: `Fraude confirmado: ${modal.id}`, type: 'success' })
      } else {
        await descartarAlerta(modal.id)
        setToast({ msg: `Alerta descartada como falso positivo: ${modal.id}`, type: 'info' })
      }
      setModal(null)
      if (selected?.id_transaccion === modal.id) setSelected(null)
      load()
    } catch (e) {
      setToast({ msg: e.message, type: 'error' })
    } finally {
      setActing(false)
    }
  }

  return (
    <div>
      {toast && <Toast message={toast.msg} type={toast.type} onClose={() => setToast(null)} />}
      {modal && (
        <ConfirmModal
          title={modal.action === 'confirmar' ? '¿Confirmar fraude?' : '¿Descartar como falso positivo?'}
          body={modal.action === 'confirmar'
            ? `Se marcará ${modal.id} como CONFIRMADO_FRAUDE. Esta transacción influirá en futuros análisis KNN como vecino fraudulento.`
            : `Se marcará ${modal.id} como FALSO_POSITIVO. La transacción no influirá como fraude en futuros KNN.`}
          confirmLabel={modal.action === 'confirmar' ? 'Confirmar fraude' : 'Descartar alerta'}
          confirmClass={modal.action === 'confirmar' ? 'btn-danger' : 'btn-warning'}
          onConfirm={handleAction}
          onCancel={() => setModal(null)}
        />
      )}

      <div className="page-header">
        <div>
          <h1 className="page-title">Alertas activas</h1>
          <p className="page-subtitle">UPDATE — Transacciones que requieren revisión del analista</p>
        </div>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          <select className="select input" style={{ width: 'auto' }} value={nivel} onChange={e => setNivel(+e.target.value)}>
            <option value={1}>Media + Alta</option>
            <option value={2}>Solo Alta</option>
          </select>
          <button className="btn btn-ghost btn-sm" onClick={load}>↺</button>
        </div>
      </div>

      {/* Resumen */}
      {data && (
        <div style={{ display: 'flex', gap: 12, marginBottom: 20 }}>
          <div className="card card-sm" style={{ flex: 1, borderColor: 'rgba(239,68,68,.3)' }}>
            <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>Alertas ALTA</div>
            <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--alta)' }}>{data.alertas_alta}</div>
          </div>
          <div className="card card-sm" style={{ flex: 1, borderColor: 'rgba(245,158,11,.3)' }}>
            <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>Alertas MEDIA</div>
            <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--media)' }}>{data.alertas_media}</div>
          </div>
          <div className="card card-sm" style={{ flex: 1 }}>
            <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>Total activas</div>
            <div style={{ fontSize: 24, fontWeight: 700 }}>{data.total_alertas}</div>
          </div>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: selected ? '1fr 360px' : '1fr', gap: 16 }}>
        {/* Tabla */}
        <div>
          {loading ? (
            <div style={{ display: 'flex', justifyContent: 'center', padding: 60 }}>
              <div className="spinner" style={{ width: 28, height: 28 }} />
            </div>
          ) : !data?.transacciones?.length ? (
            <div className="empty-state card">
              <span className="empty-icon">✓</span>
              <span className="empty-text">Sin alertas activas</span>
            </div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Cuenta</th>
                    <th>Monto</th>
                    <th>Tipo</th>
                    <th>Hora</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {data.transacciones.map(t => (
                    <tr
                      key={t.id_transaccion}
                      style={{ cursor: 'pointer', background: selected?.id_transaccion === t.id_transaccion ? 'var(--bg-hover)' : '' }}
                      onClick={() => setSelected(t)}
                    >
                      <td>
                        <span className="font-mono" style={{ color: 'var(--accent)', fontSize: 12 }}>
                          {t.id_transaccion}
                        </span>
                      </td>
                      <td style={{ color: 'var(--text-secondary)' }}>{t.num_cuenta}</td>
                      <td style={{ fontWeight: 600 }}>{fmtCOP(t.monto)}</td>
                      <td>
                        <span className="badge" style={{ background: 'var(--bg-input)', color: 'var(--text-secondary)' }}>
                          {t.tipo}
                        </span>
                      </td>
                      <td style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{fmtDate(t.timestamp)}</td>
                      <td><AlertBadge estado={t.estado_alerta} /></td>
                      <td onClick={e => e.stopPropagation()}>
                        <div style={{ display: 'flex', gap: 6 }}>
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={() => setModal({ id: t.id_transaccion, action: 'confirmar' })}
                          >Fraude</button>
                          <button
                            className="btn btn-warning btn-sm"
                            onClick={() => setModal({ id: t.id_transaccion, action: 'descartar' })}
                          >F.Pos.</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Panel detalle */}
        {selected && (
          <div className="card" style={{ position: 'sticky', top: 0, alignSelf: 'start' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 14 }}>
              <div>
                <p style={{ fontWeight: 600, fontSize: 14 }}>{selected.id_transaccion}</p>
                <p style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{selected.num_cuenta}</p>
              </div>
              <button className="btn btn-ghost btn-sm" onClick={() => setSelected(null)}>✕</button>
            </div>

            <AlertBadge estado={selected.estado_alerta} />

            <div style={{ marginTop: 14, display: 'flex', flexDirection: 'column', gap: 8 }}>
              {[
                { label: 'Monto',     val: fmtCOP(selected.monto) },
                { label: 'Tipo',      val: selected.tipo },
                { label: 'Fecha',     val: fmtDate(selected.timestamp) },
                { label: 'En disco',  val: `offset ${selected.offset_disco}B` },
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

            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 16 }}>
              <button
                className="btn btn-danger"
                style={{ justifyContent: 'center' }}
                disabled={acting}
                onClick={() => setModal({ id: selected.id_transaccion, action: 'confirmar' })}
              >
                ✕ Confirmar fraude
              </button>
              <button
                className="btn btn-warning"
                style={{ justifyContent: 'center' }}
                disabled={acting}
                onClick={() => setModal({ id: selected.id_transaccion, action: 'descartar' })}
              >
                ○ Marcar falso positivo
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
