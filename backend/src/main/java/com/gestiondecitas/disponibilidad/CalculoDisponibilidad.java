package com.gestiondecitas.disponibilidad;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.gestiondecitas.horario.Tramo;

/**
 * Horas de inicio libres de un día (CU-007), según el diagrama de actividad
 * {@code Actividad-Disponibilidad}. Sin Spring ni base de datos: recibe todo lo
 * que necesita, así se prueba con pruebas unitarias rápidas.
 *
 * @param intervaloMinutos RN-010: las horas van alineadas al reloj (con 30: 09:00, 09:30...)
 * @param zona             zona horaria del negocio, para pasar de hora local a instante
 * @param ahora            las horas que ya han empezado no se ofrecen
 */
public record CalculoDisponibilidad(int intervaloMinutos, ZoneId zona, Instant ahora) {

    private static final int MINUTOS_DIA = 24 * 60;

    /** Lo que ocupa al profesional: una cita confirmada o completada. */
    public record Ocupacion(Instant inicio, Instant fin) {
    }

    /**
     * @param tramos          lo que trabaja ese día (vacío si cierra o no trabaja)
     * @param duracionMinutos RN-001: el servicio tiene que caber entero en el tramo
     */
    public List<LocalTime> horas(LocalDate fecha, List<Tramo> tramos, int duracionMinutos,
            List<Ocupacion> ocupaciones) {
        Duration duracion = Duration.ofMinutes(duracionMinutos);
        List<LocalTime> libres = new ArrayList<>();
        tramos.stream().sorted(Comparator.comparing(Tramo::inicio)).forEach(tramo -> {
            int finTramo = minutos(tramo.fin());
            for (int m = alineado(minutos(tramo.inicio())); m + duracionMinutos <= finTramo && m < MINUTOS_DIA;
                    m += intervaloMinutos) {
                LocalTime hora = LocalTime.of(m / 60, m % 60);
                Instant inicio = ZonedDateTime.of(fecha, hora, zona).toInstant();
                Instant fin = inicio.plus(duracion);
                if (inicio.isAfter(ahora) && ocupaciones.stream().noneMatch(o -> seSolapan(o, inicio, fin))) {
                    libres.add(hora);
                }
            }
        });
        return libres;
    }

    /** Primer múltiplo del intervalo que no es anterior a {@code minuto}: 09:15 con 30 → 09:30. */
    private int alineado(int minuto) {
        return (minuto + intervaloMinutos - 1) / intervaloMinutos * intervaloMinutos;
    }

    /** Intervalos [inicio, fin): una cita que acaba a las 10:00 deja libre la hora de las 10:00. */
    private static boolean seSolapan(Ocupacion ocupacion, Instant inicio, Instant fin) {
        return ocupacion.inicio().isBefore(fin) && inicio.isBefore(ocupacion.fin());
    }

    private static int minutos(LocalTime hora) {
        return hora.getHour() * 60 + hora.getMinute();
    }
}
