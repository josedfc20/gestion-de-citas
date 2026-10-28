package com.gestiondecitas.shared.demo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gestiondecitas.auth.PoliticaPassword;
import com.gestiondecitas.beneficiario.Beneficiario;
import com.gestiondecitas.beneficiario.BeneficiarioRepository;
import com.gestiondecitas.cita.Cita;
import com.gestiondecitas.cita.CitaRepository;
import com.gestiondecitas.empleado.EmpleadoRepository;
import com.gestiondecitas.horario.AjusteHorario;
import com.gestiondecitas.horario.AjusteHorarioRepository;
import com.gestiondecitas.horario.DiaCierre;
import com.gestiondecitas.horario.DiaCierreRepository;
import com.gestiondecitas.horario.Tramo;
import com.gestiondecitas.horario.TramoHorario;
import com.gestiondecitas.horario.TramoHorarioRepository;
import com.gestiondecitas.negocio.Negocio;
import com.gestiondecitas.negocio.NegocioRepository;
import com.gestiondecitas.servicio.Servicio;
import com.gestiondecitas.servicio.ServicioRepository;
import com.gestiondecitas.usuario.Cliente;
import com.gestiondecitas.usuario.Empleado;
import com.gestiondecitas.usuario.UsuarioRepository;

/**
 * Datos de demostración de la peluquería (perfil {@code demo}, decisión del 28/10):
 * servicios, tres profesionales, un cliente con una hija menor y dos citas, el
 * horario y algunos días de cierre.
 * Solo se crean si aún no hay ningún servicio, así reiniciar no duplica nada.
 * Las cuentas usan {@code DEMO_PASSWORD} y nunca se escribe ninguna contraseña.
 */
@Component
@Profile("demo")
public class DatosDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemo.class);

    private static final Tramo MANANA = tramo("09:00", "14:00");
    private static final Tramo TARDE = tramo("16:00", "20:00");
    private static final Map<MonthDay, String> FESTIVOS =
            Map.of(MonthDay.of(12, 25), "Navidad", MonthDay.of(1, 1), "Año Nuevo");

    private final NegocioRepository negocios;
    private final ServicioRepository servicios;
    private final EmpleadoRepository empleados;
    private final UsuarioRepository usuarios;
    private final TramoHorarioRepository tramos;
    private final AjusteHorarioRepository ajustes;
    private final DiaCierreRepository diasCierre;
    private final BeneficiarioRepository beneficiarios;
    private final CitaRepository citas;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String password;

    public DatosDemo(NegocioRepository negocios, ServicioRepository servicios, EmpleadoRepository empleados,
            UsuarioRepository usuarios, TramoHorarioRepository tramos, AjusteHorarioRepository ajustes,
            DiaCierreRepository diasCierre, BeneficiarioRepository beneficiarios, CitaRepository citas,
            PasswordEncoder passwordEncoder, Clock clock,
            @Value("${app.demo.password:}") String password) {
        this.negocios = negocios;
        this.servicios = servicios;
        this.empleados = empleados;
        this.usuarios = usuarios;
        this.tramos = tramos;
        this.ajustes = ajustes;
        this.diasCierre = diasCierre;
        this.beneficiarios = beneficiarios;
        this.citas = citas;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (servicios.count() > 0) {
            log.info("Ya hay servicios: no se crean los datos de demostración.");
            return;
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("El perfil demo necesita DEMO_PASSWORD en el .env "
                    + "(la contraseña de las cuentas de demostración).");
        }
        PoliticaPassword.incumplimiento(password).ifPresent(motivo -> {
            throw new IllegalStateException("DEMO_PASSWORD no cumple la política de contraseñas: " + motivo);
        });

        Negocio negocio = negocios.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No existe el registro del negocio (migración V2)."));
        negocio.actualizar("Peluquería Demo", negocio.getZonaHoraria(), negocio.getIntervaloMinutos(),
                negocio.getAntelacionMaxDias(), negocio.getHorasMinCancelacion(), negocio.getEdadMinimaCuenta());

        Servicio corte = servicio("Corte de pelo", "Lavado, corte y secado.", 30, "15.00");
        Servicio infantil = servicio("Corte infantil", "Para menores de 12 años.", 30, "10.00");
        Servicio peinado = servicio("Lavar y peinar", null, 30, "12.00");
        Servicio barba = servicio("Arreglo de barba", null, 30, "8.00");
        Servicio tinte = servicio("Tinte", "Color completo con un solo tono.", 90, "40.00");
        Servicio mechas = servicio("Mechas", null, 120, "55.00");

        Empleado luis = empleado("Luis", "García López", "luis@demo.invalid", List.of(corte, infantil, barba));
        Empleado ana = empleado("Ana", "Martín Ruiz", "ana@demo.invalid",
                List.of(corte, infantil, peinado, tinte, mechas));
        Empleado marta = empleado("Marta", "López Sanz", "marta@demo.invalid", List.of(peinado, tinte, mechas));
        Cliente carmen = usuarios.save(new Cliente("Carmen", "Pérez Gil", "cliente@demo.invalid", "600000000",
                passwordEncoder.encode(password), LocalDate.of(1990, 3, 15), clock.instant()));

        // Horario general: de lunes a viernes mañana y tarde; sábado por la mañana; domingo cerrado.
        Long negocioId = negocio.getId();
        Stream.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                .forEach(dia -> {
                    tramos.save(new TramoHorario(negocioId, null, dia, MANANA));
                    tramos.save(new TramoHorario(negocioId, null, dia, TARDE));
                });
        tramos.save(new TramoHorario(negocioId, null, DayOfWeek.SATURDAY, MANANA));

        // Ajustes (RN-002): Ana libra los lunes y Marta entra más tarde los sábados.
        ajustes.save(new AjusteHorario(ana.getId(), DayOfWeek.MONDAY));
        ajustes.save(new AjusteHorario(marta.getId(), DayOfWeek.SATURDAY));
        tramos.save(new TramoHorario(negocioId, marta.getId(), DayOfWeek.SATURDAY, tramo("10:00", "14:00")));

        // Días de cierre: los próximos Navidad y Año Nuevo, y una semana de vacaciones de Luis.
        LocalDate hoy = LocalDate.now(clock.withZone(negocio.getZonaHoraria()));
        FESTIVOS.forEach((dia, motivo) ->
                diasCierre.save(new DiaCierre(negocioId, null, siguiente(dia, hoy), motivo)));
        LocalDate vacaciones = hoy.plusDays(30);
        for (int i = 0; i < 7; i++) {
            diasCierre.save(new DiaCierre(negocioId, luis.getId(), vacaciones.plusDays(i), "Vacaciones"));
        }

        // Una hija menor de Carmen (CU-009) y una cita para cada una, en el primer día laborable
        // a partir de pasado mañana que no sea festivo (Ana libra los lunes).
        Beneficiario lucia = beneficiarios.save(new Beneficiario(carmen.getId(), "Lucía", "Pérez Gil",
                hoy.minusYears(8)));
        LocalDate diaCitas = hoy.plusDays(2);
        while (diaCitas.getDayOfWeek() == DayOfWeek.MONDAY || diaCitas.getDayOfWeek().getValue() > 5
                || FESTIVOS.containsKey(MonthDay.from(diaCitas))) {
            diaCitas = diaCitas.plusDays(1);
        }
        var zona = negocio.getZonaHoraria();
        citas.save(Cita.reservar(carmen, marta, peinado, null,
                diaCitas.atTime(10, 0).atZone(zona).toInstant(), carmen.getId(), clock.instant()));
        citas.save(Cita.reservar(carmen, ana, infantil, lucia,
                diaCitas.atTime(11, 0).atZone(zona).toInstant(), carmen.getId(), clock.instant()));
        log.info("Datos de demostración creados: 6 servicios, 3 profesionales, el cliente cliente@demo.invalid "
                + "y 2 citas.");
    }

    private Servicio servicio(String nombre, String descripcion, int minutos, String precio) {
        return servicios.save(new Servicio(nombre, descripcion, minutos, new BigDecimal(precio)));
    }

    /** Sin contraseña temporal: en la demo se entra directamente con {@code DEMO_PASSWORD}. */
    private Empleado empleado(String nombre, String apellidos, String email, List<Servicio> asignados) {
        String hash = passwordEncoder.encode(password);
        Empleado empleado = new Empleado(nombre, apellidos, email, null, hash, clock.instant());
        empleado.cambiarPassword(hash);
        empleado.asignarServicios(asignados);
        return empleados.save(empleado);
    }

    private static LocalDate siguiente(MonthDay dia, LocalDate hoy) {
        LocalDate esteAno = dia.atYear(hoy.getYear());
        return esteAno.isBefore(hoy) ? esteAno.plusYears(1) : esteAno;
    }

    private static Tramo tramo(String inicio, String fin) {
        return new Tramo(LocalTime.parse(inicio), LocalTime.parse(fin));
    }
}
