package co.edu.udea.fraud_detector.estructura.kdtree;

import co.edu.udea.fraud_detector.persistencia.RegistroTransaccion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

// KD-tree 5 dimensiones para buscar transacciones similares con KNN
// eje de partición = nivel % 5, izquierdo si dim <= split, derecho si >
// los nodos eliminados (deleted=1) se quedan en el árbol pero no cuentan en KNN
// KNN usa un max-heap de tamaño k: el tope es el vecino más lejano actual
// poda: si distancia al hiperplano >= peor vecino, no vale la pena explorar esa rama
@Slf4j
@Component
public class KDTree {

    private static final int K_DIMS = 5;

    private KDNode raiz;
    private int    totalNodos;
    private int    sizeActivos;

    public void insert(RegistroTransaccion registro) {
        raiz = insertarRecursivo(raiz, registro, 0);
        totalNodos++;
        if (registro.isActivo()) sizeActivos++;
    }

    private KDNode insertarRecursivo(KDNode nodo, RegistroTransaccion registro, int nivel) {
        if (nodo == null) return new KDNode(registro);

        int eje = nivel % K_DIMS;
        double[] dims     = registro.getDimensiones();
        double   splitVal = nodo.getDimension(eje);

        if (dims[eje] <= splitVal) {
            nodo.izquierdo = insertarRecursivo(nodo.izquierdo, registro, nivel + 1);
        } else {
            nodo.derecho = insertarRecursivo(nodo.derecho, registro, nivel + 1);
        }
        return nodo;
    }

    // retorna los k vecinos activos más cercanos al punto; lanza IllegalStateException si hay menos de k activos
    public List<ResultadoKNN> knn(double[] punto, int k) {
        if (sizeActivos < k) {
            throw new IllegalStateException(
                "El KDTree solo tiene " + sizeActivos + " nodos activos; se requieren " + k);
        }
        // Max-heap: el tope siempre es el vecino MÁS LEJANO entre los k candidatos actuales
        PriorityQueue<ResultadoKNN> heap =
            new PriorityQueue<>(k, Comparator.reverseOrder());

        knnRecursivo(raiz, punto, k, heap, 0);

        List<ResultadoKNN> resultado = new ArrayList<>(heap);
        Collections.sort(resultado);   // ascendente: el más cercano primero
        return resultado;
    }

    private void knnRecursivo(KDNode nodo, double[] punto, int k,
                               PriorityQueue<ResultadoKNN> heap, int nivel) {
        if (nodo == null) return;

        // Evaluar este nodo (solo si está activo)
        if (nodo.registro.isActivo()) {
            double dist = distanciaEuclidiana(nodo.registro.getDimensiones(), punto);
            if (heap.size() < k) {
                heap.add(new ResultadoKNN(nodo.registro, dist));
            } else if (dist < heap.peek().distancia) {
                heap.poll();
                heap.add(new ResultadoKNN(nodo.registro, dist));
            }
        }

        int    eje        = nivel % K_DIMS;
        double diferencia = punto[eje] - nodo.getDimension(eje);

        // Explorar primero el subárbol más cercano al punto de consulta
        KDNode primero = diferencia <= 0 ? nodo.izquierdo : nodo.derecho;
        KDNode segundo = diferencia <= 0 ? nodo.derecho   : nodo.izquierdo;

        knnRecursivo(primero, punto, k, heap, nivel + 1);

        // Poda: explorar el otro lado solo si puede contener vecinos más cercanos.
        // distancia al hiperplano de corte = |diferencia|.
        // Si es menor que el peor vecino actual, el otro lado podría mejorar.
        if (heap.size() < k || Math.abs(diferencia) < heap.peek().distancia) {
            knnRecursivo(segundo, punto, k, heap, nivel + 1);
        }
    }

    // retorna todos los registros activos dentro del hipercubo [min[0]..max[0]] x ... x [min[4]..max[4]]
    public List<RegistroTransaccion> rangeSearch(double[] min, double[] max) {
        List<RegistroTransaccion> resultado = new ArrayList<>();
        rangeRecursivo(raiz, min, max, resultado, 0);
        return resultado;
    }

    private void rangeRecursivo(KDNode nodo, double[] min, double[] max,
                                 List<RegistroTransaccion> resultado, int nivel) {
        if (nodo == null) return;

        double[] dims = nodo.registro.getDimensiones();

        // Comprobar si este nodo está dentro del hipercubo
        if (nodo.registro.isActivo() && dentroDeRango(dims, min, max)) {
            resultado.add(nodo.registro);
        }

        int eje = nivel % K_DIMS;

        // Poda por dimensión de corte:
        //   izquierdo tiene dim[eje] ≤ dims[eje] → explorar si min[eje] ≤ dims[eje]
        //   derecho  tiene dim[eje] >  dims[eje] → explorar si max[eje] >  dims[eje]
        if (min[eje] <= dims[eje]) {
            rangeRecursivo(nodo.izquierdo, min, max, resultado, nivel + 1);
        }
        if (max[eje] > dims[eje]) {
            rangeRecursivo(nodo.derecho, min, max, resultado, nivel + 1);
        }
    }

    private boolean dentroDeRango(double[] dims, double[] min, double[] max) {
        for (int i = 0; i < K_DIMS; i++) {
            if (dims[i] < min[i] || dims[i] > max[i]) return false;
        }
        return true;
    }

    // actualiza estado_alerta en el nodo activo con ese ID para que el KNN vea el estado correcto
    // O(n) pero solo se llama cuando un analista confirma o descarta un fraude
    public boolean actualizarEstado(String idTransaccion, byte nuevoEstado) {
        boolean[] encontrado = { false };
        actualizarEstadoRecursivo(raiz, idTransaccion, nuevoEstado, encontrado);
        return encontrado[0];
    }

    private void actualizarEstadoRecursivo(KDNode nodo, String id, byte estado, boolean[] encontrado) {
        if (nodo == null || encontrado[0]) return;
        if (nodo.registro.isActivo() && nodo.registro.getIdTransaccion().equals(id)) {
            nodo.registro.estado_alerta = estado;
            encontrado[0] = true;
            return;
        }
        actualizarEstadoRecursivo(nodo.izquierdo, id, estado, encontrado);
        actualizarEstadoRecursivo(nodo.derecho,   id, estado, encontrado);
    }

    // marca deleted=1 en el nodo con ese ID; O(n) pero las eliminaciones son poco frecuentes
    public boolean marcarEliminado(String idTransaccion) {
        boolean[] encontrado = { false };
        marcarEliminadoRecursivo(raiz, idTransaccion, encontrado);
        if (encontrado[0]) sizeActivos--;
        return encontrado[0];
    }

    private void marcarEliminadoRecursivo(KDNode nodo, String id, boolean[] encontrado) {
        if (nodo == null || encontrado[0]) return;
        if (nodo.registro.getIdTransaccion().equals(id) && nodo.registro.isActivo()) {
            nodo.registro.deleted = 1;
            encontrado[0] = true;
            return;
        }
        marcarEliminadoRecursivo(nodo.izquierdo, id, encontrado);
        marcarEliminadoRecursivo(nodo.derecho,   id, encontrado);
    }

    private double distanciaEuclidiana(double[] a, double[] b) {
        double suma = 0.0;
        for (int i = 0; i < K_DIMS; i++) {
            double diff = a[i] - b[i];
            suma += diff * diff;
        }
        return Math.sqrt(suma);
    }

    // vacía el árbol; se usa al reiniciar el sistema con el endpoint seed
    public void limpiar() {
        raiz        = null;
        totalNodos  = 0;
        sizeActivos = 0;
    }

    public int getSizeActivos() { return sizeActivos; }
    public int getTotalNodos()  { return totalNodos; }
    public boolean isEmpty()    { return raiz == null; }
}
