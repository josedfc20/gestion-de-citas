package com.gestiondecitas.negocio;

import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.negocio.dto.NegocioRequest;
import com.gestiondecitas.negocio.dto.NegocioResponse;
import com.gestiondecitas.servicio.DuracionServicio;
import com.gestiondecitas.servicio.Servicio;
import com.gestiondecitas.servicio.ServicioRepository;
import com.gestiondecitas.shared.error.ReglaNegocioException;

/** Configuración del negocio: intervalo, antelación, plazo de cancelación y edad mínima. */
@Service
public class NegocioService {

    private final NegocioRepository negocios;
    private final ServicioRepository servicios;

    public NegocioService(NegocioRepository negocios, ServicioRepository servicios) {
        this.negocios = negocios;
        this.servicios = servicios;
    }

    @Transactional(readOnly = true)
    public NegocioResponse obtener() {
        return NegocioResponse.de(negocios.findFirstByOrderByIdAsc().orElseThrow(NegocioService::sinNegocio));
    }

    /**
     * RN-011: el intervalo no puede cambiar si algún servicio (activo o no) dejaría
     * de ser múltiplo; así el catálogo nunca queda en un estado no válido.
     */
    @Transactional
    public NegocioResponse actualizar(NegocioRequest datos) {
        Negocio negocio = negocios.findFirstForUpdateByOrderByIdAsc().orElseThrow(NegocioService::sinNegocio);
        int intervalo = datos.intervaloMinutos();
        List<Servicio> incompatibles = servicios.findAllByOrderByNombreAsc().stream()
                .filter(s -> !DuracionServicio.esMultiplo(s.getDuracionMinutos(), intervalo))
                .toList();
        if (!incompatibles.isEmpty()) {
            String lista = incompatibles.stream()
                    .map(s -> s.getNombre() + " (" + s.getDuracionMinutos() + ")")
                    .collect(Collectors.joining(", "));
            throw new ReglaNegocioException("Estos servicios no son múltiplo de " + intervalo + " minutos: "
                    + lista + ". Cambia su duración antes.");
        }
        negocio.actualizar(datos.nombre().trim(), ZoneId.of(datos.zonaHoraria().trim()), intervalo,
                datos.antelacionMaxDias(), datos.horasMinCancelacion(), datos.edadMinimaCuenta());
        return NegocioResponse.de(negocio);
    }

    private static IllegalStateException sinNegocio() {
        return new IllegalStateException("No existe el registro del negocio (migración V2).");
    }
}
