package co.edu.udea.fraud_detector.model.enums;

public enum EstadoAlerta {
    NORMAL            (0),
    MEDIA             (1),
    ALTA              (2),
    CONFIRMADO_FRAUDE (3),
    FALSO_POSITIVO    (4);

    public final int codigo;

    EstadoAlerta(int codigo) { this.codigo = codigo; }

    public static EstadoAlerta fromCodigo(int codigo) {
        for (EstadoAlerta e : values()) {
            if (e.codigo == codigo) return e;
        }
        throw new IllegalArgumentException("Código de estado inválido: " + codigo);
    }
}
