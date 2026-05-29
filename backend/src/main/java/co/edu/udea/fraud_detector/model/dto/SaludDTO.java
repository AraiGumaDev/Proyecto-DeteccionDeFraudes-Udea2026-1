package co.edu.udea.fraud_detector.model.dto;

/**
 * Respuesta de GET /api/v1/sistema/salud.
 */
public class SaludDTO {
    public String estado;           // "OK" | "DEGRADADO"
    public String archivoDatos;     // "OK" | mensaje de error
    public String hashTable;        // "OK (N entradas)"
    public String kdTree;           // "OK (N nodos activos)"
    public int    registrosTotales;
}
