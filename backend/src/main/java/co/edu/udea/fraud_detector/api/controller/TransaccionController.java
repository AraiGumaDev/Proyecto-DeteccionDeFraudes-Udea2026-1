package co.edu.udea.fraud_detector.api.controller;

import co.edu.udea.fraud_detector.model.dto.PaginaDTO;
import co.edu.udea.fraud_detector.model.dto.TransaccionCompletaDTO;
import co.edu.udea.fraud_detector.model.dto.TransaccionInputDTO;
import co.edu.udea.fraud_detector.model.dto.TransaccionUpdateDTO;
import co.edu.udea.fraud_detector.service.TransaccionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/transacciones")
public class TransaccionController {

    private final TransaccionService transaccionService;

    public TransaccionController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    /** POST /api/v1/transacciones — crear transacción + KNN automático */
    @PostMapping
    public ResponseEntity<TransaccionCompletaDTO> crear(
            @RequestBody TransaccionInputDTO input) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transaccionService.crear(input));
    }

    /** GET /api/v1/transacciones — listar con filtros y paginación */
    @GetMapping
    public ResponseEntity<PaginaDTO<TransaccionCompletaDTO>> listar(
            @RequestParam(required = false) String numCuenta,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estadoAlerta,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamano) throws IOException {
        return ResponseEntity.ok(
                transaccionService.listar(numCuenta, tipo, estadoAlerta, pagina, tamano));
    }

    /** GET /api/v1/transacciones/{id} — buscar por ID vía Hash O(1) */
    @GetMapping("/{id}")
    public ResponseEntity<TransaccionCompletaDTO> buscarPorId(
            @PathVariable String id) throws IOException {
        return ResponseEntity.ok(transaccionService.buscarPorId(id));
    }

    /** PUT /api/v1/transacciones/{id} — actualizar monto/tipo/timestamp */
    @PutMapping("/{id}")
    public ResponseEntity<TransaccionCompletaDTO> actualizar(
            @PathVariable String id,
            @RequestBody TransaccionUpdateDTO input) throws IOException {
        return ResponseEntity.ok(transaccionService.actualizar(id, input));
    }

    /** DELETE /api/v1/transacciones/{id} — eliminación lógica */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) throws IOException {
        transaccionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
