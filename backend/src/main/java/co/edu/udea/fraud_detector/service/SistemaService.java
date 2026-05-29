package co.edu.udea.fraud_detector.service;

import co.edu.udea.fraud_detector.estructura.hash.HashTable;
import co.edu.udea.fraud_detector.estructura.kdtree.KDTree;
import co.edu.udea.fraud_detector.model.dto.EstadisticasDTO;
import co.edu.udea.fraud_detector.model.dto.SaludDTO;
import co.edu.udea.fraud_detector.model.enums.EstadoAlerta;
import co.edu.udea.fraud_detector.model.exception.ValidacionException;
import co.edu.udea.fraud_detector.persistencia.DataSeeder;
import co.edu.udea.fraud_detector.persistencia.DimensionCalculator;
import co.edu.udea.fraud_detector.persistencia.FileManager;
import co.edu.udea.fraud_detector.persistencia.RegistroTransaccion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// métricas, diagnóstico y gestión del sistema (estadísticas, salud, seed)
@Slf4j
@Service
public class SistemaService {

    private final FileManager        fileManager;
    private final HashTable          hashTable;
    private final KDTree             kdTree;
    private final DataSeeder         dataSeeder;
    private final DimensionCalculator dimensionCalculator;

    public SistemaService(FileManager fileManager,
                          HashTable hashTable,
                          KDTree kdTree,
                          DataSeeder dataSeeder,
                          DimensionCalculator dimensionCalculator) {
        this.fileManager         = fileManager;
        this.hashTable           = hashTable;
        this.kdTree              = kdTree;
        this.dataSeeder          = dataSeeder;
        this.dimensionCalculator = dimensionCalculator;
    }

    public EstadisticasDTO estadisticas() throws IOException {
        List<RegistroTransaccion> todos = fileManager.loadAll();

        int total     = todos.size();
        int activos   = (int) todos.stream().filter(RegistroTransaccion::isActivo).count();
        int eliminados = total - activos;

        // Distribución por estado (orden fijo para facilitar la lectura)
        Map<String, Long> distribucion = new LinkedHashMap<>();
        for (EstadoAlerta e : EstadoAlerta.values()) {
            distribucion.put(e.name(), 0L);
        }
        todos.stream()
                .filter(RegistroTransaccion::isActivo)
                .collect(Collectors.groupingBy(
                        r -> EstadoAlerta.fromCodigo(r.estado_alerta & 0xFF).name(),
                        Collectors.counting()))
                .forEach(distribucion::put);

        EstadisticasDTO dto = new EstadisticasDTO();
        dto.totalRegistros      = total;
        dto.registrosActivos    = activos;
        dto.registrosEliminados = eliminados;
        dto.archivoRuta         = fileManager.getFilePath();
        dto.distribucionEstados = distribucion;

        dto.hashEntradas        = hashTable.size();
        dto.hashTableSize       = hashTable.getTableSize();
        dto.hashLoadFactor      = hashTable.getLoadFactor();
        dto.hashBucketsOcupados = hashTable.getBucketsOcupados();

        dto.kdTreeTotalNodos    = kdTree.getTotalNodos();
        dto.kdTreeNodosActivos  = kdTree.getSizeActivos();

        return dto;
    }

    // carga los 30 registros de ejemplo; con forzar=true reinicia todo primero
    public Map<String, Object> seed(boolean forzar) throws IOException {
        if (!forzar && !fileManager.isEmpty()) {
            throw new ValidacionException(
                "El archivo ya contiene datos. Use ?forzar=true para reiniciar con los datos de ejemplo.");
        }

        if (forzar) {
            log.info("Reiniciando sistema con datos de ejemplo (forzar=true)...");
            fileManager.resetear();
            hashTable.limpiar();
            kdTree.limpiar();
        }

        dataSeeder.seed();

        // Recargar estructuras en memoria desde el archivo recién sembrado
        List<RegistroTransaccion> todos = fileManager.loadAll();
        dimensionCalculator.inicializar(todos);
        for (RegistroTransaccion r : todos) {
            if (r.isActivo()) {
                hashTable.insert(r.getIdTransaccion(), r.numRegistro);
                kdTree.insert(r);
            }
        }

        int total = fileManager.contarRegistros();
        log.info("Seed completado | {} registros | Hash: {} entradas | KDTree: {} nodos",
                total, hashTable.size(), kdTree.getSizeActivos());

        return Map.of(
                "mensaje",           "Datos de ejemplo cargados correctamente",
                "registrosCargados", total,
                "hashEntradas",      hashTable.size(),
                "kdTreeNodos",       kdTree.getSizeActivos());
    }

    public SaludDTO salud() {
        SaludDTO dto = new SaludDTO();
        try {
            dto.registrosTotales = fileManager.contarRegistros();
            dto.archivoDatos     = "OK";
        } catch (IOException e) {
            dto.archivoDatos     = "ERROR: " + e.getMessage();
            dto.registrosTotales = -1;
        }
        dto.hashTable = "OK (" + hashTable.size() + " entradas)";
        dto.kdTree    = "OK (" + kdTree.getSizeActivos() + " nodos activos)";
        dto.estado    = dto.archivoDatos.equals("OK") ? "OK" : "DEGRADADO";
        return dto;
    }
}
