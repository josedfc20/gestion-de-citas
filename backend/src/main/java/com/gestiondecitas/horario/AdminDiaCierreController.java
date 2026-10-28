package com.gestiondecitas.horario;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.horario.dto.DiaCierreRequest;
import com.gestiondecitas.horario.dto.DiaCierreResponse;

@Tag(name = "Admin · Días de cierre", description = "Festivos del negocio y vacaciones de empleados (CU-018)")
@RestController
@RequestMapping("/api/v1/admin/dias-cierre")
public class AdminDiaCierreController {

    private final DiaCierreService diaCierreService;

    public AdminDiaCierreController(DiaCierreService diaCierreService) {
        this.diaCierreService = diaCierreService;
    }

    @Operation(summary = "Días de cierre del negocio y de empleados", description = "Sin desde, de hoy en adelante.")
    @GetMapping
    public List<DiaCierreResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return diaCierreService.listar(desde, hasta);
    }

    @Operation(summary = "Un día de cierre")
    @GetMapping("/{id}")
    public DiaCierreResponse obtener(@PathVariable Long id) {
        return diaCierreService.obtener(id);
    }

    @Operation(summary = "Añadir un día de cierre",
            description = "Sin empleadoId cierra todo el negocio. Las citas confirmadas de ese día llegan en "
                    + "citasAfectadas y se mantienen.")
    @PostMapping
    public ResponseEntity<DiaCierreResponse> crear(@Valid @RequestBody DiaCierreRequest datos) {
        DiaCierreResponse creado = diaCierreService.crear(datos);
        return ResponseEntity.created(URI.create("/api/v1/admin/dias-cierre/" + creado.id())).body(creado);
    }

    @Operation(summary = "Quitar un día de cierre")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable Long id) {
        diaCierreService.borrar(id);
        return ResponseEntity.noContent().build();
    }
}
