package co.edu.udea.fraud_detector.model.dto;

/**
 * Respuesta uniforme para todos los errores de la API.
 */
public class ErrorResponseDTO {
    public String codigo;
    public String mensaje;

    public static ErrorResponseDTO of(String codigo, String mensaje) {
        ErrorResponseDTO dto = new ErrorResponseDTO();
        dto.codigo  = codigo;
        dto.mensaje = mensaje;
        return dto;
    }
}
