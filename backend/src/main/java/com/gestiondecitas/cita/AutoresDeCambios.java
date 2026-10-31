package com.gestiondecitas.cita;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.gestiondecitas.empleado.EmpleadoService;
import com.gestiondecitas.usuario.Usuario;
import com.gestiondecitas.usuario.UsuarioRepository;

/**
 * Nombre de quien hizo cada cambio del historial de una cita (CU-019, decisión 3
 * del B9): "Cliente", el nombre público del empleado ("Luis G.") o
 * "Administrador". Así el admin no ve solo un id.
 */
@Component
class AutoresDeCambios {

    private final UsuarioRepository usuarios;

    AutoresDeCambios(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /** Una sola consulta por cita con los usuarios que aparecen en su historial. */
    Map<Long, String> de(Cita cita) {
        Set<Long> ids = cita.getCambios().stream().map(CambioEstadoCita::getRealizadoPor).collect(Collectors.toSet());
        Map<Long, String> nombres = new HashMap<>();
        usuarios.findAllById(ids).forEach(usuario -> nombres.put(usuario.getId(), nombre(usuario)));
        return nombres;
    }

    static String nombre(Usuario usuario) {
        return switch (usuario.getRol()) {
            case CLIENTE -> "Cliente";
            case EMPLEADO -> EmpleadoService.nombrePublico(usuario.getNombre(), usuario.getApellidos());
            case ADMIN -> "Administrador";
        };
    }
}
