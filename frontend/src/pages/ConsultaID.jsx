import { useState } from 'react'
import { obtenerTransaccion, eliminarTransaccion, listarTransacciones } from '../api/transacciones'
import AlertBadge from '../components/AlertBadge'
import DimensionVector from '../components/DimensionVector'
import ConfirmModal from '../components/ConfirmModal'
import Toast from '../components/Toast'

const fmtCOP  = (v) => new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0 }).format(v)
const fmtDate = (ts) => new Date(ts * 1000).toLocaleString('es-CO')

export default function ConsultaID() {
  const [id, setId]           = useState('')
  const [txn, setTxn]         = useState(null)
  const [loading, setLoading] = useState(false)
  const [delModal, setDelModal] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [toast, setToast]     = useState(null)

  // Lista con filtros
  const [lista, setLista]         = useState(null)
  const [loadingList, setLoadingList] = useState(false)
  const [filters, setFilters]     = useState({ cuenta: '', tipo: '', estado_alerta: '', page: 0, size: 20 })
  const [tab, setTab]             = useState('buscar') // 'buscar' | 'lista'

  const buscar = async (e) => {
    e.preventDefault()
    if (!id.trim()) return
    setLoading(true)
    setTxn(null)
    try {
      const r = await obtenerTransaccion(id.trim())
      setTxn(r)
    } catch (e) {
      setToast({ msg: e.message, type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const handleEliminar = async () => {
    setDeleting(true)
    try {
      await eliminarTransaccion(txn.id_transaccion)
      setToast({ msg: `Transacción ${txn.id_transaccion} marcada como eliminada (lógico)`, type: 'info' })
      setDelModal(false)
      setTxn(t => ({ ...t, deleted: true }))
    } catch (e) {
      setToast({ msg: e.message, type: 'error' })
    } finally {
      setDeleting(false)
    }
  }

  const cargarLista = async (overrideFilters) => {
    setLoadingList(true)
    try {
      const r = await listarTransacciones(overrideFilters ?? filters)
      setLista(r)
    } catch (e) {
      setToast({ msg: e.message, type: 'error' })
    } finally {
      setLoadingList(false)
    }
  }

  // FIX: no usar setTimeout — pasar los filtros nuevos directamente
  // para evitar stale closure (setFilters es async y cargarLista capturaría
  // el filters viejo del closure antes de que React aplique el cambio)
  const changePage = (delta) => {
    const newFilters = { ...filters, page: filters.page + delta }
    setFilters(newFilters)
    cargarLista(newFilters)
  }

  return (
    <div>
      {toast && <Toast message={toast.msg} type={toast.type} onClose={() => setToast(null)} />}
      {delModal && (
        <ConfirmModal
          title="¿Eliminar transacción?"
          body={`Se marcará ${txn?.id_transaccion} como eliminada lógicamente (deleted=true en disco). El nodo permanece en el KD-tree.`}
          confirmLabel="Eliminar (lógico)"
          confirmClass="btn-danger"
          onConfirm={handleEliminar}
          onCancel={() => setDelModal(false)}
        />
      )}

      <div className="page-header">
        <div>
          <h1 className="page-title">Consulta de transacciones</h1>
          <p className="page-subtitle">READ + DELETE — Búsqueda O(1) por Hash Table · Eliminación lógica</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="tabs">
        <button className={`tab ${tab === 'buscar' ? 'active' : ''}`} onClick={() => setTab('buscar')}>
          Buscar por ID
        </button>
        <button className={`tab ${tab === 'lista' ? 'active' : ''}`} onClick={() => { setTab('lista'); if (!lista) cargarLista() }}>
          Listar todas
        </button>
      </div>

      {tab === 'buscar' && (
        <div>
          <form onSubmit={buscar} style={{ marginBottom: 20 }}>
            <div style={{ display: 'flex', gap: 10 }}>
              <input
                className="input"
                value={id}
                onChange={e => setId(e.target.value)}
                placeholder="TXN-000042"
                style={{ flex: 1 }}
              />
              <button className="btn btn-primary" type="submit" disabled={loading || !id.trim()}>
                {loading ? <span className="spinner" style={{ width: 14, height: 14 }} /> : '⌕ Buscar'}
              </button>
            </div>
          </form>

          {txn && (
            <div className="card">
              {/* Header */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20 }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
                    <span style={{ fontFamily: 'monospace', fontSize: 18, fontWeight: 700, color: 'var(--accent)' }}>
                      {txn.id_transaccion}
                    </span>
                    <AlertBadge estado={txn.estado_alerta} />
                    {txn.deleted && <span className="badge badge-falso">ELIMINADA</span>}
                  </div>
                  <p style={{ fontSize: 13, color: 'var(--text-secondary)' }}>Cuenta: {txn.num_cuenta}</p>
                </div>
                {!txn.deleted && (
                  <button className="btn btn-danger btn-sm" onClick={() => setDelModal(true)}>
                    ✕ Eliminar (lógico)
                  </button>
                )}
              </div>

              {/* Datos */}
              <div className="grid-3" style={{ marginBottom: 20 }}>
                {[
                  { label: 'Monto',        val: fmtCOP(txn.monto) },
                  { label: 'Tipo',         val: txn.tipo },
                  { label: 'Timestamp',    val: fmtDate(txn.timestamp) },
                  { label: 'Estado alerta', val: txn.estado_alerta_nombre },
                  { label: 'Offset disco', val: `${txn.offset_disco} bytes` },
                  { label: 'Deleted flag', val: txn.deleted ? 'true' : 'false' },
                ].map(({ label, val }) => (
                  <div key={label} style={{
                    background: 'var(--bg-input)', borderRadius: 8, padding: '12px 14px',
                    border: '1px solid var(--border)',
                  }}>
                    <div style={{ fontSize: 11, color: 'var(--text-tertiary)', marginBottom: 4 }}>{label}</div>
                    <div style={{ fontFamily: 'monospace', fontWeight: 600, fontSize: 13 }}>{val}</div>
                  </div>
                ))}
              </div>

              {/* Vector R⁵ */}
              <p className="section-title">Vector R⁵ — posición en el espacio KD-Tree</p>
              <DimensionVector dimensiones={txn.dimensiones} />

              {txn.deleted && (
                <div style={{
                  marginTop: 14, padding: '10px 14px', borderRadius: 8,
                  background: 'var(--falso-bg)', border: '1px solid rgba(107,114,128,.3)',
                  fontSize: 13, color: 'var(--falso)',
                }}>
                  ○ Esta transacción está marcada como eliminada. El nodo permanece en el KD-tree con deleted=true y es filtrado de los resultados KNN.
                </div>
              )}
            </div>
          )}

          {!txn && !loading && (
            <div className="empty-state">
              <span className="empty-icon">⌕</span>
              <span className="empty-text">Ingresa un ID para buscar por Hash Table (O(1))</span>
            </div>
          )}
        </div>
      )}

      {tab === 'lista' && (
        <div>
          {/* Filtros */}
          <div className="card" style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'flex-end' }}>
              <div className="input-group" style={{ flex: 1, minWidth: 140 }}>
                <label className="label">Cuenta</label>
                <input className="input" value={filters.cuenta}
                  onChange={e => setFilters(f => ({ ...f, cuenta: e.target.value }))}
                  placeholder="CTA-001928" />
              </div>
              <div className="input-group" style={{ width: 150 }}>
                <label className="label">Tipo</label>
                <select className="select input" value={filters.tipo}
                  onChange={e => setFilters(f => ({ ...f, tipo: e.target.value }))}>
                  <option value="">Todos</option>
                  <option>RETIRO</option>
                  <option>DEPOSITO</option>
                  <option>TRANSFERENCIA</option>
                </select>
              </div>
              <div className="input-group" style={{ width: 160 }}>
                <label className="label">Estado alerta</label>
                <select className="select input" value={filters.estado_alerta}
                  onChange={e => setFilters(f => ({ ...f, estado_alerta: e.target.value }))}>
                  <option value="">Todos</option>
                  <option value="0">Normal</option>
                  <option value="1">Media</option>
                  <option value="2">Alta</option>
                  <option value="3">Confirmado fraude</option>
                  <option value="4">Falso positivo</option>
                </select>
              </div>
              <button className="btn btn-primary" onClick={cargarLista} disabled={loadingList}>
                {loadingList ? <span className="spinner" style={{ width: 14, height: 14 }} /> : 'Filtrar'}
              </button>
            </div>
          </div>

          {loadingList ? (
            <div style={{ display: 'flex', justifyContent: 'center', padding: 40 }}>
              <div className="spinner" style={{ width: 28, height: 28 }} />
            </div>
          ) : lista ? (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
                <p style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                  {lista.total_elementos?.toLocaleString()} registros · Página {lista.pagina_actual + 1} de {lista.total_paginas}
                </p>
                <div style={{ display: 'flex', gap: 8 }}>
                  <button className="btn btn-ghost btn-sm" disabled={filters.page === 0} onClick={() => changePage(-1)}>← Anterior</button>
                  <button className="btn btn-ghost btn-sm" disabled={filters.page >= (lista.total_paginas - 1)} onClick={() => changePage(1)}>Siguiente →</button>
                </div>
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
                      <th>Deleted</th>
                    </tr>
                  </thead>
                  <tbody>
                    {lista.transacciones?.map(t => (
                      <tr key={t.id_transaccion}>
                        <td><span className="font-mono" style={{ color: 'var(--accent)', fontSize: 12 }}>{t.id_transaccion}</span></td>
                        <td style={{ color: 'var(--text-secondary)' }}>{t.num_cuenta}</td>
                        <td style={{ fontWeight: 600 }}>{fmtCOP(t.monto)}</td>
                        <td><span className="badge" style={{ background: 'var(--bg-input)', color: 'var(--text-secondary)' }}>{t.tipo}</span></td>
                        <td style={{ fontSize: 12, color: 'var(--text-secondary)' }}>{fmtDate(t.timestamp)}</td>
                        <td><AlertBadge estado={t.estado_alerta} /></td>
                        <td>
                          <span style={{ fontSize: 12, color: t.deleted ? 'var(--falso)' : 'var(--normal)' }}>
                            {t.deleted ? 'true' : 'false'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          ) : null}
        </div>
      )}
    </div>
  )
}
