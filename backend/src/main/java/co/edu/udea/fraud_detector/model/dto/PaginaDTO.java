package co.edu.udea.fraud_detector.model.dto;

import java.util.List;

/**
 * Respuesta paginada genérica.
 * totalFiltrados indica cuántos registros coinciden con los filtros aplicados
 * (sin paginación), lo que permite al frontend calcular el total de páginas.
 */
public class PaginaDTO<T> {
    public List<T> contenido;
    public int     totalFiltrados;  // total que coincide con los filtros
    public int     pagina;          // página actual (base 0)
    public int     tamano;          // registros solicitados por página
}
