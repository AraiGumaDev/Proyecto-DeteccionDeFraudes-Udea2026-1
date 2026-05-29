package co.edu.udea.fraud_detector.estructura.hash;

/**
 * Contenedor del bucket en la tabla hash.
 * Apunta al primer nodo de la lista enlazada y lleva la cuenta de elementos.
 */
public class HashBucket {

    public HashEntry head;
    public int       size;

    public HashBucket() {
        this.head = null;
        this.size = 0;
    }
}
