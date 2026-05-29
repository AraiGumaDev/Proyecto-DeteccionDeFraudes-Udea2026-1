package co.edu.udea.fraud_detector.persistencia;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// Registro de 128 bytes que se guarda en transacciones.dat
// offset en disco = numRegistro * 128
// Layout: [0-19] id_transaccion | [20-39] num_cuenta | [40-47] monto | [48-55] timestamp
//         [56-95] d1..d5 (5 doubles de 8 bytes c/u) | [96] estado_alerta | [97] deleted | [98-127] padding
public class RegistroTransaccion {

    public static final int RECORD_SIZE          = 128;
    public static final int OFFSET_ESTADO_ALERTA = 96;
    public static final int OFFSET_DELETED       = 97;

    public byte[] id_transaccion = new byte[20];  // offset   0
    public byte[] num_cuenta     = new byte[20];  // offset  20
    public double monto;                           // offset  40
    public long   timestamp;                       // offset  48
    public double d1_monto_norm;                  // offset  56
    public double d2_hora;                        // offset  64
    public double d3_frecuencia;                  // offset  72
    public double d4_tipo;                        // offset  80
    public double d5_desviacion;                  // offset  88
    public byte   estado_alerta;                  // offset  96
    public byte   deleted;                        // offset  97
    public byte[] padding        = new byte[30];  // offset  98

    // no se guarda en disco, se asigna al leer según la posición en el archivo
    public transient int numRegistro = -1;

    public byte[] serializar() {
        ByteBuffer buf = ByteBuffer.allocate(RECORD_SIZE);
        buf.put(id_transaccion);      // 20
        buf.put(num_cuenta);          // 20
        buf.putDouble(monto);         //  8
        buf.putLong(timestamp);       //  8
        buf.putDouble(d1_monto_norm); //  8
        buf.putDouble(d2_hora);       //  8
        buf.putDouble(d3_frecuencia); //  8
        buf.putDouble(d4_tipo);       //  8
        buf.putDouble(d5_desviacion); //  8
        buf.put(estado_alerta);       //  1
        buf.put(deleted);             //  1
        buf.put(padding);             // 30
        byte[] result = buf.array();
        assert result.length == RECORD_SIZE : "Serialización produjo " + result.length + " bytes en lugar de 128";
        return result;
    }

    public static RegistroTransaccion deserializar(byte[] data) {
        if (data.length != RECORD_SIZE) {
            throw new IllegalArgumentException(
                "Registro debe ser " + RECORD_SIZE + " bytes, recibido: " + data.length);
        }
        ByteBuffer buf = ByteBuffer.wrap(data);
        RegistroTransaccion r = new RegistroTransaccion();
        buf.get(r.id_transaccion);
        buf.get(r.num_cuenta);
        r.monto         = buf.getDouble();
        r.timestamp     = buf.getLong();
        r.d1_monto_norm = buf.getDouble();
        r.d2_hora       = buf.getDouble();
        r.d3_frecuencia = buf.getDouble();
        r.d4_tipo       = buf.getDouble();
        r.d5_desviacion = buf.getDouble();
        r.estado_alerta = buf.get();
        r.deleted       = buf.get();
        buf.get(r.padding);
        return r;
    }

    public String getIdTransaccion() {
        return new String(id_transaccion, StandardCharsets.UTF_8).replace("\0", "").trim();
    }

    public String getNumCuenta() {
        return new String(num_cuenta, StandardCharsets.UTF_8).replace("\0", "").trim();
    }

    // copia el string al byte[], rellenando con \0; max 19 chars para dejar espacio al terminador
    public void setIdTransaccion(String id) {
        Arrays.fill(id_transaccion, (byte) 0);
        byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(bytes, 0, id_transaccion, 0, Math.min(bytes.length, 19));
    }

    public void setNumCuenta(String cuenta) {
        Arrays.fill(num_cuenta, (byte) 0);
        byte[] bytes = cuenta.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(bytes, 0, num_cuenta, 0, Math.min(bytes.length, 19));
    }

    // devuelve el vector de 5 dimensiones para insertar/buscar en el KD-tree
    public double[] getDimensiones() {
        return new double[]{ d1_monto_norm, d2_hora, d3_frecuencia, d4_tipo, d5_desviacion };
    }

    public boolean isActivo() {
        return deleted == 0;
    }
}
