package co.edu.udea.fraud_detector.model.dto;

import co.edu.udea.fraud_detector.model.enums.EstadoAlerta;
import co.edu.udea.fraud_detector.model.enums.TipoTransaccion;
import co.edu.udea.fraud_detector.persistencia.RegistroTransaccion;

/**
 * Respuesta completa de una transacción.
 * Incluye las 5 dimensiones para facilitar la depuración y la sustentación.
 */
public class TransaccionCompletaDTO {

    public int    numRegistro;
    public String idTransaccion;
    public String numCuenta;
    public double monto;
    public long   timestamp;
    public String tipo;          // nombre del enum: RETIRO / DEPOSITO / TRANSFERENCIA
    public String estadoAlerta;  // nombre del enum: NORMAL / MEDIA / ALTA / ...

    // Dimensiones del KD-tree
    public double d1MontoNorm;
    public double d2Hora;
    public double d3Frecuencia;
    public double d4Tipo;
    public double d5Desviacion;

    public static TransaccionCompletaDTO from(RegistroTransaccion r) {
        TransaccionCompletaDTO dto = new TransaccionCompletaDTO();
        dto.numRegistro   = r.numRegistro;
        dto.idTransaccion = r.getIdTransaccion();
        dto.numCuenta     = r.getNumCuenta();
        dto.monto         = r.monto;
        dto.timestamp     = r.timestamp;
        dto.tipo          = TipoTransaccion.fromValor(r.d4_tipo).name();
        dto.estadoAlerta  = EstadoAlerta.fromCodigo(r.estado_alerta & 0xFF).name();
        dto.d1MontoNorm   = r.d1_monto_norm;
        dto.d2Hora        = r.d2_hora;
        dto.d3Frecuencia  = r.d3_frecuencia;
        dto.d4Tipo        = r.d4_tipo;
        dto.d5Desviacion  = r.d5_desviacion;
        return dto;
    }
}
