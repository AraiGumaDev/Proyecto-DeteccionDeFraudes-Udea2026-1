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

import java.io.File;
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

        // claves en minúsculas para el frontend
        Map<String, Long> distribucionFrontend = new LinkedHashMap<>();
        distribucionFrontend.put("normal",            distribucion.getOrDefault("NORMAL", 0L));
        distribucionFrontend.put("media",             distribucion.getOrDefault("MEDIA", 0L));
        distribucionFrontend.put("alta",              distribucion.getOrDefault("ALTA", 0L));
        distribucionFrontend.put("confirmado_fraude", distribucion.getOrDefault("CONFIRMADO_FRAUDE", 0L));
        distribucionFrontend.put("falso_positivo",    distribucion.getOrDefault("FALSO_POSITIVO", 0L));

        EstadisticasDTO dto = new EstadisticasDTO();
        dto.totalTransacciones  = activos;
        dto.totalEliminadas     = eliminados;
        dto.distribucionAlertas = distribucionFrontend;

        EstadisticasDTO.HashTableStats ht = new EstadisticasDTO.HashTableStats();
        ht.bucketsTotales = hashTable.getTableSize();
        ht.factorCarga    = hashTable.getLoadFactor();
        ht.colisiones     = Math.max(0, hashTable.size() - hashTable.getBucketsOcupados());
        dto.hashTable     = ht;

        EstadisticasDTO.KDTreeStats kd = new EstadisticasDTO.KDTreeStats();
        kd.totalNodos     = kdTree.getSizeActivos();
        kd.profundidadMax = kdTree.getProfundidadMax();
        dto.kdtree        = kd;

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
        long archivoBytesVal = 0;
        try {
            dto.registrosTotales = fileManager.contarRegistros();
            dto.archivoDatos     = "OK";
            File f = new File(fileManager.getFilePath());
            archivoBytesVal = f.exists() ? f.length() : 0;
        } catch (IOException e) {
            dto.archivoDatos     = "ERROR: " + e.getMessage();
            dto.registrosTotales = -1;
        }
        dto.hashTable      = "OK (" + hashTable.size() + " entradas)";
        dto.kdTree         = "OK (" + kdTree.getSizeActivos() + " nodos activos)";
        dto.estado         = dto.archivoDatos.equals("OK") ? "OK" : "DEGRADADO";
        dto.hashTableCarga = hashTable.getLoadFactor();
        dto.kdtreeNodos    = kdTree.getSizeActivos();
        dto.archivoBytes   = archivoBytesVal;
        return dto;
    }
}
