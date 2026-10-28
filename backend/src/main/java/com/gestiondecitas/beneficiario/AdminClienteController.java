package com.gestiondecitas.beneficiario;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gestiondecitas.beneficiario.dto.ClienteEncontradoResponse;

@Tag(name = "Admin · Clientes", description = "Buscar al cliente para reservar en su nombre (CU-019)")
@RestController
@RequestMapping("/api/v1/admin/clientes")
public class AdminClienteController {

    private final BusquedaClientesService busquedaClientes;

    public AdminClienteController(BusquedaClientesService busquedaClientes) {
        this.busquedaClientes = busquedaClientes;
    }

    @Operation(summary = "Buscar clientes",
            description = "Por nombre, correo o teléfono (al menos 3 letras). Como mucho 20, con sus menores a cargo.")
    @GetMapping
    public List<ClienteEncontradoResponse> buscar(@RequestParam String buscar) {
        return busquedaClientes.buscar(buscar);
    }
}
