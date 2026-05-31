import { useState, useRef, useEffect } from 'react'
import { registrarTransaccion } from '../api/transacciones'
import DimensionVector from '../components/DimensionVector'
import AlertBadge from '../components/AlertBadge'
import Toast from '../components/Toast'

const TIPOS = ['RETIRO', 'DEPOSITO', 'TRANSFERENCIA']

const INIT = {
  id_transaccion: '',
  num_cuenta: '',
  monto: '',
  fecha: '',
  hora: '',
  tipo: 'RETIRO',
}

const pad2 = (n) => String(n).padStart(2, '0')

const nowFields = () => {
  const d = new Date()
  return {
    fecha: `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`,
    hora: `${pad2(d.getHours())}:${pad2(d.getMinutes())}`,
  }
}

const toTimestamp = (fecha, hora) =>
  Math.floor(new Date(`${fecha}T${hora}`).getTime() / 1000)

const validate = (f) => {
  const errs = {}
  if (!f.id_transaccion.trim()) errs.id_transaccion = 'Requerido'
  else if (f.id_transaccion.length > 19) errs.id_transaccion = 'Máx. 19 caracteres'
  if (!f.num_cuenta.trim()) errs.num_cuenta = 'Requerido'
  else if (f.num_cuenta.length > 19) errs.num_cuenta = 'Máx. 19 caracteres'
  if (!f.monto) errs.monto = 'Requerido'
  else if (parseFloat(f.monto) <= 0) errs.monto = 'Debe ser mayor a 0'
  if (!f.fecha) errs.fecha = 'Requerido'
  if (!f.hora) errs.hora = 'Requerido'
  else if (f.fecha && Number.isNaN(toTimestamp(f.fecha, f.hora))) errs.hora = 'Fecha u hora inválida'
  if (!f.tipo) errs.tipo = 'Requerido'
  return errs
}

function Field({ label, children }) {
  return (
    <div className="input-group">
      <label className="label">{label}</label>
      {children}
    </div>
  )
}

export default function RegistrarTransaccion() {
  const [fields, setFields] = useState(INIT)
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [result, setResult]   = useState(null)
  const [toast, setToast]     = useState(null)

  const setNow = () => {
    setFields(f => ({ ...f, ...nowFields() }))
    setErrors(er => ({ ...er, fecha: null, hora: null }))
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setFields(f => ({ ...f, [name]: value }))
    if (errors[name]) setErrors(er => ({ ...er, [name]: null }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errs = validate(fields)
    if (Object.keys(errs).length) { setErrors(errs); return }

    setLoading(true)
    setResult(null)
    try {
      const payload = {
        id_transaccion: fields.id_transaccion.trim(),
        num_cuenta:     fields.num_cuenta.trim(),
        monto:          parseFloat(fields.monto),
        timestamp:      toTimestamp(fields.fecha, fields.hora),
        tipo:           fields.tipo,
      }
      const res = await registrarTransaccion(payload)
      setResult(res)
      setToast({ msg: `Transacción ${res.id_transaccion} registrada. Nivel: ${res.estado_alerta_nombre}`, type: 'success' })
      setFields(INIT)
    } catch (e) {
      setToast({ msg: e.message, type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ maxWidth: 760 }}>
      {toast && <Toast message={toast.msg} type={toast.type} onClose={() => setToast(null)} />}

      <div className="page-header">
        <div>
          <h1 className="page-title">Registrar transacción</h1>
          <p className="page-subtitle">CREATE — El sistema calcula el vector R⁵ y ejecuta KNN automáticamente</p>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <form onSubmit={handleSubmit}>
          <div className="grid-2" style={{ marginBottom: 16 }}>
            <Field label="ID de transacción">
              <input
                className={`input ${errors.id_transaccion ? 'border-error' : ''}`}
                style={errors.id_transaccion ? { borderColor: 'var(--alta)' } : {}}
                type="text" name="id_transaccion"
                value={fields.id_transaccion} onChange={handleChange}
                placeholder="TXN-000001"
              />
              {errors.id_transaccion && <span className="field-error">⚠ {errors.id_transaccion}</span>}
            </Field>

            <Field label="Número de cuenta">
              <input
                className="input"
                style={errors.num_cuenta ? { borderColor: 'var(--alta)' } : {}}
                type="text" name="num_cuenta"
                value={fields.num_cuenta} onChange={handleChange}
                placeholder="CTA-001928"
              />
              {errors.num_cuenta && <span className="field-error">⚠ {errors.num_cuenta}</span>}
            </Field>
          </div>

          <div className="grid-2" style={{ marginBottom: 16 }}>
            <div className="input-group">
              <label className="label">Monto (COP)</label>
              <input
                className="input"
                style={errors.monto ? { borderColor: 'var(--alta)' } : {}}
                type="number" name="monto" min="0.01" step="0.01"
                value={fields.monto} onChange={handleChange}
                placeholder="4500000.00"
              />
              {errors.monto && <span className="field-error">⚠ {errors.monto}</span>}
            </div>

            <div className="input-group">
              <label className="label">Tipo de transacción</label>
              <select className="select input" name="tipo" value={fields.tipo} onChange={handleChange}>
                {TIPOS.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
          </div>

          <div className="input-group" style={{ marginBottom: 20 }}>
            <label className="label">Fecha y hora de la transacción</label>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr auto', gap: 8, alignItems: 'start' }}>
              <div className="input-group">
                <label className="label" style={{ fontSize: 11 }}>Fecha</label>
                <input
                  className="input input-datetime"
                  style={errors.fecha ? { borderColor: 'var(--alta)' } : {}}
                  type="date"
                  name="fecha"
                  value={fields.fecha}
                  onChange={handleChange}
                />
                {errors.fecha && <span className="field-error">⚠ {errors.fecha}</span>}
              </div>
              <div className="input-group">
                <label className="label" style={{ fontSize: 11 }}>Hora</label>
                <input
                  className="input input-datetime"
                  style={errors.hora ? { borderColor: 'var(--alta)' } : {}}
                  type="time"
                  name="hora"
                  step="60"
                  value={fields.hora}
                  onChange={handleChange}
                />
                {errors.hora && <span className="field-error">⚠ {errors.hora}</span>}
              </div>
              <button type="button" className="btn btn-ghost" onClick={setNow} style={{ whiteSpace: 'nowrap', marginTop: 22 }}>
                Ahora
              </button>
            </div>
            {fields.fecha && fields.hora && !errors.fecha && !errors.hora && (
              <span style={{ fontSize: 11, color: 'var(--text-tertiary)' }}>
                → {new Date(`${fields.fecha}T${fields.hora}`).toLocaleString('es-CO', {
                  dateStyle: 'full',
                  timeStyle: 'short',
                })}
              </span>
            )}
          </div>

          <button type="submit" className="btn btn-primary" disabled={loading} style={{ width: '100%', justifyContent: 'center' }}>
            {loading
              ? <><span className="spinner" style={{ width: 16, height: 16 }} /> Registrando y ejecutando KNN...</>
              : '＋ Registrar transacción y ejecutar KNN'}
          </button>
        </form>
      </div>

      {/* Resultado */}
      {result && (
        <div className="card" style={{ borderColor: result.estado_alerta >= 2 ? 'var(--alta)' : result.estado_alerta === 1 ? 'var(--media)' : 'var(--normal)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
            <p style={{ fontWeight: 600, fontSize: 15 }}>✓ Transacción registrada</p>
            <AlertBadge estado={result.estado_alerta} />
          </div>

          <div className="grid-2" style={{ marginBottom: 16 }}>
            {[
              { label: 'ID', val: result.id_transaccion },
              { label: 'Cuenta', val: result.num_cuenta },
              { label: 'Monto', val: new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP' }).format(result.monto) },
              { label: 'Tipo', val: result.tipo },
              { label: 'Fecha y hora', val: new Date(result.timestamp * 1000).toLocaleString('es-CO') },
              { label: 'Offset disco', val: `${result.offset_disco} bytes` },
            ].map(({ label, val }) => (
              <div key={label} style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <span style={{ fontSize: 11, color: 'var(--text-tertiary)' }}>{label}</span>
                <span style={{ fontSize: 13, fontFamily: 'monospace', color: 'var(--text-primary)' }}>{val}</span>
              </div>
            ))}
          </div>

          <p className="section-title">Vector R⁵ calculado</p>
          <DimensionVector dimensiones={result.dimensiones} />

          {result.estado_alerta >= 1 && (
            <div style={{
              marginTop: 14, padding: '10px 14px', borderRadius: 8,
              background: result.estado_alerta >= 2 ? 'var(--alta-bg)' : 'var(--media-bg)',
              border: `1px solid ${result.estado_alerta >= 2 ? 'rgba(239,68,68,.3)' : 'rgba(245,158,11,.3)'}`,
              fontSize: 13, color: result.estado_alerta >= 2 ? 'var(--alta)' : 'var(--media)',
            }}>
              ⚠ Esta transacción ha sido marcada como alerta {result.estado_alerta_nombre}. Ve a <strong>Alertas activas</strong> para revisarla.
            </div>
          )}
        </div>
      )}
    </div>
  )
}
