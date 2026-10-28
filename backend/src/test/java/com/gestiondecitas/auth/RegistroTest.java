package com.gestiondecitas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.gestiondecitas.ApiDePrueba;
import com.gestiondecitas.IntegracionTest;

import tools.jackson.databind.json.JsonMapper;

/** CU-001 · Registrarse. */
@IntegracionTest
class RegistroTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiDePrueba api;

    @BeforeEach
    void preparar() {
        api = new ApiDePrueba(mockMvc, jsonMapper);
    }

    @Test
    void creaUnaCuentaDeClienteConLaContrasenaEnBcrypt() throws Exception {
        String email = ApiDePrueba.emailNuevo();

        registrar(ApiDePrueba.datosRegistro(email))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/me"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.debeCambiarPassword").value(false))
                .andExpect(jsonPath("$.password").doesNotExist());

        var fila = jdbc.queryForMap("""
                SELECT u.rol, u.password_hash, c.fecha_nacimiento, c.privacidad_aceptada_en
                FROM usuario u JOIN cliente c ON c.usuario_id = u.id WHERE u.email = ?""", email);
        assertThat(fila.get("rol")).isEqualTo("CLIENTE");
        assertThat((String) fila.get("password_hash")).startsWith("$2a$12$");
        assertThat(fila.get("privacidad_aceptada_en")).isNotNull();
    }

    @Test
    void correoYaRegistradoDevuelve409AunqueCambienLasMayusculas() throws Exception {
        String email = ApiDePrueba.emailNuevo();
        api.registrar(email);

        registrar(ApiDePrueba.datosRegistro(email.toUpperCase()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe una cuenta con ese correo."));
    }

    @Test
    void menorDeCatorceAnosDevuelve422() throws Exception {
        var datos = conCambio("fechaNacimiento", LocalDate.now().minusYears(14).plusDays(2).toString());

        registrar(datos)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(
                        "Debes tener al menos 14 años para crear una cuenta. Un adulto puede reservar citas por ti."));
    }

    @Test
    void sinAceptarLaPrivacidadDevuelve400() throws Exception {
        registrar(conCambio("aceptaPrivacidad", false))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("aceptaPrivacidad"));
    }

    @Test
    void contrasenasQueNoCoincidenDevuelve400() throws Exception {
        registrar(conCambio("confirmacionPassword", "Otra-clave-distinta-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].mensaje").value("Las contraseñas no coinciden."));
    }

    @Test
    void contrasenaCortaOComunDevuelve400ConElMotivo() throws Exception {
        var corta = conCambio("password", "corta");
        corta.put("confirmacionPassword", "corta");
        registrar(corta)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].mensaje").value("Debe tener entre 10 y 64 caracteres."));

        var comun = conCambio("password", "1234567890");
        comun.put("confirmacionPassword", "1234567890");
        registrar(comun)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].mensaje").value("Es una contraseña demasiado común; elige otra."));
    }

    private Map<String, Object> conCambio(String campo, Object valor) {
        Map<String, Object> datos = new HashMap<>(ApiDePrueba.datosRegistro(ApiDePrueba.emailNuevo()));
        datos.put(campo, valor);
        return datos;
    }

    private ResultActions registrar(Map<String, Object> datos) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(api.aJson(datos)));
    }
}
