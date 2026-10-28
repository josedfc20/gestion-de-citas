package com.gestiondecitas.usuario.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.gestiondecitas.usuario.Cliente;
import com.gestiondecitas.usuario.Rol;
import com.gestiondecitas.usuario.Usuario;

/** Datos del usuario actual ({@code GET /me}). La fecha de nacimiento solo existe para clientes. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PerfilResponse(Long id, Rol rol, String nombre, String apellidos, String email, String telefono,
        LocalDate fechaNacimiento, boolean debeCambiarPassword) {

    public static PerfilResponse de(Usuario usuario) {
        LocalDate nacimiento = usuario instanceof Cliente cliente ? cliente.getFechaNacimiento() : null;
        return new PerfilResponse(usuario.getId(), usuario.getRol(), usuario.getNombre(), usuario.getApellidos(),
                usuario.getEmail(), usuario.getTelefono(), nacimiento, usuario.debeCambiarPassword());
    }
}
