import { get } from './client'

/** GET /sistema/estadisticas — totales y distribución de alertas */
export const obtenerEstadisticas = () => get('/sistema/estadisticas')

/** GET /sistema/salud — health check del sistema */
export const obtenerSalud = () => get('/sistema/salud')
