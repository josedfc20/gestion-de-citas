package com.gestiondecitas.usuario;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.shared.error.DatoDuplicadoException;
import com.gestiondecitas.shared.error.RecursoNoEncontradoException;
import com.gestiondecitas.usuario.dto.ActualizarPerfilRequest;
import com.gestiondecitas.usuario.dto.PerfilResponse;

/** Perfil del usuario actual (CU-004). El cambio de contraseña está en {@code AuthService}. */
@Service
public class PerfilService {

    private static final String CORREO_EN_USO = "Ese correo ya lo usa otra cuenta.";

    private final UsuarioRepository usuarios;

    public PerfilService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public PerfilResponse obtener(Long usuarioId) {
        return PerfilResponse.de(buscar(usuarioId));
    }

    @Transactional
    public PerfilResponse actualizar(Long usuarioId, ActualizarPerfilRequest datos) {
        Usuario usuario = buscar(usuarioId);
        String email = Usuario.normalizarEmail(datos.email());
        if (usuarios.existsByEmailAndIdNot(email, usuarioId)) {
            throw new DatoDuplicadoException(CORREO_EN_USO);
        }
        String telefono = datos.telefono() == null || datos.telefono().isBlank() ? null : datos.telefono().trim();
        usuario.actualizarPerfil(datos.nombre().trim(), datos.apellidos().trim(), email, telefono);
        try {
            usuarios.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new DatoDuplicadoException(CORREO_EN_USO);
        }
        return PerfilResponse.de(usuario);
    }

    private Usuario buscar(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
    }
}
