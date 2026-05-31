package co.edu.udea.fraud_detector.model.dto;

import java.util.List;

/** Respuesta de GET /api/v1/deteccion/vecinos/:id */
public class VecinosResultadoDTO {
    public String        idTransaccionConsulta;
    public int           k;
    public List<Vecino>  vecinos;

    public static class Vecino {
        public int    posicion;
        public String idTransaccion;
        public double monto;
        public String tipo;
        public int    estadoAlerta;
        public String estadoAlertaNombre;
        public double distanciaEuclidiana;
    }
}
