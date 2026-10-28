package com.gestiondecitas.horario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gestiondecitas.cita.CitaOcupada;
import com.gestiondecitas.shared.error.ReglaNegocioException;

/** RN-002 (horario general y ajustes), CU-018 paso 4 (tramos) y alt. 1 (citas afectadas). */
class HorarioSemanalTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final Tramo MANANA = tramo("09:00", "14:00");
    private static final Tramo TARDE = tramo("16:00", "20:00");

    private final Map<DayOfWeek, List<Tramo>> general = Map.of(
            DayOfWeek.MONDAY, List.of(TARDE, MANANA),
            DayOfWeek.SATURDAY, List.of(MANANA));

    @Test
    void sinAjusteUsaElHorarioGeneralOrdenado() {
        HorarioSemanal horario = HorarioSemanal.general(general);

        assertThat(horario.tramos(DayOfWeek.MONDAY)).containsExactly(MANANA, TARDE);
        assertThat(horario.tramos(DayOfWeek.SUNDAY)).isEmpty();
        assertThat(horario.ajustado(DayOfWeek.MONDAY)).isFalse();
    }

    @Test
    void unDiaAjustadoUsaSusTramosYSinTramosNoTrabaja() {
        HorarioSemanal horario = new HorarioSemanal(general, Map.of(
                DayOfWeek.MONDAY, List.of(),
                DayOfWeek.SATURDAY, List.of(tramo("10:00", "14:00")),
                DayOfWeek.SUNDAY, List.of(tramo("10:00", "13:00"))));

        assertThat(horario.tramos(DayOfWeek.MONDAY)).isEmpty();
        assertThat(horario.tramos(DayOfWeek.SATURDAY)).containsExactly(tramo("10:00", "14:00"));
        // El ajuste puede salirse del horario general (decisión 4).
        assertThat(horario.tramos(DayOfWeek.SUNDAY)).containsExactly(tramo("10:00", "13:00"));
        assertThat(horario.ajustado(DayOfWeek.MONDAY)).isTrue();
    }

    @Test
    void unTramoNecesitaInicioAnteriorAlFin() {
        assertThatThrownBy(() -> tramo("14:00", "09:00")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tramo("09:00", "09:00")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaTramosSolapados() {
        assertThatThrownBy(() -> ValidadorTramos.comprobar(Map.of(DayOfWeek.MONDAY,
                List.of(tramo("13:00", "16:00"), tramo("09:00", "14:00")))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los tramos del lunes se solapan (09:00-14:00 y 13:00-16:00).");
    }

    @Test
    void rechazaUnTramoQueContieneAOtroAunqueHayaUnoEnMedio() {
        assertThatThrownBy(() -> ValidadorTramos.comprobar(Map.of(DayOfWeek.TUESDAY,
                List.of(tramo("09:00", "18:00"), tramo("10:00", "11:00"), tramo("12:00", "13:00")))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageStartingWith("Los tramos del martes se solapan");
    }

    @Test
    void rechazaTramosQueSeTocan() {
        assertThatThrownBy(() -> ValidadorTramos.comprobar(Map.of(DayOfWeek.FRIDAY,
                List.of(MANANA, tramo("14:00", "20:00")))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los tramos del viernes se tocan (09:00-14:00 y 14:00-20:00): únelos en uno.");
    }

    @Test
    void aceptaTramosSeparadosYDiasSinTramos() {
        ValidadorTramos.comprobar(Map.of(DayOfWeek.MONDAY, List.of(TARDE, MANANA), DayOfWeek.SUNDAY, List.of()));
    }

    @Test
    void unaCitaDentroDeUnTramoNoQuedaAfectada() {
        HorarioSemanal horario = HorarioSemanal.general(general);
        // 02/11/2026 es lunes.
        assertThat(CitasFueraDeHorario.fuera(cita("2026-11-02", "13:30", "14:00"), MADRID, horario, Set.of()))
                .isFalse();
    }

    @Test
    void unaCitaQueSaleDelTramoUnDiaCerradoOEnUnCierreQuedaAfectada() {
        HorarioSemanal horario = HorarioSemanal.general(general);

        assertThat(CitasFueraDeHorario.fuera(cita("2026-11-02", "13:30", "14:30"), MADRID, horario, Set.of()))
                .isTrue();
        assertThat(CitasFueraDeHorario.fuera(cita("2026-11-03", "10:00", "10:30"), MADRID, horario, Set.of()))
                .as("martes cerrado").isTrue();
        assertThat(CitasFueraDeHorario.fuera(cita("2026-11-02", "10:00", "10:30"), MADRID, horario,
                Set.of(LocalDate.of(2026, 11, 2)))).as("día de cierre").isTrue();
    }

    private static CitaOcupada cita(String fecha, String inicio, String fin) {
        LocalDate dia = LocalDate.parse(fecha);
        return new CitaOcupada(1L, 2L, "Luis García", instante(dia, inicio), instante(dia, fin));
    }

    private static Instant instante(LocalDate dia, String hora) {
        return ZonedDateTime.of(dia, LocalTime.parse(hora), MADRID).toInstant();
    }

    private static Tramo tramo(String inicio, String fin) {
        return new Tramo(LocalTime.parse(inicio), LocalTime.parse(fin));
    }
}
