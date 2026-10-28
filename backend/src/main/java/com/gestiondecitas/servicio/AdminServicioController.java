package com.gestiondecitas.servicio;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.servicio.dto.ServicioAdminResponse;
import com.gestiondecitas.servicio.dto.ServicioRequest;
import com.gestiondecitas.shared.dto.CambioActivoRequest;

@Tag(name = "Admin · Servicios", description = "Gestionar servicios y precios (CU-016)")
@RestController
@RequestMapping("/api/v1/admin/servicios")
public class AdminServicioController {

    private final ServicioService servicioService;

    public AdminServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @Operation(summary = "Todos los servicios, activos e inactivos")
    @GetMapping
    public List<ServicioAdminResponse> listar() {
        return servicioService.listarTodos();
    }

    @Operation(summary = "Un servicio")
    @GetMapping("/{id}")
    public ServicioAdminResponse obtener(@PathVariable Long id) {
        return servicioService.obtener(id);
    }

    @Operation(summary = "Crear un servicio",
            description = "La duración debe ser múltiplo del intervalo de reserva (RN-011); si no, 422.")
    @PostMapping
    public ResponseEntity<ServicioAdminResponse> crear(@Valid @RequestBody ServicioRequest datos) {
        ServicioAdminResponse creado = servicioService.crear(datos);
        return ResponseEntity.created(URI.create("/api/v1/admin/servicios/" + creado.id())).body(creado);
    }

    @Operation(summary = "Modificar un servicio", description = "Solo afecta a las reservas nuevas.")
    @PutMapping("/{id}")
    public ServicioAdminResponse actualizar(@PathVariable Long id, @Valid @RequestBody ServicioRequest datos) {
        return servicioService.actualizar(id, datos);
    }

    @Operation(summary = "Activar o desactivar un servicio",
            description = "Desactivado deja de verse y de poder reservarse; las citas ya confirmadas se mantienen.")
    @PatchMapping("/{id}/activo")
    public ServicioAdminResponse cambiarActivo(@PathVariable Long id, @Valid @RequestBody CambioActivoRequest datos) {
        return servicioService.cambiarActivo(id, datos.activo());
    }
}
