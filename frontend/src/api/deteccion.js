import { get, post } from './client'

/** POST /deteccion/analizar/:id — re-ejecutar KNN sobre transacción existente */
export const analizarTransaccion = (id, k = 5) =>
  post(`/deteccion/analizar/${encodeURIComponent(id)}?k=${k}`)

/** GET /deteccion/vecinos/:id — obtener K vecinos más cercanos */
export const obtenerVecinos = (id, k = 5) =>
  get(`/deteccion/vecinos/${encodeURIComponent(id)}`, { k })

/** POST /deteccion/rango — búsqueda por hipercubo 5D */
export const buscarPorRango = (rango) => post('/deteccion/rango', rango)

/** POST /deteccion/escaneo-masivo — re-analizar todas las pendientes */
export const escaneoMasivo = () => post('/deteccion/escaneo-masivo')
