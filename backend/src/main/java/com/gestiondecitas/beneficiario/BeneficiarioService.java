package com.gestiondecitas.beneficiario;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.beneficiario.dto.BeneficiarioRequest;
import com.gestiondecitas.beneficiario.dto.BeneficiarioResponse;
import com.gestiondecitas.negocio.Negocio;
import com.gestiondecitas.negocio.NegocioRepository;
import com.gestiondecitas.shared.error.RecursoNoEncontradoException;
import com.gestiondecitas.shared.error.ReglaNegocioException;
import com.gestiondecitas.usuario.AportaDatosPersonales;

/**
 * Menores a cargo de un cliente (CU-009, RN-007). Cada cliente solo ve y toca los
 * suyos: uno ajeno es un 404.
 */
@Service
public class BeneficiarioService implements AportaDatosPersonales {

    static final String YA_TIENE_LA_EDAD = "Ya tiene la edad para crear su propia cuenta.";

    private final BeneficiarioRepository beneficiarios;
    private final NegocioRepository negocios;
    private final CitasDeBeneficiario citas;
    private final Clock clock;

    public BeneficiarioService(BeneficiarioRepository beneficiarios, NegocioRepository negocios,
            CitasDeBeneficiario citas, Clock clock) {
        this.beneficiarios = beneficiarios;
        this.negocios = negocios;
        this.citas = citas;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<BeneficiarioResponse> listar(Long clienteId) {
        return beneficiarios.findByClienteIdAndEliminadoEnIsNullOrderByNombreAscApellidosAsc(clienteId).stream()
                .map(BeneficiarioResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public BeneficiarioResponse obtener(Long clienteId, Long id) {
        return BeneficiarioResponse.de(buscar(clienteId, id));
    }

    @Transactional
    public BeneficiarioResponse crear(Long clienteId, BeneficiarioRequest datos) {
        Beneficiario beneficiario = new Beneficiario(clienteId, datos.nombre().trim(), datos.apellidos().trim(),
                datos.fechaNacimiento());
        comprobarQueEsMenor(beneficiario);
        return BeneficiarioResponse.de(beneficiarios.save(beneficiario));
    }

    @Transactional
    public BeneficiarioResponse actualizar(Long clienteId, Long id, BeneficiarioRequest datos) {
        Beneficiario beneficiario = buscar(clienteId, id);
        beneficiario.actualizar(datos.nombre().trim(), datos.apellidos().trim(), datos.fechaNacimiento());
        comprobarQueEsMenor(beneficiario);
        return BeneficiarioResponse.de(beneficiario);
    }

    /**
     * Sin citas se borra la fila; con citas se anonimiza (son historial del negocio)
     * y se cancelan las que aún no han empezado.
     */
    @Transactional
    public void eliminar(Long clienteId, Long id) {
        Beneficiario beneficiario = buscar(clienteId, id);
        if (!citas.tieneCitas(id)) {
            beneficiarios.delete(beneficiario);
            return;
        }
        citas.cancelarFuturas(id, "Beneficiario eliminado", clienteId);
        beneficiario.anonimizar(clock.instant());
    }

    /**
     * Para reservar (CU-009): un beneficiario del cliente, no eliminado y que siga
     * siendo menor hoy (pudo cumplir años desde que se añadió).
     */
    @Transactional(readOnly = true)
    public Beneficiario paraReservar(Long clienteId, Long id) {
        Beneficiario beneficiario = buscar(clienteId, id);
        comprobarQueEsMenor(beneficiario);
        return beneficiario;
    }

    @Override
    public String apartado() {
        return "beneficiarios";
    }

    @Override
    public List<BeneficiarioResponse> exportar(Long clienteId) {
        return listar(clienteId);
    }

    /** Al eliminar la cuenta, los datos de sus menores también se anonimizan (RNF-007). */
    @Override
    public void alEliminarCuenta(Long clienteId, Instant ahora) {
        beneficiarios.findByClienteIdAndEliminadoEnIsNull(clienteId).forEach(b -> b.anonimizar(ahora));
    }

    private void comprobarQueEsMenor(Beneficiario beneficiario) {
        Negocio negocio = negocios.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No existe el registro del negocio (migración V2)."));
        LocalDate hoy = LocalDate.now(clock.withZone(negocio.getZonaHoraria()));
        if (!beneficiario.esMenor(hoy, negocio.getEdadMinimaCuenta())) {
            throw new ReglaNegocioException(YA_TIENE_LA_EDAD);
        }
    }

    private Beneficiario buscar(Long clienteId, Long id) {
        return beneficiarios.findByIdAndClienteIdAndEliminadoEnIsNull(id, clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El beneficiario no existe."));
    }
}
