package com.gestiondecitas.empleado;

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

import com.gestiondecitas.empleado.dto.AltaEmpleadoRequest;
import com.gestiondecitas.empleado.dto.DatosEmpleadoRequest;
import com.gestiondecitas.empleado.dto.EmpleadoAdminResponse;
import com.gestiondecitas.empleado.dto.EmpleadoCreadoResponse;
import com.gestiondecitas.empleado.dto.PasswordTemporalResponse;
import com.gestiondecitas.empleado.dto.ServiciosAsignadosRequest;
import com.gestiondecitas.shared.dto.CambioActivoRequest;

@Tag(name = "Admin · Empleados", description = "Gestionar empleados y sus servicios (CU-017, RN-005)")
@RestController
@RequestMapping("/api/v1/admin/empleados")
public class AdminEmpleadoController {

    private final EmpleadoService empleadoService;

    public AdminEmpleadoController(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    @Operation(summary = "Todos los empleados, activos e inactivos")
    @GetMapping
    public List<EmpleadoAdminResponse> listar() {
        return empleadoService.listar();
    }

    @Operation(summary = "Un empleado")
    @GetMapping("/{id}")
    public EmpleadoAdminResponse obtener(@PathVariable Long id) {
        return empleadoService.obtener(id);
    }

    @Operation(summary = "Dar de alta un empleado",
            description = "Devuelve la contraseña temporal UNA sola vez; el empleado la cambia al entrar (RN-005).")
    @PostMapping
    public ResponseEntity<EmpleadoCreadoResponse> alta(@Valid @RequestBody AltaEmpleadoRequest datos) {
        EmpleadoCreadoResponse creado = empleadoService.alta(datos);
        return ResponseEntity.created(URI.create("/api/v1/admin/empleados/" + creado.empleado().id())).body(creado);
    }

    @Operation(summary = "Modificar los datos de un empleado")
    @PutMapping("/{id}")
    public EmpleadoAdminResponse actualizar(@PathVariable Long id, @Valid @RequestBody DatosEmpleadoRequest datos) {
        return empleadoService.actualizar(id, datos);
    }

    @Operation(summary = "Asignar los servicios de un empleado",
            description = "Reemplaza la lista. Si quitas un servicio con citas futuras, se mantienen y llega un aviso.")
    @PutMapping("/{id}/servicios")
    public EmpleadoAdminResponse asignarServicios(@PathVariable Long id,
            @Valid @RequestBody ServiciosAsignadosRequest datos) {
        return empleadoService.asignarServicios(id, datos.servicioIds());
    }

    @Operation(summary = "Activar o desactivar un empleado",
            description = "409 si tiene citas futuras confirmadas (citasFuturas). Al desactivar se cierran sus sesiones.")
    @PatchMapping("/{id}/activo")
    public EmpleadoAdminResponse cambiarActivo(@PathVariable Long id, @Valid @RequestBody CambioActivoRequest datos) {
        return empleadoService.cambiarActivo(id, datos.activo());
    }

    @Operation(summary = "Generar otra contraseña temporal",
            description = "La anterior deja de valer y se cierran sus sesiones. Se devuelve una sola vez.")
    @PostMapping("/{id}/password-temporal")
    public PasswordTemporalResponse nuevaPasswordTemporal(@PathVariable Long id) {
        return empleadoService.nuevaPasswordTemporal(id);
    }
}
