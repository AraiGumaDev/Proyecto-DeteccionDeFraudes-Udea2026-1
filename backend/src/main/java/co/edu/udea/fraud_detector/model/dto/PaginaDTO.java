package co.edu.udea.fraud_detector.model.dto;

import java.util.List;

/**
 * Respuesta paginada genérica.
 * totalElementos indica cuántos registros coinciden con los filtros (sin paginación).
 */
public class PaginaDTO<T> {
    public List<T> transacciones;   // contenido de la página
    public int     totalElementos;  // total que coincide con los filtros
    public int     paginaActual;    // página actual (base 0)
    public int     totalPaginas;    // ceil(totalElementos / tamanoPagina)
}
