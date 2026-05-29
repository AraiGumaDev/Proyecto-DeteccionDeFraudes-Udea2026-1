package co.edu.udea.fraud_detector.model.dto;

/**
 * Un resultado de búsqueda KNN: transacción vecina + distancia euclidiana 5D.
 */
public class VecinoDTO {

    public int                  posicion;     // 1 = vecino más cercano
    public double               distancia;
    public TransaccionCompletaDTO transaccion;

    public static VecinoDTO of(int posicion, double distancia, TransaccionCompletaDTO transaccion) {
        VecinoDTO v = new VecinoDTO();
        v.posicion    = posicion;
        v.distancia   = distancia;
        v.transaccion = transaccion;
        return v;
    }
}
