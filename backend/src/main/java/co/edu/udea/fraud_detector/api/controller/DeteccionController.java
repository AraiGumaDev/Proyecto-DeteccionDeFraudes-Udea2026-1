package co.edu.udea.fraud_detector.api.controller;

import co.edu.udea.fraud_detector.model.dto.AnalisisResultadoDTO;
import co.edu.udea.fraud_detector.model.dto.RangoBusquedaDTO;
import co.edu.udea.fraud_detector.model.dto.RangoResultadoDTO;
import co.edu.udea.fraud_detector.model.dto.TransaccionCompletaDTO;
import co.edu.udea.fraud_detector.model.dto.VecinosResultadoDTO;
import co.edu.udea.fraud_detector.service.DeteccionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/deteccion")
public class DeteccionController {

    private final DeteccionService deteccionService;

    public DeteccionController(DeteccionService deteccionService) {
        this.deteccionService = deteccionService;
    }

    /** POST /api/v1/deteccion/analizar/{id}?k=5 — re-ejecutar KNN sobre transacción existente */
    @PostMapping("/analizar/{id}")
    public ResponseEntity<AnalisisResultadoDTO> analizar(
            @PathVariable String id,
            @RequestParam(defaultValue = "5") int k) throws IOException {
        return ResponseEntity.ok(deteccionService.analizar(id, k));
    }

    /** GET /api/v1/deteccion/vecinos/{id}?k=5 — obtener K vecinos más cercanos */
    @GetMapping("/vecinos/{id}")
    public ResponseEntity<VecinosResultadoDTO> obtenerVecinos(
            @PathVariable String id,
            @RequestParam(defaultValue = "5") int k) throws IOException {
        return ResponseEntity.ok(deteccionService.obtenerVecinos(id, k));
    }

    /** POST /api/v1/deteccion/rango — búsqueda por hipercubo 5D */
    @PostMapping("/rango")
    public ResponseEntity<RangoResultadoDTO> buscarRango(
            @RequestBody RangoBusquedaDTO rango) {
        return ResponseEntity.ok(deteccionService.buscarRango(rango));
    }

    /**
     * POST /api/v1/deteccion/escaneo-masivo
     * Re-clasifica todas las transacciones NORMAL y MEDIA con el estado actual del árbol.
     */
    @PostMapping("/escaneo-masivo")
    public ResponseEntity<Map<String, Object>> escaneoMasivo() throws IOException {
        Map<String, Object> resultado = deteccionService.escaneoMasivo();
        return ResponseEntity.ok(resultado);
    }
}
