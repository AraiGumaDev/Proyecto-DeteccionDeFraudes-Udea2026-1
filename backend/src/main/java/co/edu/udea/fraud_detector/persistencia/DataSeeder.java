package co.edu.udea.fraud_detector.persistencia;

import co.edu.udea.fraud_detector.model.enums.EstadoAlerta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

// carga 30 transacciones de ejemplo cuando el archivo está vacío:
// 20 normales (horario laboral, montos bajos), 8 fraudes confirmados (madrugada, montos altos)
// y 2 alertas ALTA sin confirmar; dimensiones precalculadas para consistencia
@Slf4j
@Component
public class DataSeeder {

    // epochs base: D1=01 Ene 2024, D2=02 Ene, D3=03 Ene (en segundos UTC)
    private static final long D1 = 1704067200L;
    private static final long D2 = 1704153600L;
    private static final long D3 = 1704240000L;

    private final FileManager fileManager;

    public DataSeeder(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    public void seed() throws IOException {
        log.info("Cargando datos de ejemplo...");

        // normales — Día 1 (01 Ene 2024, horario laboral)
        //         id        cuenta   monto   timestamp       d1      d2    d3   d4    d5    estado
        reg("TX001", "CTA001",  120.0, D1+36000, 0.002, 10.0, 1.0, 0.5, 1.80, EstadoAlerta.NORMAL);
        reg("TX002", "CTA001",  350.0, D1+50400, 0.007, 14.0, 1.0, 0.0, 0.50, EstadoAlerta.NORMAL);
        reg("TX003", "CTA002",  200.0, D1+32400, 0.004,  9.0, 1.0, 0.5, 1.60, EstadoAlerta.NORMAL);
        reg("TX004", "CTA002",  450.0, D1+54000, 0.009, 15.0, 1.0, 1.0, 0.07, EstadoAlerta.NORMAL);
        reg("TX005", "CTA003",  800.0, D1+39600, 0.016, 11.0, 1.0, 0.5, 1.20, EstadoAlerta.NORMAL);
        reg("TX006", "CTA003", 1200.0, D1+57600, 0.024, 16.0, 1.0, 0.0, 0.40, EstadoAlerta.NORMAL);
        reg("TX007", "CTA004",  300.0, D1+46800, 0.006, 13.0, 1.0, 0.5, 0.75, EstadoAlerta.NORMAL);
        reg("TX008", "CTA005",  650.0, D1+36000, 0.013, 10.0, 1.0, 1.0, 0.58, EstadoAlerta.NORMAL);
        reg("TX009", "CTA005",  400.0, D1+54000, 0.008, 15.0, 1.0, 0.0, 1.35, EstadoAlerta.NORMAL);
        reg("TX010", "CTA001",  280.0, D1+61200, 0.006, 17.0, 1.0, 0.5, 0.20, EstadoAlerta.NORMAL);

        // normales — Día 2 (02 Ene 2024, horario laboral)
        reg("TX011", "CTA002",  550.0, D2+43200, 0.011, 12.0, 2.0, 0.0, 0.73, EstadoAlerta.NORMAL);
        reg("TX012", "CTA003", 1500.0, D2+50400, 0.030, 14.0, 2.0, 1.0, 0.20, EstadoAlerta.NORMAL);
        reg("TX013", "CTA005",  750.0, D2+39600, 0.015, 11.0, 2.0, 0.5, 1.35, EstadoAlerta.NORMAL);
        reg("TX014", "CTA001",  180.0, D2+46800, 0.003, 13.0, 2.0, 0.0, 1.20, EstadoAlerta.NORMAL);
        reg("TX015", "CTA003", 2200.0, D2+57600, 0.044, 16.0, 1.0, 0.5, 1.60, EstadoAlerta.NORMAL);
        reg("TX016", "CTA002",  320.0, D2+32400, 0.005,  9.0, 1.0, 0.5, 0.80, EstadoAlerta.NORMAL);
        reg("TX017", "CTA005",  500.0, D2+50400, 0.010, 14.0, 1.0, 1.0, 0.58, EstadoAlerta.NORMAL);
        reg("TX018", "CTA001",  250.0, D2+36000, 0.005, 10.0, 1.0, 0.5, 0.50, EstadoAlerta.NORMAL);
        reg("TX019", "CTA004",  450.0, D2+54000, 0.009, 15.0, 1.0, 0.0, 0.75, EstadoAlerta.NORMAL);
        reg("TX020", "CTA002",  700.0, D2+46800, 0.014, 13.0, 2.0, 1.0, 1.73, EstadoAlerta.NORMAL);

        // fraudes confirmados — Día 3 madrugada (01h-04h): monto alto, alta frecuencia, alta desviación
        reg("TXF001", "CTA004", 45000.0, D3+ 7200, 0.900, 2.0,  8.0, 0.0, 7.50, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF002", "CTA004", 38000.0, D3+10800, 0.760, 3.0,  9.0, 1.0, 7.20, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF003", "CTA004", 42000.0, D3+ 3600, 0.840, 1.0,  7.0, 0.0, 7.80, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF004", "CTA001", 35000.0, D3+14400, 0.700, 4.0,  6.0, 1.0, 6.50, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF005", "CTA003", 48000.0, D3+ 7200, 0.960, 2.0, 10.0, 0.0, 8.00, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF006", "CTA005", 40000.0, D3+10800, 0.800, 3.0,  8.0, 1.0, 7.30, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF007", "CTA002", 32000.0, D3+14400, 0.640, 4.0,  7.0, 0.0, 6.80, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF008", "CTA004", 50000.0, D3+ 3600, 1.000, 1.0,  9.0, 1.0, 8.50, EstadoAlerta.CONFIRMADO_FRAUDE);

        // alertas ALTA sin confirmar: perfil sospechoso, pendiente de revisión por el analista
        reg("TXF009", "CTA001", 29000.0, D3+10800, 0.580, 3.0, 5.0, 0.0, 5.90, EstadoAlerta.ALTA);
        reg("TXF010", "CTA003", 44000.0, D3+ 7200, 0.880, 2.0, 6.0, 1.0, 6.20, EstadoAlerta.ALTA);

        log.info("Datos de ejemplo cargados: {} registros (20 normales, 8 fraudes confirmados, 2 alertas altas)",
                fileManager.contarRegistros());
    }

    private void reg(String id, String cuenta, double monto, long ts,
                     double d1, double d2, double d3, double d4, double d5,
                     EstadoAlerta estado) throws IOException {
        RegistroTransaccion r = new RegistroTransaccion();
        r.setIdTransaccion(id);
        r.setNumCuenta(cuenta);
        r.monto         = monto;
        r.timestamp     = ts;
        r.d1_monto_norm = d1;
        r.d2_hora       = d2;
        r.d3_frecuencia = d3;
        r.d4_tipo       = d4;
        r.d5_desviacion = d5;
        r.estado_alerta = (byte) estado.codigo;
        r.deleted       = 0;
        fileManager.append(r);
    }
}
