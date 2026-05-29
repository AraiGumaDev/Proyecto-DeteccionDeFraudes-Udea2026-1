package co.edu.udea.fraud_detector.api.controller;

import co.edu.udea.fraud_detector.model.dto.EstadisticasDTO;
import co.edu.udea.fraud_detector.model.dto.SaludDTO;
import co.edu.udea.fraud_detector.service.SistemaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sistema")
public class SistemaController {

    private final SistemaService sistemaService;

    public SistemaController(SistemaService sistemaService) {
        this.sistemaService = sistemaService;
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<EstadisticasDTO> estadisticas() throws IOException {
        return ResponseEntity.ok(sistemaService.estadisticas());
    }

    @GetMapping("/salud")
    public ResponseEntity<SaludDTO> salud() {
        return ResponseEntity.ok(sistemaService.salud());
    }

    // forzar=true reinicia todo antes de sembrar; forzar=false solo siembra si el archivo está vacío
    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seed(
            @RequestParam(defaultValue = "false") boolean forzar) throws IOException {
        return ResponseEntity.ok(sistemaService.seed(forzar));
    }
}
