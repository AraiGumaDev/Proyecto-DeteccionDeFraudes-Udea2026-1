package co.edu.udea.fraud_detector.estructura.kdtree;

import co.edu.udea.fraud_detector.persistencia.RegistroTransaccion;

/**
 * Nodo del KD-tree 5D.
 * Cada nodo almacena un RegistroTransaccion y referencia a sus dos subárboles.
 */
public class KDNode {

    public RegistroTransaccion registro;
    public KDNode              izquierdo;
    public KDNode              derecho;

    public KDNode(RegistroTransaccion registro) {
        this.registro   = registro;
        this.izquierdo  = null;
        this.derecho    = null;
    }

    /** Valor de la dimensión indicada (0=d1 … 4=d5). */
    public double getDimension(int eje) {
        return registro.getDimensiones()[eje];
    }
}
