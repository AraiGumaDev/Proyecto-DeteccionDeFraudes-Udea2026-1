package co.edu.udea.fraud_detector.api.controller;

import co.edu.udea.fraud_detector.model.dto.TransaccionCompletaDTO;
import co.edu.udea.fraud_detector.service.AlertaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/alertas")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    /** GET /api/v1/alertas — listar alertas activas (estado MEDIA o ALTA) */
    @GetMapping
    public ResponseEntity<List<TransaccionCompletaDTO>> listarActivas() throws IOException {
        return ResponseEntity.ok(alertaService.listarActivas());
    }

    /** PATCH /api/v1/alertas/{id}/confirmar — marcar como CONFIRMADO_FRAUDE */
    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<TransaccionCompletaDTO> confirmar(
            @PathVariable String id) throws IOException {
        return ResponseEntity.ok(alertaService.confirmar(id));
    }

    /** PATCH /api/v1/alertas/{id}/descartar — marcar como FALSO_POSITIVO */
    @PatchMapping("/{id}/descartar")
    public ResponseEntity<TransaccionCompletaDTO> descartar(
            @PathVariable String id) throws IOException {
        return ResponseEntity.ok(alertaService.descartar(id));
    }
}
