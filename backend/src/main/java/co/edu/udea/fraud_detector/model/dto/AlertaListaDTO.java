package co.edu.udea.fraud_detector.model.dto;

import java.util.List;

/** Respuesta de GET /api/v1/alertas */
public class AlertaListaDTO {
    public int                      totalAlertas;
    public long                     alertasAlta;
    public long                     alertasMedia;
    public List<TransaccionCompletaDTO> transacciones;
}
