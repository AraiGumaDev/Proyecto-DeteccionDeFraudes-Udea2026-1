package co.edu.udea.fraud_detector.model.dto;

import java.util.Map;

/**
 * Respuesta de GET /api/v1/sistema/estadisticas.
 * Estructura anidada que coincide con lo que espera el frontend React.
 */
public class EstadisticasDTO {

    public int totalTransacciones;   // total activas
    public int totalEliminadas;

    public Map<String, Long> distribucionAlertas;  // keys: normal, media, alta, confirmado_fraude, falso_positivo

    public HashTableStats hashTable;
    public KDTreeStats    kdtree;

    public static class HashTableStats {
        public int    bucketsTotales;
        public double factorCarga;
        public int    colisiones;   // entradas que comparten bucket (size - bucketsOcupados)
    }

    public static class KDTreeStats {
        public int totalNodos;
        public int profundidadMax;
    }
}
