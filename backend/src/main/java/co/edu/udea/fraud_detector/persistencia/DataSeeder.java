package co.edu.udea.fraud_detector.persistencia;

import co.edu.udea.fraud_detector.model.enums.EstadoAlerta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

// carga 105 transacciones de ejemplo cuando el archivo está vacío:
// 65 normales (horario laboral, 2026), 8 MEDIA, 9 ALTA sin confirmar
// y 23 fraudes confirmados en dos olas de ataque (feb y mar 2026)
@Slf4j
@Component
public class DataSeeder {

    // días base en UTC (segundos) — año 2026
    // 2026-01-01 00:00 UTC = 1767225600
    private static final long E1  = 1767657600L; // 2026-01-06 Lun
    private static final long E2  = 1767744000L; // 2026-01-07 Mar
    private static final long E3  = 1767830400L; // 2026-01-08 Mié
    private static final long E4  = 1767916800L; // 2026-01-09 Jue
    private static final long E5  = 1768176000L; // 2026-01-12 Lun
    private static final long E6  = 1768262400L; // 2026-01-13 Mar
    private static final long E7  = 1768348800L; // 2026-01-14 Mié
    private static final long E8  = 1768435200L; // 2026-01-15 Jue
    private static final long F1  = 1769990400L; // 2026-02-02 Lun
    private static final long F2  = 1770076800L; // 2026-02-03 Mar
    private static final long F3  = 1770163200L; // 2026-02-04 Mié
    private static final long F4  = 1770249600L; // 2026-02-05 Jue
    private static final long F5  = 1770595200L; // 2026-02-09 Lun
    private static final long F6  = 1770681600L; // 2026-02-10 Mar
    private static final long FA  = 1771545600L; // 2026-02-20 Vie — ola de fraude 1
    private static final long M1  = 1772409600L; // 2026-03-02 Lun
    private static final long M2  = 1772496000L; // 2026-03-03 Mar
    private static final long M3  = 1772582400L; // 2026-03-04 Mié
    private static final long M4  = 1773014400L; // 2026-03-09 Lun
    private static final long M5  = 1773100800L; // 2026-03-10 Mar
    private static final long MB  = 1773532800L; // 2026-03-15 Dom — ola de fraude 2
    private static final long A1  = 1775433600L; // 2026-04-06 Lun
    private static final long A2  = 1775520000L; // 2026-04-07 Mar
    private static final long A3  = 1775606400L; // 2026-04-08 Mié
    private static final long A4  = 1775692800L; // 2026-04-09 Jue
    private static final long MY1 = 1777852800L; // 2026-05-04 Lun
    private static final long MY2 = 1777939200L; // 2026-05-05 Mar
    private static final long MY3 = 1778025600L; // 2026-05-06 Mié

    // offsets de hora del día (en segundos)
    // H09=32400, H10=36000, H11=39600, H12=43200, H13=46800
    // H14=50400, H15=54000, H16=57600, H17=61200, H19=68400
    // H20=72000, H21=75600 | madrugada: H01=3600, H1h=5400, H02=7200
    // H2h=9000, H03=10800, H3h=12600, H04=14400, H4h=16200, H05=18000
    // H5h=19800, H06=21600

    private final FileManager fileManager;

    public DataSeeder(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    public void seed() throws IOException {
        log.info("Cargando datos de ejemplo 2026...");

        seederNormalesEnero();
        seederNormalesFebrero();
        seederNormalesMarzoMayo();
        seederAlertas();
        seederFraudes();

        log.info("Seed completado: {} registros (65 normales, 8 MEDIA, 9 ALTA, 23 CONFIRMADO_FRAUDE)",
                fileManager.contarRegistros());
    }

    // ─── Enero 2026 — 30 transacciones normales ───────────────────────────────

    private void seederNormalesEnero() throws IOException {
        // Semana 1 (6-9 ene) — horario laboral, montos habituales por cuenta
        //          id              cuenta    monto      timestamp   d1      d2   d3   d4    d5   estado
        reg("TXN-26-001", "CTA-001",  420000.0, E1+32400, 0.003,  9.0, 1.0, 0.0, 0.20, EstadoAlerta.NORMAL);
        reg("TXN-26-002", "CTA-002", 1800000.0, E1+36000, 0.018, 10.0, 1.0, 0.5, 0.40, EstadoAlerta.NORMAL);
        reg("TXN-26-003", "CTA-003",  180000.0, E1+39600, 0.001, 11.0, 1.0, 0.0, 0.63, EstadoAlerta.NORMAL);
        reg("TXN-26-004", "CTA-004", 2400000.0, E1+50400, 0.024, 14.0, 1.0, 1.0, 0.44, EstadoAlerta.NORMAL);
        reg("TXN-26-005", "CTA-005",  650000.0, E1+54000, 0.006, 15.0, 1.0, 0.0, 0.00, EstadoAlerta.NORMAL);
        reg("TXN-26-006", "CTA-006",  950000.0, E1+43200, 0.009, 12.0, 1.0, 0.5, 0.00, EstadoAlerta.NORMAL);
        reg("TXN-26-007", "CTA-001",  280000.0, E2+36000, 0.002, 10.0, 1.0, 0.5, 1.13, EstadoAlerta.NORMAL);
        reg("TXN-26-008", "CTA-002",  900000.0, E2+46800, 0.008, 13.0, 1.0, 0.0, 1.40, EstadoAlerta.NORMAL);
        reg("TXN-26-009", "CTA-007", 1600000.0, E2+39600, 0.016, 11.0, 1.0, 1.0, 0.50, EstadoAlerta.NORMAL);
        reg("TXN-26-010", "CTA-003",  220000.0, E2+54000, 0.001, 15.0, 1.0, 0.5, 0.13, EstadoAlerta.NORMAL);
        reg("TXN-26-011", "CTA-008", 4200000.0, E3+32400, 0.043,  9.0, 1.0, 1.0, 0.00, EstadoAlerta.NORMAL);
        reg("TXN-26-012", "CTA-004", 3100000.0, E3+46800, 0.032, 13.0, 1.0, 0.0, 0.33, EstadoAlerta.NORMAL);
        reg("TXN-26-013", "CTA-005",  780000.0, E3+50400, 0.007, 14.0, 1.0, 0.5, 0.65, EstadoAlerta.NORMAL);
        reg("TXN-26-014", "CTA-006", 1100000.0, E3+57600, 0.011, 16.0, 1.0, 1.0, 0.50, EstadoAlerta.NORMAL);
        reg("TXN-26-015", "CTA-001",  350000.0, E4+39600, 0.003, 11.0, 1.0, 0.0, 0.67, EstadoAlerta.NORMAL);
        reg("TXN-26-016", "CTA-007", 2300000.0, E4+43200, 0.023, 12.0, 1.0, 0.5, 0.67, EstadoAlerta.NORMAL);
        reg("TXN-26-017", "CTA-002", 1400000.0, E4+50400, 0.014, 14.0, 1.0, 1.0, 0.40, EstadoAlerta.NORMAL);
        // CTA-F01 y CTA-F02: inician normales; se comprometen en febrero/marzo
        reg("TXN-26-018", "CTA-F01",  580000.0, E4+36000, 0.005, 10.0, 1.0, 0.0, 0.17, EstadoAlerta.NORMAL);
        reg("TXN-26-019", "CTA-F01",  420000.0, E4+57600, 0.003, 16.0, 2.0, 0.5, 0.72, EstadoAlerta.NORMAL);
        reg("TXN-26-020", "CTA-F02", 1200000.0, E4+46800, 0.012, 13.0, 1.0, 1.0, 0.29, EstadoAlerta.NORMAL);

        // Semana 2 (12-15 ene)
        reg("TXN-26-021", "CTA-003",  300000.0, E5+39600, 0.002, 11.0, 1.0, 0.0, 0.88, EstadoAlerta.NORMAL);
        reg("TXN-26-022", "CTA-004", 2800000.0, E5+50400, 0.029, 14.0, 1.0, 0.5, 0.00, EstadoAlerta.NORMAL);
        reg("TXN-26-023", "CTA-005",  520000.0, E5+43200, 0.004, 12.0, 1.0, 1.0, 0.65, EstadoAlerta.NORMAL);
        reg("TXN-26-024", "CTA-001",  460000.0, E5+54000, 0.004, 15.0, 1.0, 0.0, 0.07, EstadoAlerta.NORMAL);
        reg("TXN-26-025", "CTA-008", 3800000.0, E6+32400, 0.039,  9.0, 1.0, 0.5, 0.33, EstadoAlerta.NORMAL);
        reg("TXN-26-026", "CTA-006",  850000.0, E6+46800, 0.008, 13.0, 1.0, 0.0, 0.33, EstadoAlerta.NORMAL);
        reg("TXN-26-027", "CTA-007", 1900000.0, E6+39600, 0.019, 11.0, 1.0, 1.0, 0.00, EstadoAlerta.NORMAL);
        reg("TXN-26-028", "CTA-002", 2100000.0, E7+50400, 0.021, 14.0, 1.0, 0.5, 1.00, EstadoAlerta.NORMAL);
        reg("TXN-26-029", "CTA-F02",  950000.0, E7+36000, 0.009, 10.0, 1.0, 0.0, 0.43, EstadoAlerta.NORMAL);
        reg("TXN-26-030", "CTA-F01",  650000.0, E8+43200, 0.006, 12.0, 1.0, 0.5, 0.56, EstadoAlerta.NORMAL);
    }

    // ─── Febrero 2026 — 18 transacciones normales ─────────────────────────────

    private void seederNormalesFebrero() throws IOException {
        reg("TXN-26-031", "CTA-001",  480000.0, F1+36000, 0.004, 10.0, 1.0, 0.5, 0.20, EstadoAlerta.NORMAL);
        reg("TXN-26-032", "CTA-002", 1650000.0, F1+46800, 0.016, 13.0, 1.0, 1.0, 0.10, EstadoAlerta.NORMAL);
        reg("TXN-26-033", "CTA-003",  240000.0, F1+39600, 0.001, 11.0, 1.0, 0.0, 0.13, EstadoAlerta.NORMAL);
        reg("TXN-26-034", "CTA-004", 2200000.0, F1+54000, 0.022, 15.0, 1.0, 0.5, 0.67, EstadoAlerta.NORMAL);
        reg("TXN-26-035", "CTA-005",  720000.0, F2+32400, 0.006,  9.0, 1.0, 0.0, 0.35, EstadoAlerta.NORMAL);
        reg("TXN-26-036", "CTA-006", 1050000.0, F2+43200, 0.010, 12.0, 1.0, 0.5, 0.33, EstadoAlerta.NORMAL);
        reg("TXN-26-037", "CTA-007", 1750000.0, F2+50400, 0.017, 14.0, 1.0, 1.0, 0.25, EstadoAlerta.NORMAL);
        reg("TXN-26-038", "CTA-008", 4500000.0, F3+36000, 0.046, 10.0, 1.0, 0.5, 0.25, EstadoAlerta.NORMAL);
        // CTA-F01: última tx normal antes de ser comprometida el 20 feb
        reg("TXN-26-039", "CTA-F01",  550000.0, F3+46800, 0.005, 13.0, 1.0, 0.0, 0.00, EstadoAlerta.NORMAL);
        reg("TXN-26-040", "CTA-F02", 1100000.0, F3+39600, 0.011, 11.0, 1.0, 1.0, 0.00, EstadoAlerta.NORMAL);
        reg("TXN-26-041", "CTA-001",  390000.0, F4+54000, 0.003, 15.0, 1.0, 0.0, 0.40, EstadoAlerta.NORMAL);
        reg("TXN-26-042", "CTA-004", 2900000.0, F4+46800, 0.030, 13.0, 1.0, 0.5, 0.11, EstadoAlerta.NORMAL);
        reg("TXN-26-043", "CTA-003",  170000.0, F5+32400, 0.001,  9.0, 1.0, 0.0, 0.75, EstadoAlerta.NORMAL);
        reg("TXN-26-044", "CTA-006",  800000.0, F5+43200, 0.007, 12.0, 1.0, 0.5, 0.50, EstadoAlerta.NORMAL);
        reg("TXN-26-045", "CTA-005",  610000.0, F5+50400, 0.005, 14.0, 1.0, 1.0, 0.20, EstadoAlerta.NORMAL);
        reg("TXN-26-046", "CTA-002", 1300000.0, F6+36000, 0.013, 10.0, 1.0, 0.0, 0.60, EstadoAlerta.NORMAL);
        reg("TXN-26-047", "CTA-007", 2100000.0, F6+50400, 0.021, 14.0, 1.0, 0.5, 0.33, EstadoAlerta.NORMAL);
        reg("TXN-26-048", "CTA-008", 3700000.0, F6+43200, 0.038, 12.0, 1.0, 1.0, 0.42, EstadoAlerta.NORMAL);
    }

    // ─── Marzo–Mayo 2026 — 17 transacciones normales ──────────────────────────

    private void seederNormalesMarzoMayo() throws IOException {
        reg("TXN-26-049", "CTA-001",  500000.0, M1+36000, 0.004, 10.0, 1.0, 0.5, 0.33, EstadoAlerta.NORMAL);
        reg("TXN-26-050", "CTA-002", 1500000.0, M1+46800, 0.015, 13.0, 1.0, 0.0, 0.20, EstadoAlerta.NORMAL);
        reg("TXN-26-051", "CTA-004", 2600000.0, M1+39600, 0.026, 11.0, 1.0, 0.5, 0.22, EstadoAlerta.NORMAL);
        reg("TXN-26-052", "CTA-006",  900000.0, M2+43200, 0.009, 12.0, 1.0, 1.0, 0.17, EstadoAlerta.NORMAL);
        reg("TXN-26-053", "CTA-007", 1850000.0, M2+50400, 0.018, 14.0, 1.0, 0.0, 0.08, EstadoAlerta.NORMAL);
        reg("TXN-26-054", "CTA-008", 4100000.0, M3+36000, 0.042, 10.0, 1.0, 0.5, 0.08, EstadoAlerta.NORMAL);
        reg("TXN-26-055", "CTA-003",  200000.0, M3+46800, 0.001, 13.0, 1.0, 0.0, 0.38, EstadoAlerta.NORMAL);
        reg("TXN-26-056", "CTA-005",  680000.0, M4+39600, 0.006, 11.0, 1.0, 0.5, 0.15, EstadoAlerta.NORMAL);
        // CTA-F03 inicia con operación normal antes de convertirse en cuenta mula
        reg("TXN-26-057", "CTA-F03",  350000.0, M4+43200, 0.003, 12.0, 1.0, 0.0, 0.00, EstadoAlerta.NORMAL);
        reg("TXN-26-058", "CTA-001",  430000.0, A1+39600, 0.003, 11.0, 1.0, 0.0, 0.13, EstadoAlerta.NORMAL);
        reg("TXN-26-059", "CTA-002", 1700000.0, A1+50400, 0.017, 14.0, 1.0, 0.5, 0.20, EstadoAlerta.NORMAL);
        reg("TXN-26-060", "CTA-004", 3200000.0, A2+36000, 0.032, 10.0, 1.0, 1.0, 0.44, EstadoAlerta.NORMAL);
        reg("TXN-26-061", "CTA-006", 1000000.0, A2+46800, 0.010, 13.0, 1.0, 0.0, 0.17, EstadoAlerta.NORMAL);
        reg("TXN-26-062", "CTA-007", 2000000.0, A3+43200, 0.020, 12.0, 1.0, 0.5, 0.17, EstadoAlerta.NORMAL);
        reg("TXN-26-063", "CTA-008", 4800000.0, A3+50400, 0.049, 14.0, 1.0, 1.0, 0.50, EstadoAlerta.NORMAL);
        reg("TXN-26-064", "CTA-001",  450000.0, MY1+36000, 0.004, 10.0, 1.0, 0.0, 0.00, EstadoAlerta.NORMAL);
        reg("TXN-26-065", "CTA-004", 2700000.0, MY1+46800, 0.027, 13.0, 1.0, 0.5, 0.11, EstadoAlerta.NORMAL);
    }

    // ─── Alertas: MEDIA (sospechosas) y ALTA (pendiente analista) ─────────────

    private void seederAlertas() throws IOException {
        // MEDIA: montos inusuales, hora tardía — 1 o 2 vecinos fraude
        reg("TXS-26-001", "CTA-001", 5500000.0, A4+68400, 0.057, 19.0, 2.0, 0.0, 12.0, EstadoAlerta.MEDIA);  // abr 9 19:00 — retiro 12x el promedio
        reg("TXS-26-002", "CTA-003", 3800000.0, MY2+68400, 0.039, 19.0, 2.0, 0.0, 12.0, EstadoAlerta.MEDIA); // may 5 19:00 — cuenta estudiante
        reg("TXS-26-003", "CTA-005", 4200000.0, MY2+72000, 0.043, 20.0, 2.0, 1.0, 12.0, EstadoAlerta.MEDIA); // may 5 20:00
        // señal temprana en cuenta comprometida (6 días antes del fraude de mar 15)
        reg("TXS-26-004", "CTA-F01", 8500000.0, M4+68400, 0.088, 19.0, 2.0, 0.0, 12.0, EstadoAlerta.MEDIA);  // mar 9 19:00
        reg("TXS-26-005", "CTA-006", 6000000.0, A4+75600, 0.062, 21.0, 2.0, 1.0, 12.0, EstadoAlerta.MEDIA);  // abr 9 21:00
        // señal temprana 5 días antes del fraude de mar 15
        reg("TXS-26-006", "CTA-F02", 7200000.0, M5+72000, 0.075, 20.0, 2.0, 0.0, 12.0, EstadoAlerta.MEDIA);  // mar 10 20:00
        reg("TXS-26-007", "CTA-007", 7800000.0, MY3+68400, 0.081, 19.0, 2.0, 0.5, 9.83, EstadoAlerta.MEDIA); // may 6 19:00
        reg("TXS-26-008", "CTA-008", 9500000.0, MY3+75600, 0.099, 21.0, 2.0, 0.0, 4.42, EstadoAlerta.MEDIA); // may 6 21:00

        // ALTA: perfil de fraude claro, pendiente confirmación del analista
        // CTA-F03 como cuenta mula recibiendo fondos — mar 10 madrugada
        reg("TXA-26-001", "CTA-F03", 35000000.0, M5+ 7200, 0.368, 2.0, 4.0, 0.0, 12.0, EstadoAlerta.ALTA);
        reg("TXA-26-002", "CTA-F03", 28000000.0, M5+10800, 0.294, 3.0, 5.0, 1.0, 12.0, EstadoAlerta.ALTA);
        // CTA-F02 empeora la noche del 10 mar (5 días antes de la ola confirmada)
        reg("TXA-26-007", "CTA-F02", 65000000.0, M5+ 3600, 0.683, 1.0, 3.0, 0.0, 12.0, EstadoAlerta.ALTA);
        // CTA-F03 reincide — abr 9 madrugada
        reg("TXA-26-005", "CTA-F03", 55000000.0, A4+10800, 0.578, 3.0, 4.0, 0.0, 12.0, EstadoAlerta.ALTA);
        // cuentas legítimas con un retiro anómalo único — may 5-6 madrugada
        reg("TXA-26-003", "CTA-001", 42000000.0, MY2+ 3600, 0.441, 1.0, 2.0, 0.0, 12.0, EstadoAlerta.ALTA);
        reg("TXA-26-006", "CTA-008", 45000000.0, MY2+ 7200, 0.473, 2.0, 2.0, 1.0, 12.0, EstadoAlerta.ALTA);
        reg("TXA-26-004", "CTA-002", 38000000.0, MY3+ 7200, 0.400, 2.0, 2.0, 1.0, 12.0, EstadoAlerta.ALTA);
        reg("TXA-26-008", "CTA-004", 30000000.0, MY3+10800, 0.315, 3.0, 2.0, 0.0, 12.0, EstadoAlerta.ALTA);
        // cuenta de estudiante con retiro imposible para su perfil
        reg("TXA-26-009", "CTA-003", 50000000.0, MY2+14400, 0.524, 4.0, 2.0, 1.0, 12.0, EstadoAlerta.ALTA);
    }

    // ─── Fraudes confirmados — dos olas de ataque ──────────────────────────────

    private void seederFraudes() throws IOException {
        // Ola 1: CTA-F01 comprometida — noche del 20 feb 2026
        // secuencia de 9 retiros/transferencias masivos en 4,5 horas
        reg("TXF-26-001", "CTA-F01", 48000000.0, FA+ 3600, 0.504, 1.0, 1.0, 0.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-002", "CTA-F01", 62000000.0, FA+ 5400, 0.651, 1.5, 2.0, 1.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-003", "CTA-F01", 75000000.0, FA+ 7200, 0.788, 2.0, 3.0, 0.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-004", "CTA-F01", 85000000.0, FA+ 9000, 0.894, 2.5, 4.0, 0.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-005", "CTA-F01", 90000000.0, FA+10800, 0.947, 3.0, 5.0, 1.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-006", "CTA-F01", 55000000.0, FA+12600, 0.578, 3.5, 6.0, 0.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-007", "CTA-F01", 71000000.0, FA+14400, 0.746, 4.0, 7.0, 1.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-008", "CTA-F01", 95000000.0, FA+16200, 1.000, 4.5, 8.0, 0.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-009", "CTA-F01", 82000000.0, FA+19800, 0.862, 5.5, 9.0, 0.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);

        // cuentas vinculadas a la ola 1 (misma noche)
        reg("TXF-26-019", "CTA-004", 44000000.0, FA+18000, 0.462, 5.0, 2.0, 0.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-023", "CTA-006", 35000000.0, FA+21600, 0.368, 6.0, 2.0, 1.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);

        // ataque satélite — noche del 9 mar (CTA-008 y CTA-007 comprometidas)
        reg("TXF-26-020", "CTA-008", 58000000.0, M4+ 3600, 0.609, 1.0, 1.0, 0.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-022", "CTA-007", 46000000.0, M4+ 5400, 0.483, 1.5, 1.0, 0.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-021", "CTA-008", 72000000.0, M4+ 7200, 0.756, 2.0, 2.0, 1.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);

        // Ola 2: CTA-F02 comprometida — noche del 15 mar 2026
        // secuencia de 8 operaciones masivas en 3,5 horas
        reg("TXF-26-010", "CTA-F02", 52000000.0, MB+ 3600, 0.547, 1.0, 1.0, 0.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-011", "CTA-F02", 68000000.0, MB+ 5400, 0.714, 1.5, 2.0, 1.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-012", "CTA-F02", 79000000.0, MB+ 7200, 0.830, 2.0, 3.0, 0.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-013", "CTA-F02", 88000000.0, MB+ 9000, 0.926, 2.5, 4.0, 0.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-014", "CTA-F02", 92000000.0, MB+10800, 0.968, 3.0, 5.0, 1.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-015", "CTA-F02", 63000000.0, MB+12600, 0.662, 3.5, 6.0, 0.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-016", "CTA-F02", 78000000.0, MB+14400, 0.820, 4.0, 7.0, 1.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
        reg("TXF-26-017", "CTA-F02", 91000000.0, MB+16200, 0.958, 4.5, 8.0, 0.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);

        // CTA-F03 como mula — recibe fondos en la misma noche de la ola 2
        reg("TXF-26-018", "CTA-F03", 40000000.0, MB+18000, 0.420, 5.0, 6.0, 1.0, 12.0, EstadoAlerta.CONFIRMADO_FRAUDE);
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
