package com.gestiondecitas.shared.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comprueba el formato Problem Details con un controlador de prueba que lanza
 * cada excepción. Sin filtros de seguridad: aquí solo importa el formato.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ControladorDePrueba.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandlerTest.ControladorDePrueba.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void datosConFormatoIncorrectoDevuelve400ConLosCampos() throws Exception {
        mockMvc.perform(post("/prueba/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\", \"email\": \"no-es-un-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Datos no válidos"))
                .andExpect(jsonPath("$.errores.length()").value(2))
                .andExpect(jsonPath("$.errores[?(@.campo == 'email')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'nombre')]").exists());
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(post("/prueba/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void recursoNoEncontradoDevuelve404() throws Exception {
        mockMvc.perform(get("/prueba/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value("La cita no existe."));
    }

    @Test
    void huecoNoDisponibleDevuelve409() throws Exception {
        mockMvc.perform(get("/prueba/409"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Hueco no disponible"));
    }

    @Test
    void reglaDeNegocioDevuelve422() throws Exception {
        mockMvc.perform(get("/prueba/422"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Regla de negocio incumplida"));
    }

    @Test
    void errorInesperadoDevuelve500SinDetallesInternos() throws Exception {
        mockMvc.perform(get("/prueba/500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Ha ocurrido un error inesperado."));
    }

    record DatosDePrueba(@NotBlank String nombre, @Email String email) {
    }

    @RestController
    static class ControladorDePrueba {

        @PostMapping("/prueba/validar")
        void validar(@Valid @RequestBody DatosDePrueba datos) {
        }

        @GetMapping("/prueba/404")
        void noEncontrado() {
            throw new RecursoNoEncontradoException("La cita no existe.");
        }

        @GetMapping("/prueba/409")
        void ocupado() {
            throw new HuecoNoDisponibleException("El hueco ya está ocupado.");
        }

        @GetMapping("/prueba/422")
        void reglaNegocio() {
            throw new ReglaNegocioException("Ya no se puede cancelar.");
        }

        @GetMapping("/prueba/500")
        void inesperado() {
            throw new IllegalStateException("detalle interno que no debe salir");
        }
    }
}
