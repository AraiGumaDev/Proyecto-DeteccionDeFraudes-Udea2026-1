package co.edu.udea.fraud_detector.model.exception;

public class TransaccionNoEncontradaException extends RuntimeException {
    public TransaccionNoEncontradaException(String id) {
        super("Transacción no encontrada: " + id);
    }
}
