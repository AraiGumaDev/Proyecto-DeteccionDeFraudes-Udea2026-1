package co.edu.udea.fraud_detector.model.dto;

/**
 * Cuerpo del PUT /api/v1/transacciones/{id}.
 * Solo se pueden modificar monto, tipo y timestamp.
 * numCuenta e idTransaccion no se actualizan.
 */
public class TransaccionUpdateDTO {
    public double  monto;
    public String  tipo;
    public Long    timestamp;  // null = mantener el original
}
