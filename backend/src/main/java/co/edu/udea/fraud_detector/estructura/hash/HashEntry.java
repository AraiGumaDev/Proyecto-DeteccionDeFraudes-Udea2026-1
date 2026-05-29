package co.edu.udea.fraud_detector.estructura.hash;

/**
 * Nodo de la lista enlazada que implementa el encadenamiento del Hash.
 * Almacena la clave (id_transaccion) y el valor (num_registro en disco).
 */
public class HashEntry {

    public final String idTransaccion;
    public int          numRegistro;
    public HashEntry    siguiente;

    public HashEntry(String idTransaccion, int numRegistro) {
        this.idTransaccion = idTransaccion;
        this.numRegistro   = numRegistro;
        this.siguiente     = null;
    }
}
