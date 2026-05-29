package co.edu.udea.fraud_detector.estructura.hash;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Tabla hash con encadenamiento: guarda id_transaccion → num_registro en disco
// función hash: h = (h * 31 + c) % tableSize (tableSize=1543, primo)
// el offset real en el archivo se calcula como: num_registro * 128
@Slf4j
@Component
public class HashTable {

    private final HashBucket[] buckets;
    private final int          tableSize;
    private int                totalEntradas;

    public HashTable(@Value("${app.hash.tableSize:1543}") int tableSize) {
        this.tableSize     = tableSize;
        this.totalEntradas = 0;
        this.buckets       = new HashBucket[tableSize];
        for (int i = 0; i < tableSize; i++) {
            buckets[i] = new HashBucket();
        }
        log.info("HashTable inicializada | tableSize={}", tableSize);
    }

    // si el ID ya existe actualiza numRegistro (usado al actualizar una transacción)
    public void insert(String id, int numRegistro) {
        int index = hash(id);
        HashBucket bucket = buckets[index];

        // Actualizar si ya existe
        HashEntry curr = bucket.head;
        while (curr != null) {
            if (curr.idTransaccion.equals(id)) {
                curr.numRegistro = numRegistro;
                return;
            }
            curr = curr.siguiente;
        }

        // Insertar al inicio de la cadena (O(1))
        HashEntry nueva = new HashEntry(id, numRegistro);
        nueva.siguiente = bucket.head;
        bucket.head = nueva;
        bucket.size++;
        totalEntradas++;
    }

    // retorna el num_registro del ID, o -1 si no existe
    public int search(String id) {
        int index = hash(id);
        HashEntry curr = buckets[index].head;
        while (curr != null) {
            if (curr.idTransaccion.equals(id)) return curr.numRegistro;
            curr = curr.siguiente;
        }
        return -1;
    }

    // retorna true si existía y fue eliminada, false si no existía
    public boolean delete(String id) {
        int index = hash(id);
        HashBucket bucket = buckets[index];

        HashEntry prev = null;
        HashEntry curr = bucket.head;
        while (curr != null) {
            if (curr.idTransaccion.equals(id)) {
                if (prev == null) bucket.head = curr.siguiente;
                else prev.siguiente = curr.siguiente;
                bucket.size--;
                totalEntradas--;
                return true;
            }
            prev = curr;
            curr = curr.siguiente;
        }
        return false;
    }

    public boolean contains(String id) {
        return search(id) != -1;
    }

    // vacía la tabla; se usa al reiniciar el sistema con el endpoint seed
    public void limpiar() {
        for (int i = 0; i < tableSize; i++) {
            buckets[i] = new HashBucket();
        }
        totalEntradas = 0;
    }

    public int size() { return totalEntradas; }

    public double getLoadFactor() { return (double) totalEntradas / tableSize; }

    public int getTableSize() { return tableSize; }

    // cuántos buckets tienen al menos una entrada (sirve para medir colisiones)
    public int getBucketsOcupados() {
        int count = 0;
        for (HashBucket b : buckets) if (b.size > 0) count++;
        return count;
    }

    // h = (h * 31 + c) % tableSize para cada carácter del ID
    private int hash(String id) {
        int h = 0;
        for (char c : id.toCharArray()) {
            h = (h * 31 + c) % tableSize;
        }
        return Math.abs(h);
    }
}
