package co.edu.udea.fraud_detector.persistencia;

import co.edu.udea.fraud_detector.model.enums.TipoTransaccion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Calcula las 5 dimensiones para el KD-tree al insertar una transacción
// D1: monto normalizado (0-1) | D2: hora del día | D3: frecuencia 24h
// D4: tipo codificado (RETIRO=0.0/DEPOSITO=0.5/TRANSFERENCIA=1.0) | D5: desviación del promedio de la cuenta
// las estadísticas de D1 y D5 se mantienen en memoria y se actualizan con cada inserción
@Slf4j
@Component
public class DimensionCalculator {

    private static final long SEGUNDOS_24H = 86_400L;

    private final FileManager fileManager;

    // Estadísticas globales para D1
    private double montoMin = Double.MAX_VALUE;
    private double montoMax = 0.0;

    // Estadísticas por cuenta para D5: cuenta → [suma, sumaCuadrados, count]
    private final Map<String, double[]> statsPorCuenta = new HashMap<>();

    public DimensionCalculator(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    // reconstruye las estadísticas en memoria a partir del historial cargado del archivo
    // llama a limpiar() primero para que sea seguro invocarlo varias veces
    public void inicializar(List<RegistroTransaccion> registros) {
        limpiar();
        for (RegistroTransaccion r : registros) {
            if (r.isActivo()) {
                actualizarEstadisticasInternas(r.getNumCuenta(), r.monto);
            }
        }
        log.info("DimensionCalculator inicializado | montoMin={} montoMax={} cuentas={}",
                montoMin == Double.MAX_VALUE ? 0 : montoMin, montoMax, statsPorCuenta.size());
    }

    // D1: normaliza el monto al rango [0,1]; si no hay historial retorna 0.5
    public double calcularD1(double monto) {
        if (montoMax <= montoMin) return 0.5;
        double d1 = (monto - montoMin) / (montoMax - montoMin);
        return Math.min(1.0, Math.max(0.0, d1));
    }

    // D2: hora del día como número real (ej. 14:30 → 14.5)
    public double calcularD2(long timestamp) {
        LocalDateTime dt = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC);
        return dt.getHour() + dt.getMinute() / 60.0;
    }

    // D3: cuenta las transacciones activas de la cuenta en las últimas 24h antes del timestamp
    public double calcularD3(String numCuenta, long timestamp) throws IOException {
        long desde = timestamp - SEGUNDOS_24H;
        List<RegistroTransaccion> todos = fileManager.loadAll();
        return todos.stream()
                .filter(r -> r.isActivo()
                        && r.getNumCuenta().equals(numCuenta)
                        && r.timestamp >= desde
                        && r.timestamp < timestamp)
                .count();
    }

    // D4: codificación del tipo (RETIRO=0.0, DEPOSITO=0.5, TRANSFERENCIA=1.0)
    public double calcularD4(TipoTransaccion tipo) {
        return tipo.valorDimension;
    }

    // D5: cuántas desviaciones estándar se aleja el monto del promedio de la cuenta; 0 si no hay historial
    public double calcularD5(String numCuenta, double monto) {
        double[] stats = statsPorCuenta.get(numCuenta);
        if (stats == null || stats[2] == 0) return 0.0;

        double count   = stats[2];
        double avg     = stats[0] / count;
        // Varianza muestral: E[X²] - (E[X])²
        double variance = (count > 1)
                ? (stats[1] / count) - (avg * avg)
                : 0.0;
        double stddev  = Math.sqrt(Math.max(variance, 0.0));

        return Math.abs(monto - avg) / Math.max(stddev, 1.0);
    }

    // actualiza estadísticas internas DESPUÉS de calcular las dimensiones, no antes
    public void actualizarConNuevoRegistro(String numCuenta, double monto) {
        actualizarEstadisticasInternas(numCuenta, monto);
    }

    // resetea todas las estadísticas en memoria
    public void limpiar() {
        montoMin = Double.MAX_VALUE;
        montoMax = 0.0;
        statsPorCuenta.clear();
    }

    private void actualizarEstadisticasInternas(String cuenta, double monto) {
        if (monto < montoMin) montoMin = monto;
        if (monto > montoMax) montoMax = monto;

        double[] stats = statsPorCuenta.computeIfAbsent(cuenta, k -> new double[3]);
        stats[0] += monto;           // suma
        stats[1] += monto * monto;   // suma de cuadrados
        stats[2]++;                  // conteo
    }
}
