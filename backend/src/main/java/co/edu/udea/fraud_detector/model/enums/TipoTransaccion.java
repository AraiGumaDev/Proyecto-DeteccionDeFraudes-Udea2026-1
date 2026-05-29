package co.edu.udea.fraud_detector.model.enums;

public enum TipoTransaccion {
    RETIRO        (0.0),
    DEPOSITO      (0.5),
    TRANSFERENCIA (1.0);

    public final double valorDimension;

    TipoTransaccion(double valorDimension) { this.valorDimension = valorDimension; }

    public static TipoTransaccion fromValor(double valor) {
        for (TipoTransaccion t : values()) {
            if (Math.abs(t.valorDimension - valor) < 0.01) return t;
        }
        throw new IllegalArgumentException("Valor de tipo inválido: " + valor);
    }
}
