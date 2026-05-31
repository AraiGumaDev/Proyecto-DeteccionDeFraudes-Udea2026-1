package co.edu.udea.fraud_detector.model.dto;

import co.edu.udea.fraud_detector.model.enums.EstadoAlerta;
import co.edu.udea.fraud_detector.model.enums.TipoTransaccion;
import co.edu.udea.fraud_detector.persistencia.RegistroTransaccion;

/**
 * Respuesta completa de una transacción.
 * Los campos se serializan en snake_case por la config global de Jackson.
 */
public class TransaccionCompletaDTO {

    public int     offsetDisco;          // posición en bytes = numRegistro * 128
    public String  idTransaccion;
    public String  numCuenta;
    public double  monto;
    public long    timestamp;
    public String  tipo;
    public int     estadoAlerta;         // código numérico: 0=NORMAL 1=MEDIA 2=ALTA 3=CONFIRMADO_FRAUDE 4=FALSO_POSITIVO
    public String  estadoAlertaNombre;   // nombre del enum para mostrar
    public boolean deleted;
    public Dimensiones dimensiones;

    public static class Dimensiones {
        public double d1MontoNorm;
        public double d2Hora;
        public double d3Frecuencia;
        public double d4Tipo;
        public double d5Desviacion;
    }

    public static TransaccionCompletaDTO from(RegistroTransaccion r) {
        TransaccionCompletaDTO dto = new TransaccionCompletaDTO();
        dto.offsetDisco        = r.numRegistro * RegistroTransaccion.RECORD_SIZE;
        dto.idTransaccion      = r.getIdTransaccion();
        dto.numCuenta          = r.getNumCuenta();
        dto.monto              = r.monto;
        dto.timestamp          = r.timestamp;
        dto.tipo               = TipoTransaccion.fromValor(r.d4_tipo).name();
        int codigo             = r.estado_alerta & 0xFF;
        dto.estadoAlerta       = codigo;
        dto.estadoAlertaNombre = EstadoAlerta.fromCodigo(codigo).name();
        dto.deleted            = !r.isActivo();

        Dimensiones dims  = new Dimensiones();
        dims.d1MontoNorm  = r.d1_monto_norm;
        dims.d2Hora       = r.d2_hora;
        dims.d3Frecuencia = r.d3_frecuencia;
        dims.d4Tipo       = r.d4_tipo;
        dims.d5Desviacion = r.d5_desviacion;
        dto.dimensiones   = dims;
        return dto;
    }
}
