package com.gestiondecitas.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Los correos se guardan normalizados ({@link Usuario#normalizarEmail}). */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String emailNormalizado);

    boolean existsByEmail(String emailNormalizado);

    boolean existsByEmailAndIdNot(String emailNormalizado, Long id);

    /**
     * CU-019, alt. 1: clientes activos cuyo nombre completo, correo o teléfono
     * contiene el texto. {@code patron} ya va en minúsculas, entre {@code %} y con
     * {@code !} como carácter de escape.
     */
    @Query("""
            SELECT c FROM Cliente c
            WHERE c.activo = true
              AND (LOWER(CONCAT(c.nombre, ' ', c.apellidos)) LIKE :patron ESCAPE '!'
                   OR c.email LIKE :patron ESCAPE '!'
                   OR c.telefono LIKE :patron ESCAPE '!')
            ORDER BY c.nombre, c.apellidos, c.id""")
    List<Cliente> buscarClientes(@Param("patron") String patron, Limit limite);
}
