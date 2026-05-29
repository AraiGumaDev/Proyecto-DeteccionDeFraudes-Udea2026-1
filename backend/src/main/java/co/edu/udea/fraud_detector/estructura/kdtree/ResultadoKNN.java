package co.edu.udea.fraud_detector.estructura.kdtree;

import co.edu.udea.fraud_detector.persistencia.RegistroTransaccion;

/**
 * Resultado de una búsqueda KNN: par (registro, distancia euclidiana 5D).
 * Implementa Comparable en orden ascendente de distancia para ordenar
 * la lista final de resultados (el más cercano primero).
 */
public class ResultadoKNN implements Comparable<ResultadoKNN> {

    public final RegistroTransaccion registro;
    public final double              distancia;

    public ResultadoKNN(RegistroTransaccion registro, double distancia) {
        this.registro  = registro;
        this.distancia = distancia;
    }

    /** Orden ascendente: el vecino más cercano queda primero. */
    @Override
    public int compareTo(ResultadoKNN otro) {
        return Double.compare(this.distancia, otro.distancia);
    }
}
