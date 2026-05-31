package co.edu.udea.fraud_detector.api.controller;

import co.edu.udea.fraud_detector.model.dto.AlertaListaDTO;
import co.edu.udea.fraud_detector.model.dto.TransaccionCompletaDTO;
import co.edu.udea.fraud_detector.service.AlertaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/alertas")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    /** GET /api/v1/alertas?nivel_minimo=1 — listar alertas activas (MEDIA=1, ALTA=2) */
    @GetMapping
    public ResponseEntity<AlertaListaDTO> listarActivas(
            @RequestParam(defaultValue = "1") int nivelMinimo) throws IOException {
        return ResponseEntity.ok(alertaService.listarActivas(nivelMinimo));
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
