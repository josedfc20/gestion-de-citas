package com.gestiondecitas.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gestiondecitas.disponibilidad.CalculoDisponibilidad.Ocupacion;
import com.gestiondecitas.horario.Tramo;

/** CU-007: el cálculo de horas libres del diagrama de actividad, sin base de datos. */
class CalculoDisponibilidadTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    /** Un martes de invierno (UTC+1). */
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 3);
    private static final Instant ANTES = Instant.parse("2026-11-01T08:00:00Z");

    private final CalculoDisponibilidad conIntervalo30 = new CalculoDisponibilidad(30, MADRID, ANTES);

    @Test
    void generaHorasCadaIntervaloEnCadaTramo() {
        List<LocalTime> horas = conIntervalo30.horas(FECHA,
                List.of(tramo("16:00", "17:30"), tramo("09:00", "11:00")), 30, List.of());

        assertThat(horas).containsExactlyElementsOf(horas("09:00", "09:30", "10:00", "10:30",
                "16:00", "16:30", "17:00"));
    }

    @Test
    void elServicioTieneQueCaberEnteroEnElTramo() {
        assertThat(conIntervalo30.horas(FECHA, List.of(tramo("09:00", "11:00")), 60, List.of()))
                .containsExactlyElementsOf(horas("09:00", "09:30", "10:00"));
        assertThat(conIntervalo30.horas(FECHA, List.of(tramo("09:00", "11:00")), 90, List.of()))
                .containsExactlyElementsOf(horas("09:00", "09:30"));
        assertThat(conIntervalo30.horas(FECHA, List.of(tramo("09:00", "10:00")), 90, List.of())).isEmpty();
    }

    @Test
    void lasHorasVanAlineadasAlRelojAunqueElTramoEmpieceDescuadrado() {
        assertThat(conIntervalo30.horas(FECHA, List.of(tramo("09:15", "11:00")), 30, List.of()))
                .containsExactlyElementsOf(horas("09:30", "10:00", "10:30"));
    }

    @Test
    void respetaOtrosIntervalos() {
        CalculoDisponibilidad cada15 = new CalculoDisponibilidad(15, MADRID, ANTES);
        assertThat(cada15.horas(FECHA, List.of(tramo("09:00", "10:00")), 30, List.of()))
                .containsExactlyElementsOf(horas("09:00", "09:15", "09:30"));

        CalculoDisponibilidad cada60 = new CalculoDisponibilidad(60, MADRID, ANTES);
        assertThat(cada60.horas(FECHA, List.of(tramo("09:30", "13:00")), 60, List.of()))
                .containsExactlyElementsOf(horas("10:00", "11:00", "12:00"));
    }

    @Test
    void unaCitaBloqueaLasHorasQueSeSolapanPeroNoLaQueEmpiezaAlAcabar() {
        // Cita de 10:00 a 11:00 (hora de Madrid); el servicio dura 60 minutos.
        List<Ocupacion> citas = List.of(ocupacion("10:00", "11:00"));

        assertThat(conIntervalo30.horas(FECHA, List.of(tramo("09:00", "13:00")), 60, citas))
                .containsExactlyElementsOf(horas("09:00", "11:00", "11:30", "12:00"));
    }

    @Test
    void citasSeguidasDejanElHuecoJustoEntreEllas() {
        List<Ocupacion> citas = List.of(ocupacion("09:00", "10:00"), ocupacion("10:30", "12:00"));

        assertThat(conIntervalo30.horas(FECHA, List.of(tramo("09:00", "12:00")), 30, citas))
                .containsExactlyElementsOf(horas("10:00"));
        assertThat(conIntervalo30.horas(FECHA, List.of(tramo("09:00", "12:00")), 60, citas)).isEmpty();
    }

    @Test
    void hoyNoOfreceLasHorasQueYaHanEmpezado() {
        // Son las 10:10 en Madrid (09:10 UTC).
        Instant ahora = ZonedDateTime.of(FECHA, LocalTime.of(10, 10), MADRID).toInstant();
        CalculoDisponibilidad hoy = new CalculoDisponibilidad(30, MADRID, ahora);

        assertThat(hoy.horas(FECHA, List.of(tramo("09:00", "12:00")), 30, List.of()))
                .containsExactlyElementsOf(horas("10:30", "11:00", "11:30"));
    }

    @Test
    void unaHoraQueEmpiezaJustoAhoraTampocoSeOfrece() {
        Instant ahora = ZonedDateTime.of(FECHA, LocalTime.of(10, 0), MADRID).toInstant();
        CalculoDisponibilidad hoy = new CalculoDisponibilidad(30, MADRID, ahora);

        assertThat(hoy.horas(FECHA, List.of(tramo("09:00", "11:00")), 30, List.of()))
                .containsExactlyElementsOf(horas("10:30"));
    }

    @Test
    void sinTramosNoHayHoras() {
        assertThat(conIntervalo30.horas(FECHA, List.of(), 30, List.of())).isEmpty();
    }

    @Test
    void elDiaDelCambioDeHoraUsaLaZonaCorrecta() {
        // 25/10/2026: a las 03:00 los relojes vuelven a las 02:00. Por la mañana ya es UTC+1.
        LocalDate cambio = LocalDate.of(2026, 10, 25);
        Instant ahora = Instant.parse("2026-10-24T08:00:00Z");
        CalculoDisponibilidad calculo = new CalculoDisponibilidad(30, MADRID, ahora);
        // Cita de 09:00 a 09:30 hora local = 08:00-08:30 UTC.
        List<Ocupacion> citas = List.of(new Ocupacion(Instant.parse("2026-10-25T08:00:00Z"),
                Instant.parse("2026-10-25T08:30:00Z")));

        assertThat(calculo.horas(cambio, List.of(tramo("09:00", "10:30")), 30, citas))
                .containsExactlyElementsOf(horas("09:30", "10:00"));
    }

    private static Ocupacion ocupacion(String inicio, String fin) {
        return new Ocupacion(ZonedDateTime.of(FECHA, LocalTime.parse(inicio), MADRID).toInstant(),
                ZonedDateTime.of(FECHA, LocalTime.parse(fin), MADRID).toInstant());
    }

    private static Tramo tramo(String inicio, String fin) {
        return new Tramo(LocalTime.parse(inicio), LocalTime.parse(fin));
    }

    private static List<LocalTime> horas(String... horas) {
        return Arrays.stream(horas).map(LocalTime::parse).toList();
    }
}
