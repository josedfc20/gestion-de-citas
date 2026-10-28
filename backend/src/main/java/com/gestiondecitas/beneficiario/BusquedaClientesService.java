package com.gestiondecitas.beneficiario;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.beneficiario.dto.BeneficiarioResponse;
import com.gestiondecitas.beneficiario.dto.ClienteEncontradoResponse;
import com.gestiondecitas.shared.error.PeticionNoValidaException;
import com.gestiondecitas.usuario.Cliente;
import com.gestiondecitas.usuario.UsuarioRepository;

/**
 * CU-019, alt. 1: el administrador busca al cliente para reservar en su nombre.
 * Vive aquí y no en {@code usuario} porque devuelve también sus beneficiarios, y
 * {@code usuario} no depende de este paquete.
 */
@Service
public class BusquedaClientesService {

    static final int MIN_LETRAS = 3;
    static final int MAX_RESULTADOS = 20;

    private final UsuarioRepository usuarios;
    private final BeneficiarioRepository beneficiarios;

    public BusquedaClientesService(UsuarioRepository usuarios, BeneficiarioRepository beneficiarios) {
        this.usuarios = usuarios;
        this.beneficiarios = beneficiarios;
    }

    @Transactional(readOnly = true)
    public List<ClienteEncontradoResponse> buscar(String texto) {
        String limpio = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        if (limpio.length() < MIN_LETRAS) {
            throw new PeticionNoValidaException("Escribe al menos " + MIN_LETRAS + " letras para buscar.");
        }
        String patron = "%" + limpio.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        List<Cliente> clientes = usuarios.buscarClientes(patron, Limit.of(MAX_RESULTADOS));
        Map<Long, List<BeneficiarioResponse>> suyos = beneficiarios
                .findByClienteIdInAndEliminadoEnIsNullOrderByNombreAscApellidosAsc(
                        clientes.stream().map(Cliente::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(Beneficiario::getClienteId,
                        Collectors.mapping(BeneficiarioResponse::de, Collectors.toList())));
        return clientes.stream()
                .map(c -> new ClienteEncontradoResponse(c.getId(), c.getNombre(), c.getApellidos(), c.getEmail(),
                        c.getTelefono(), suyos.getOrDefault(c.getId(), List.of())))
                .toList();
    }
}
