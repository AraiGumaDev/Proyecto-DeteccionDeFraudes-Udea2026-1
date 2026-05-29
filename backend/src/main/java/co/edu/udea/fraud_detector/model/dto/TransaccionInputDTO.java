package co.edu.udea.fraud_detector.model.dto;

/**
 * Cuerpo de la petición POST /api/v1/transacciones.
 * El campo tipo acepta: "RETIRO", "DEPOSITO", "TRANSFERENCIA".
 */
public class TransaccionInputDTO {
    public String idTransaccion;
    public String numCuenta;
    public double monto;
    public long   timestamp;   // Unix epoch en segundos
    public String tipo;
}
