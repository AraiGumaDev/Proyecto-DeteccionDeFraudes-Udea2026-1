package co.edu.udea.fraud_detector.model.dto;

import java.util.Map;

/**
 * Respuesta de GET /api/v1/sistema/estadisticas.
 */
public class EstadisticasDTO {

    // Archivo
    public int    totalRegistros;
    public int    registrosActivos;
    public int    registrosEliminados;
    public String archivoRuta;

    // Distribución por estado de alerta (solo activos)
    public Map<String, Long> distribucionEstados;

    // Hash Table
    public int    hashEntradas;
    public int    hashTableSize;
    public double hashLoadFactor;
    public int    hashBucketsOcupados;

    // KD-tree
    public int    kdTreeTotalNodos;
    public int    kdTreeNodosActivos;
}
