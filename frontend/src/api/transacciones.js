import { get, post, del } from './client'

/** POST /transacciones — registrar nueva transacción */
export const registrarTransaccion = (data) => post('/transacciones', data)

/** GET /transacciones — listar todas (con filtros opcionales) */
export const listarTransacciones = ({ cuenta, tipo, estado_alerta, page = 0, size = 20 } = {}) =>
  get('/transacciones', { cuenta, tipo, estado_alerta, page, size })

/** GET /transacciones/:id — consultar por ID (hash table O(1)) */
export const obtenerTransaccion = (id) => get(`/transacciones/${encodeURIComponent(id)}`)

/** DELETE /transacciones/:id — eliminación lógica */
export const eliminarTransaccion = (id) => del(`/transacciones/${encodeURIComponent(id)}`)
