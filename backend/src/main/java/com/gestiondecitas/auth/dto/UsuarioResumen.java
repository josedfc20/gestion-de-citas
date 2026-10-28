package com.gestiondecitas.auth.dto;

import com.gestiondecitas.usuario.Rol;
import com.gestiondecitas.usuario.Usuario;

/** Lo mínimo que la web necesita para saber a qué pantalla llevar al usuario. */
public record UsuarioResumen(Long id, String nombre, Rol rol, boolean debeCambiarPassword) {

    public static UsuarioResumen de(Usuario usuario) {
        return new UsuarioResumen(usuario.getId(), usuario.getNombre(), usuario.getRol(),
                usuario.debeCambiarPassword());
    }
}
