const DIMS = [
  { key: 'd1_monto_norm', label: 'D1', name: 'Monto norm.' },
  { key: 'd2_hora',       label: 'D2', name: 'Hora' },
  { key: 'd3_frecuencia', label: 'D3', name: 'Frecuencia' },
  { key: 'd4_tipo',       label: 'D4', name: 'Tipo' },
  { key: 'd5_desviacion', label: 'D5', name: 'Desviación' },
]

export default function DimensionVector({ dimensiones }) {
  if (!dimensiones) return null
  return (
    <div className="dim-vector">
      {DIMS.map(d => (
        <div key={d.key} className="dim-item">
          <span className="dim-label">{d.label}</span>
          <span className="dim-val">
            {dimensiones[d.key] !== undefined
              ? Number(dimensiones[d.key]).toFixed(3)
              : '—'}
          </span>
          <span className="dim-name">{d.name}</span>
        </div>
      ))}
    </div>
  )
}
