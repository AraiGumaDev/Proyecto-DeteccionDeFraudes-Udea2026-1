package co.edu.udea.fraud_detector.api;

import co.edu.udea.fraud_detector.model.dto.ErrorResponseDTO;
import co.edu.udea.fraud_detector.model.exception.TransaccionDuplicadaException;
import co.edu.udea.fraud_detector.model.exception.TransaccionNoEncontradaException;
import co.edu.udea.fraud_detector.model.exception.ValidacionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TransaccionNoEncontradaException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDTO handleNotFound(TransaccionNoEncontradaException e) {
        return ErrorResponseDTO.of("NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(TransaccionDuplicadaException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseDTO handleDuplicate(TransaccionDuplicadaException e) {
        return ErrorResponseDTO.of("CONFLICT", e.getMessage());
    }

    @ExceptionHandler(ValidacionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO handleValidacion(ValidacionException e) {
        return ErrorResponseDTO.of("BAD_REQUEST", e.getMessage());
    }

    // KDTree lanza esto cuando hay insuficientes nodos para KNN
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponseDTO handleIllegalState(IllegalStateException e) {
        return ErrorResponseDTO.of("UNPROCESSABLE", e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseDTO handleIO(IOException e) {
        log.error("Error de I/O en el archivo de datos", e);
        return ErrorResponseDTO.of("IO_ERROR", "Error accediendo al archivo de datos: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseDTO handleGeneric(Exception e) {
        log.error("Error inesperado", e);
        return ErrorResponseDTO.of("INTERNAL_ERROR", "Error interno del servidor: " + e.getMessage());
    }
}
