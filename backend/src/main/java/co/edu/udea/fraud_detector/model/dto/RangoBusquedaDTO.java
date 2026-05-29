package co.edu.udea.fraud_detector.model.dto;

/**
 * Hipercubo 5D para POST /api/v1/deteccion/rango.
 * Cada array tiene exactamente 5 valores: [d1, d2, d3, d4, d5].
 */
public class RangoBusquedaDTO {
    public double[] min;  // límites inferiores inclusivos
    public double[] max;  // límites superiores inclusivos
}
