package co.edu.udea.fraud_detector.service;

import co.edu.udea.fraud_detector.estructura.hash.HashTable;
import co.edu.udea.fraud_detector.estructura.kdtree.KDTree;
import co.edu.udea.fraud_detector.model.dto.PaginaDTO;
import co.edu.udea.fraud_detector.model.dto.TransaccionCompletaDTO;
import co.edu.udea.fraud_detector.model.dto.TransaccionInputDTO;
import co.edu.udea.fraud_detector.model.dto.TransaccionUpdateDTO;
import co.edu.udea.fraud_detector.model.enums.EstadoAlerta;
import co.edu.udea.fraud_detector.model.enums.TipoTransaccion;
import co.edu.udea.fraud_detector.model.exception.TransaccionDuplicadaException;
import co.edu.udea.fraud_detector.model.exception.TransaccionNoEncontradaException;
import co.edu.udea.fraud_detector.model.exception.ValidacionException;
import co.edu.udea.fraud_detector.persistencia.DimensionCalculator;
import co.edu.udea.fraud_detector.persistencia.FileManager;
import co.edu.udea.fraud_detector.persistencia.RegistroTransaccion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

// CRUD de transacciones; en crear() el KNN se ejecuta ANTES de insertar en el árbol
// para evitar que el registro nuevo aparezca como su propio vecino
@Slf4j
@Service
public class TransaccionService {

    private final FileManager         fileManager;
    private final HashTable           hashTable;
    private final KDTree              kdTree;
    private final DimensionCalculator dimensionCalculator;
    private final DeteccionService    deteccionService;

    public TransaccionService(FileManager fileManager,
                              HashTable hashTable,
                              KDTree kdTree,
                              DimensionCalculator dimensionCalculator,
                              DeteccionService deteccionService) {
        this.fileManager         = fileManager;
        this.hashTable           = hashTable;
        this.kdTree              = kdTree;
        this.dimensionCalculator = dimensionCalculator;
        this.deteccionService    = deteccionService;
    }

    public TransaccionCompletaDTO crear(TransaccionInputDTO input) throws IOException {
        validar(input);
        if (hashTable.contains(input.idTransaccion)) {
            throw new TransaccionDuplicadaException(input.idTransaccion);
        }

        TipoTransaccion tipo = parsearTipo(input.tipo);

        // Calcular dimensiones antes de insertar (el árbol no tiene este registro aún)
        double d1 = dimensionCalculator.calcularD1(input.monto);
        double d2 = dimensionCalculator.calcularD2(input.timestamp);
        double d3 = dimensionCalculator.calcularD3(input.numCuenta, input.timestamp);
        double d4 = dimensionCalculator.calcularD4(tipo);
        double d5 = dimensionCalculator.calcularD5(input.numCuenta, input.monto);

        RegistroTransaccion reg = new RegistroTransaccion();
        reg.setIdTransaccion(input.idTransaccion);
        reg.setNumCuenta(input.numCuenta);
        reg.monto         = input.monto;
        reg.timestamp     = input.timestamp;
        reg.d1_monto_norm = d1;
        reg.d2_hora       = d2;
        reg.d3_frecuencia = d3;
        reg.d4_tipo       = d4;
        reg.d5_desviacion = d5;
        reg.deleted       = 0;

        // KNN antes de insertar → estado_alerta final antes del primer write
        byte estado = deteccionService.clasificarAlerta(reg);
        reg.estado_alerta = estado;

        // Persistir en disco (un único append con el estado correcto)
        int numRegistro = fileManager.append(reg);
        reg.numRegistro = numRegistro;

        // Actualizar índices en memoria
        hashTable.insert(reg.getIdTransaccion(), numRegistro);
        kdTree.insert(reg);
        dimensionCalculator.actualizarConNuevoRegistro(reg.getNumCuenta(), reg.monto);

        log.info("Transacción creada [{}] cuenta={} monto={} estado={}",
                input.idTransaccion, input.numCuenta, input.monto,
                EstadoAlerta.fromCodigo(estado).name());
        return TransaccionCompletaDTO.from(reg);
    }

    public TransaccionCompletaDTO buscarPorId(String id) throws IOException {
        int numRegistro = hashTable.search(id);
        if (numRegistro == -1) throw new TransaccionNoEncontradaException(id);
        RegistroTransaccion reg = fileManager.read(numRegistro);
        if (!reg.isActivo()) throw new TransaccionNoEncontradaException(id);
        return TransaccionCompletaDTO.from(reg);
    }

    public PaginaDTO<TransaccionCompletaDTO> listar(String numCuenta, String tipo,
                                                     String estadoAlerta,
                                                     int pagina, int tamano) throws IOException {
        if (pagina < 0) throw new ValidacionException("pagina debe ser >= 0");
        if (tamano < 1 || tamano > 100) throw new ValidacionException("tamano debe estar entre 1 y 100");

        List<TransaccionCompletaDTO> filtrados = fileManager.loadAll().stream()
                .filter(RegistroTransaccion::isActivo)
                .filter(r -> numCuenta == null || r.getNumCuenta().equals(numCuenta))
                .filter(r -> tipo == null
                        || TipoTransaccion.fromValor(r.d4_tipo).name().equalsIgnoreCase(tipo))
                .filter(r -> estadoAlerta == null
                        || EstadoAlerta.fromCodigo(r.estado_alerta).name().equalsIgnoreCase(estadoAlerta))
                .map(TransaccionCompletaDTO::from)
                .collect(Collectors.toList());

        PaginaDTO<TransaccionCompletaDTO> resultado = new PaginaDTO<>();
        resultado.totalFiltrados = filtrados.size();
        resultado.pagina         = pagina;
        resultado.tamano         = tamano;
        resultado.contenido      = filtrados.stream()
                .skip((long) pagina * tamano)
                .limit(tamano)
                .collect(Collectors.toList());
        return resultado;
    }

    // actualiza monto/tipo/timestamp, recalcula dimensiones y reclasifica con KNN
    public TransaccionCompletaDTO actualizar(String id, TransaccionUpdateDTO input) throws IOException {
        validarUpdate(input);

        int numRegistro = hashTable.search(id);
        if (numRegistro == -1) throw new TransaccionNoEncontradaException(id);
        RegistroTransaccion original = fileManager.read(numRegistro);
        if (!original.isActivo()) throw new TransaccionNoEncontradaException(id);

        TipoTransaccion tipo = parsearTipo(input.tipo);
        long nuevoTimestamp  = (input.timestamp != null) ? input.timestamp : original.timestamp;

        // objeto nuevo para que el nodo viejo del árbol no comparta la misma referencia
        RegistroTransaccion actualizado = new RegistroTransaccion();
        actualizado.setIdTransaccion(original.getIdTransaccion());
        actualizado.setNumCuenta(original.getNumCuenta());
        actualizado.monto         = input.monto;
        actualizado.timestamp     = nuevoTimestamp;
        actualizado.d1_monto_norm = dimensionCalculator.calcularD1(input.monto);
        actualizado.d2_hora       = dimensionCalculator.calcularD2(nuevoTimestamp);
        actualizado.d3_frecuencia = dimensionCalculator.calcularD3(original.getNumCuenta(), nuevoTimestamp);
        actualizado.d4_tipo       = tipo.valorDimension;
        actualizado.d5_desviacion = dimensionCalculator.calcularD5(original.getNumCuenta(), input.monto);
        actualizado.estado_alerta = original.estado_alerta; // preservar hasta reclasificar
        actualizado.deleted       = 0;
        actualizado.numRegistro   = numRegistro;

        // Reemplazar en KDTree: marcar el nodo antiguo como eliminado e insertar el nuevo
        kdTree.marcarEliminado(id);
        kdTree.insert(actualizado);

        // Reclasificar (el objeto 'actualizado' ya está en el árbol)
        byte nuevoEstado = deteccionService.reclasificarAlerta(actualizado);
        actualizado.estado_alerta = nuevoEstado;

        // Persistir en disco (reescribe los 128 bytes completos)
        fileManager.updateRegistro(numRegistro, actualizado);

        // Actualizar estadísticas de dimensiones con el nuevo monto
        dimensionCalculator.actualizarConNuevoRegistro(actualizado.getNumCuenta(), actualizado.monto);

        log.info("Transacción actualizada [{}] nuevoEstado={}", id, EstadoAlerta.fromCodigo(nuevoEstado).name());
        return TransaccionCompletaDTO.from(actualizado);
    }

    public void eliminar(String id) throws IOException {
        int numRegistro = hashTable.search(id);
        if (numRegistro == -1) throw new TransaccionNoEncontradaException(id);
        RegistroTransaccion reg = fileManager.read(numRegistro);
        if (!reg.isActivo()) throw new TransaccionNoEncontradaException(id);

        fileManager.logicalDelete(numRegistro);
        hashTable.delete(id);
        kdTree.marcarEliminado(id);

        log.info("Transacción eliminada lógicamente [{}]", id);
    }

    private void validar(TransaccionInputDTO input) {
        if (input.idTransaccion == null || input.idTransaccion.isBlank())
            throw new ValidacionException("id_transaccion es obligatorio");
        if (input.idTransaccion.length() > 19)
            throw new ValidacionException("id_transaccion máximo 19 caracteres");
        if (input.numCuenta == null || input.numCuenta.isBlank())
            throw new ValidacionException("num_cuenta es obligatorio");
        if (input.numCuenta.length() > 19)
            throw new ValidacionException("num_cuenta máximo 19 caracteres");
        if (input.monto <= 0)
            throw new ValidacionException("monto debe ser mayor a 0");
        if (input.timestamp <= 0)
            throw new ValidacionException("timestamp debe ser un epoch válido (> 0)");
        if (input.tipo == null || input.tipo.isBlank())
            throw new ValidacionException("tipo es obligatorio");
        parsearTipo(input.tipo); // lanza ValidacionException si el valor no es válido
    }

    private void validarUpdate(TransaccionUpdateDTO input) {
        if (input.monto <= 0)
            throw new ValidacionException("monto debe ser mayor a 0");
        if (input.tipo == null || input.tipo.isBlank())
            throw new ValidacionException("tipo es obligatorio");
        if (input.timestamp != null && input.timestamp <= 0)
            throw new ValidacionException("timestamp debe ser un epoch válido (> 0)");
        parsearTipo(input.tipo);
    }

    private TipoTransaccion parsearTipo(String tipo) {
        try {
            return TipoTransaccion.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidacionException(
                "tipo inválido: '" + tipo + "'. Valores válidos: RETIRO, DEPOSITO, TRANSFERENCIA");
        }
    }
}
