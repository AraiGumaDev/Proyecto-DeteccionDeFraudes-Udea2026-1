package co.edu.udea.fraud_detector.service;

import co.edu.udea.fraud_detector.estructura.kdtree.KDTree;
import co.edu.udea.fraud_detector.estructura.kdtree.ResultadoKNN;
import co.edu.udea.fraud_detector.model.dto.RangoBusquedaDTO;
import co.edu.udea.fraud_detector.model.dto.TransaccionCompletaDTO;
import co.edu.udea.fraud_detector.model.dto.VecinoDTO;
import co.edu.udea.fraud_detector.model.enums.EstadoAlerta;
import co.edu.udea.fraud_detector.model.exception.TransaccionNoEncontradaException;
import co.edu.udea.fraud_detector.model.exception.ValidacionException;
import co.edu.udea.fraud_detector.persistencia.FileManager;
import co.edu.udea.fraud_detector.persistencia.RegistroTransaccion;
import co.edu.udea.fraud_detector.estructura.hash.HashTable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// Detección de fraude con KNN sobre el KD-tree
// regla: >=3 vecinos confirmados → ALTA | >=1 → MEDIA | 0 → NORMAL
// CONFIRMADO_FRAUDE y FALSO_POSITIVO nunca se sobreescriben (los decide el analista)
@Slf4j
@Service
public class DeteccionService {

    static final int K = 5;

    private final KDTree      kdTree;
    private final HashTable   hashTable;
    private final FileManager fileManager;

    public DeteccionService(KDTree kdTree, HashTable hashTable, FileManager fileManager) {
        this.kdTree      = kdTree;
        this.hashTable   = hashTable;
        this.fileManager = fileManager;
    }

    // clasifica un registro que AÚN NO está en el árbol; retorna NORMAL si no hay suficientes vecinos
    public byte clasificarAlerta(RegistroTransaccion registro) {
        if (kdTree.getSizeActivos() < K) {
            return (byte) EstadoAlerta.NORMAL.codigo;
        }
        List<ResultadoKNN> vecinos = kdTree.knn(registro.getDimensiones(), K);
        return clasificarSegunVecinos(vecinos);
    }

    // reclasifica un registro que YA está en el árbol: pide K+1 y excluye el propio ID
    public byte reclasificarAlerta(RegistroTransaccion registro) {
        byte estadoActual = registro.estado_alerta;
        // No sobreescribir decisiones del analista
        if (estadoActual == EstadoAlerta.CONFIRMADO_FRAUDE.codigo
                || estadoActual == EstadoAlerta.FALSO_POSITIVO.codigo) {
            return estadoActual;
        }
        // Necesitamos al menos K+1 activos para excluir self y tener K vecinos
        if (kdTree.getSizeActivos() <= K) {
            return (byte) EstadoAlerta.NORMAL.codigo;
        }
        String propioId = registro.getIdTransaccion();
        List<ResultadoKNN> todos = kdTree.knn(registro.getDimensiones(), K + 1);
        List<ResultadoKNN> vecinos = todos.stream()
                .filter(v -> !v.registro.getIdTransaccion().equals(propioId))
                .limit(K)
                .collect(Collectors.toList());
        return clasificarSegunVecinos(vecinos);
    }

    public TransaccionCompletaDTO analizar(String id) throws IOException {
        RegistroTransaccion reg = leerActivo(id);
        byte nuevoEstado = reclasificarAlerta(reg);
        if (nuevoEstado != reg.estado_alerta) {
            fileManager.updateEstadoAlerta(reg.numRegistro, nuevoEstado);
            kdTree.actualizarEstado(id, nuevoEstado);
            reg.estado_alerta = nuevoEstado;
        }
        log.info("Análisis KNN [{}] → {}", id, EstadoAlerta.fromCodigo(nuevoEstado).name());
        return TransaccionCompletaDTO.from(reg);
    }

    public List<VecinoDTO> obtenerVecinos(String id, int k) throws IOException {
        if (k < 1 || k > 50) throw new ValidacionException("k debe estar entre 1 y 50");
        RegistroTransaccion reg = leerActivo(id);
        if (kdTree.getSizeActivos() <= k) {
            throw new ValidacionException(
                "El sistema necesita al menos " + (k + 1) + " registros activos para buscar " + k + " vecinos");
        }
        List<ResultadoKNN> todos = kdTree.knn(reg.getDimensiones(), k + 1);
        List<VecinoDTO> resultado = new ArrayList<>();
        int posicion = 1;
        for (ResultadoKNN r : todos) {
            if (r.registro.getIdTransaccion().equals(id)) continue;
            resultado.add(VecinoDTO.of(posicion++, r.distancia, TransaccionCompletaDTO.from(r.registro)));
            if (resultado.size() == k) break;
        }
        return resultado;
    }

    public List<TransaccionCompletaDTO> buscarRango(RangoBusquedaDTO rango) {
        if (rango.min == null || rango.max == null
                || rango.min.length != 5 || rango.max.length != 5) {
            throw new ValidacionException("min y max deben ser arrays de exactamente 5 valores");
        }
        for (int i = 0; i < 5; i++) {
            if (rango.min[i] > rango.max[i]) {
                throw new ValidacionException("min[" + i + "] no puede ser mayor que max[" + i + "]");
            }
        }
        return kdTree.rangeSearch(rango.min, rango.max)
                .stream()
                .map(TransaccionCompletaDTO::from)
                .collect(Collectors.toList());
    }

    // re-clasifica todas las NORMAL/MEDIA con el estado actual del árbol; útil tras confirmar fraudes
    public int escaneoMasivo() throws IOException {
        List<RegistroTransaccion> todos = fileManager.loadAll();
        int actualizados = 0;
        for (RegistroTransaccion r : todos) {
            if (!r.isActivo()) continue;
            byte estado = r.estado_alerta;
            if (estado != EstadoAlerta.NORMAL.codigo && estado != EstadoAlerta.MEDIA.codigo) continue;

            byte nuevoEstado = reclasificarAlerta(r);
            if (nuevoEstado != estado) {
                fileManager.updateEstadoAlerta(r.numRegistro, nuevoEstado);
                kdTree.actualizarEstado(r.getIdTransaccion(), nuevoEstado);
                actualizados++;
            }
        }
        log.info("Escaneo masivo completado | actualizados: {}", actualizados);
        return actualizados;
    }

    private byte clasificarSegunVecinos(List<ResultadoKNN> vecinos) {
        long fraudesConfirmados = vecinos.stream()
                .filter(v -> v.registro.estado_alerta == EstadoAlerta.CONFIRMADO_FRAUDE.codigo)
                .count();
        if (fraudesConfirmados >= 3) return (byte) EstadoAlerta.ALTA.codigo;
        if (fraudesConfirmados >= 1) return (byte) EstadoAlerta.MEDIA.codigo;
        return (byte) EstadoAlerta.NORMAL.codigo;
    }

    private RegistroTransaccion leerActivo(String id) throws IOException {
        int numRegistro = hashTable.search(id);
        if (numRegistro == -1) throw new TransaccionNoEncontradaException(id);
        RegistroTransaccion reg = fileManager.read(numRegistro);
        if (!reg.isActivo()) throw new TransaccionNoEncontradaException(id);
        return reg;
    }
}
