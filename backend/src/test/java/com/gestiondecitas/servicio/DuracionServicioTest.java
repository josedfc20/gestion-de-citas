package com.gestiondecitas.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** RN-011: la duración es múltiplo del intervalo de reserva. */
class DuracionServicioTest {

    @ParameterizedTest(name = "{0} min con intervalo {1} → {2}")
    @CsvSource({
        "30, 30, true", "60, 30, true", "90, 30, true", "45, 30, false", "15, 30, false",
        "45, 15, true", "30, 60, false", "120, 60, true", "0, 30, false"})
    void esMultiplo(int duracion, int intervalo, boolean esperado) {
        assertThat(DuracionServicio.esMultiplo(duracion, intervalo)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Corte|Corte", "  Corte   de  pelo |Corte de pelo"})
    void normalizaElNombre(String entrada, String esperado) {
        assertThat(ServicioService.normalizarNombre(entrada)).isEqualTo(esperado);
    }
}
