import { get, patch } from './client'

/** GET /alertas — listar alertas activas (MEDIA=1, ALTA=2) */
export const listarAlertas = (nivel_minimo = 1) =>
  get('/alertas', { nivel_minimo })

/** PATCH /alertas/:id/confirmar — confirmar fraude */
export const confirmarFraude = (id) =>
  patch(`/alertas/${encodeURIComponent(id)}/confirmar`)

/** PATCH /alertas/:id/descartar — marcar falso positivo */
export const descartarAlerta = (id) =>
  patch(`/alertas/${encodeURIComponent(id)}/descartar`)
