package co.edu.udea.fraud_detector.persistencia;

import co.edu.udea.fraud_detector.estructura.hash.HashTable;
import co.edu.udea.fraud_detector.estructura.kdtree.KDTree;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// se ejecuta al arrancar: verifica el archivo, carga el seeder si está vacío
// y reconstruye HashTable + KDTree en memoria con los registros activos
@Slf4j
@Component
public class SystemInitializer implements ApplicationRunner {

    private final FileManager        fileManager;
    private final DataSeeder         dataSeeder;
    private final HashTable          hashTable;
    private final KDTree             kdTree;
    private final DimensionCalculator dimensionCalculator;

    public SystemInitializer(FileManager fileManager,
                             DataSeeder dataSeeder,
                             HashTable hashTable,
                             KDTree kdTree,
                             DimensionCalculator dimensionCalculator) {
        this.fileManager         = fileManager;
        this.dataSeeder          = dataSeeder;
        this.hashTable           = hashTable;
        this.kdTree              = kdTree;
        this.dimensionCalculator = dimensionCalculator;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== Inicializando Sistema de Detección de Fraude ===");

        // 1. Verificar/crear archivo
        fileManager.inicializar();

        // 2. Cargar datos de ejemplo si el archivo está vacío
        if (fileManager.isEmpty()) {
            log.info("Base de datos vacía — cargando datos de ejemplo...");
            dataSeeder.seed();
        }

        // 3. Leer todos los registros del archivo
        List<RegistroTransaccion> todos = fileManager.loadAll();

        // 4. Reconstruir estadísticas para DimensionCalculator
        dimensionCalculator.inicializar(todos);

        // 5. Poblar HashTable y KDTree con registros activos
        int cargados = 0;
        for (RegistroTransaccion r : todos) {
            if (r.isActivo()) {
                hashTable.insert(r.getIdTransaccion(), r.numRegistro);
                kdTree.insert(r);
                cargados++;
            }
        }

        log.info("=== Sistema listo ===");
        log.info("  Registros en disco : {}", todos.size());
        log.info("  Registros activos  : {}", cargados);
        log.info("  HashTable          : {} entradas (load factor: {})",
                hashTable.size(), String.format("%.2f", hashTable.getLoadFactor()));
        log.info("  KDTree             : {} nodos activos", kdTree.getSizeActivos());
        log.info("  Archivo            : {}", fileManager.getFilePath());
    }
}
