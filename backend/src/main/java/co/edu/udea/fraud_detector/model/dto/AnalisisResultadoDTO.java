package co.edu.udea.fraud_detector.model.dto;

/** Respuesta de POST /api/v1/deteccion/analizar/:id */
public class AnalisisResultadoDTO {
    public String idTransaccion;
    public int    kUtilizado;
    public long   vecinosFraude;
    public int    estadoAlertaAsignado;
    public String estadoAlertaNombre;
}
