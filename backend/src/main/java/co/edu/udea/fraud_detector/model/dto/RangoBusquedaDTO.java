package co.edu.udea.fraud_detector.model.dto;

/**
 * Hipercubo 5D para POST /api/v1/deteccion/rango.
 * Acepta campos planos (d1_monto_min, d1_monto_max, ...) que el frontend envía.
 * Los null/ausentes se reemplazan por -∞ / +∞ en el servicio.
 */
public class RangoBusquedaDTO {
    // D1 — Monto normalizado [0.0 - 1.0]
    public Double d1MontoMin;
    public Double d1MontoMax;
    // D2 — Hora del día [0.0 - 23.99]
    public Double d2HoraMin;
    public Double d2HoraMax;
    // D3 — Frecuencia últimas 24h
    public Double d3FrecuenciaMin;
    public Double d3FrecuenciaMax;
    // D4 — Tipo: Retiro=0, Depósito=0.5, Transferencia=1
    public Double d4TipoMin;
    public Double d4TipoMax;
    // D5 — Desviación estándar
    public Double d5DesviacionMin;
    public Double d5DesviacionMax;

    /** Convierte a arrays [min[5], max[5]] que usa internamente el KD-tree. */
    public double[] toMinArray() {
        return new double[]{
            d1MontoMin    != null ? d1MontoMin    : Double.NEGATIVE_INFINITY,
            d2HoraMin     != null ? d2HoraMin     : Double.NEGATIVE_INFINITY,
            d3FrecuenciaMin != null ? d3FrecuenciaMin : Double.NEGATIVE_INFINITY,
            d4TipoMin     != null ? d4TipoMin     : Double.NEGATIVE_INFINITY,
            d5DesviacionMin != null ? d5DesviacionMin : Double.NEGATIVE_INFINITY,
        };
    }

    public double[] toMaxArray() {
        return new double[]{
            d1MontoMax    != null ? d1MontoMax    : Double.POSITIVE_INFINITY,
            d2HoraMax     != null ? d2HoraMax     : Double.POSITIVE_INFINITY,
            d3FrecuenciaMax != null ? d3FrecuenciaMax : Double.POSITIVE_INFINITY,
            d4TipoMax     != null ? d4TipoMax     : Double.POSITIVE_INFINITY,
            d5DesviacionMax != null ? d5DesviacionMax : Double.POSITIVE_INFINITY,
        };
    }
}
