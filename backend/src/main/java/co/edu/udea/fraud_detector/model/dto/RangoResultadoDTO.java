package co.edu.udea.fraud_detector.model.dto;

import java.util.List;

/** Respuesta de POST /api/v1/deteccion/rango */
public class RangoResultadoDTO {
    public int                          total;
    public List<TransaccionCompletaDTO> transacciones;
}
