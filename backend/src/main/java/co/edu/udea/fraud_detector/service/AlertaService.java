package co.edu.udea.fraud_detector.service;

import co.edu.udea.fraud_detector.estructura.hash.HashTable;
import co.edu.udea.fraud_detector.estructura.kdtree.KDTree;
import co.edu.udea.fraud_detector.model.dto.TransaccionCompletaDTO;
import co.edu.udea.fraud_detector.model.enums.EstadoAlerta;
import co.edu.udea.fraud_detector.model.exception.TransaccionNoEncontradaException;
import co.edu.udea.fraud_detector.model.exception.ValidacionException;
import co.edu.udea.fraud_detector.persistencia.FileManager;
import co.edu.udea.fraud_detector.persistencia.RegistroTransaccion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

// gestión de alertas: listar activas y confirmar/descartar por el analista
@Slf4j
@Service
public class AlertaService {

    private final FileManager fileManager;
    private final HashTable   hashTable;
    private final KDTree      kdTree;

    public AlertaService(FileManager fileManager, HashTable hashTable, KDTree kdTree) {
        this.fileManager = fileManager;
        this.hashTable   = hashTable;
        this.kdTree      = kdTree;
    }

    public List<TransaccionCompletaDTO> listarActivas() throws IOException {
        return fileManager.loadAll().stream()
                .filter(RegistroTransaccion::isActivo)
                .filter(r -> r.estado_alerta == EstadoAlerta.MEDIA.codigo
                          || r.estado_alerta == EstadoAlerta.ALTA.codigo)
                .map(TransaccionCompletaDTO::from)
                .collect(Collectors.toList());
    }

    public TransaccionCompletaDTO confirmar(String id) throws IOException {
        return cambiarEstado(id, EstadoAlerta.CONFIRMADO_FRAUDE);
    }

    public TransaccionCompletaDTO descartar(String id) throws IOException {
        return cambiarEstado(id, EstadoAlerta.FALSO_POSITIVO);
    }

    private TransaccionCompletaDTO cambiarEstado(String id, EstadoAlerta nuevoEstado) throws IOException {
        int numRegistro = hashTable.search(id);
        if (numRegistro == -1) throw new TransaccionNoEncontradaException(id);

        RegistroTransaccion reg = fileManager.read(numRegistro);
        if (!reg.isActivo()) throw new TransaccionNoEncontradaException(id);

        byte estadoActual = reg.estado_alerta;
        // Evitar confirmar/descartar algo ya decidido por otro analista
        if (estadoActual == EstadoAlerta.CONFIRMADO_FRAUDE.codigo
                && nuevoEstado == EstadoAlerta.CONFIRMADO_FRAUDE) {
            throw new ValidacionException("La transacción ya está confirmada como fraude");
        }
        if (estadoActual == EstadoAlerta.FALSO_POSITIVO.codigo
                && nuevoEstado == EstadoAlerta.FALSO_POSITIVO) {
            throw new ValidacionException("La transacción ya fue descartada como falso positivo");
        }

        byte nuevoByte = (byte) nuevoEstado.codigo;
        fileManager.updateEstadoAlerta(numRegistro, nuevoByte);
        kdTree.actualizarEstado(id, nuevoByte);
        reg.estado_alerta = nuevoByte;

        log.info("Alerta [{}] → {} por analista", id, nuevoEstado.name());
        return TransaccionCompletaDTO.from(reg);
    }
}
