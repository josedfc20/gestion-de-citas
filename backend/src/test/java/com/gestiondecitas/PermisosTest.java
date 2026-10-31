package com.gestiondecitas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.gestiondecitas.shared.security.JwtService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Matriz de permisos (RNF-002, fase 5): cada endpoint de la API con cada perfil.
 * Solo comprueba la seguridad: quien no debe entrar recibe el 401 o el 403 de
 * Spring Security y quien sí debe entrar pasa (aunque luego la petición dé 400 o
 * 404 porque el cuerpo está vacío o el id no existe). La tabla tiene que tener
 * todos los endpoints: uno nuevo obliga a decidir quién puede usarlo.
 */
@IntegracionTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PermisosTest {

    /** Quién puede usar un endpoint (paso 3.3 y {@code SecurityConfig}). */
    enum Acceso {
        /** Cualquiera, también sin sesión. */
        PUBLICO,
        /** Cualquier usuario con sesión, aunque tenga la contraseña temporal (RN-005). */
        SESION,
        /** Cualquier usuario con sesión y la contraseña definitiva. */
        DEFINITIVA,
        CLIENTE,
        EMPLEADO,
        ADMIN,
        EMPLEADO_O_ADMIN
    }

    enum Perfil {
        ANONIMO, CLIENTE, EMPLEADO, EMPLEADO_TEMPORAL, ADMIN;

        boolean puede(Acceso acceso) {
            return switch (acceso) {
                case PUBLICO -> true;
                case SESION -> this != ANONIMO;
                case DEFINITIVA -> this != ANONIMO && this != EMPLEADO_TEMPORAL;
                case CLIENTE -> this == CLIENTE;
                case EMPLEADO -> this == EMPLEADO;
                case ADMIN -> this == ADMIN;
                case EMPLEADO_O_ADMIN -> this == EMPLEADO || this == ADMIN;
            };
        }
    }

    record Endpoint(HttpMethod metodo, String ruta, Acceso acceso) {

        String clave() {
            return metodo.name() + " " + ruta;
        }

        @Override
        public String toString() {
            return clave() + " (" + acceso + ")";
        }
    }

    private static final HttpMethod GET = HttpMethod.GET;
    private static final HttpMethod POST = HttpMethod.POST;
    private static final HttpMethod PUT = HttpMethod.PUT;
    private static final HttpMethod PATCH = HttpMethod.PATCH;
    private static final HttpMethod DELETE = HttpMethod.DELETE;

    /** Los 54 endpoints de {@code /api/v1}, con las rutas tal como las declaran los controllers. */
    static final List<Endpoint> ENDPOINTS = List.of(
            // Autenticación y cuenta (B2, B3)
            new Endpoint(POST, "/api/v1/auth/registro", Acceso.PUBLICO),
            new Endpoint(POST, "/api/v1/auth/login", Acceso.PUBLICO),
            new Endpoint(POST, "/api/v1/auth/refresh", Acceso.PUBLICO),
            new Endpoint(POST, "/api/v1/auth/logout", Acceso.PUBLICO),
            new Endpoint(POST, "/api/v1/auth/recuperar-password", Acceso.PUBLICO),
            new Endpoint(POST, "/api/v1/auth/restablecer-password", Acceso.PUBLICO),
            new Endpoint(GET, "/api/v1/me", Acceso.SESION),
            new Endpoint(PUT, "/api/v1/me/password", Acceso.SESION),
            new Endpoint(PUT, "/api/v1/me", Acceso.DEFINITIVA),
            new Endpoint(GET, "/api/v1/me/datos", Acceso.DEFINITIVA),
            new Endpoint(DELETE, "/api/v1/me", Acceso.CLIENTE),
            // Consultas públicas (B4, B5, B7)
            new Endpoint(GET, "/api/v1/negocio", Acceso.PUBLICO),
            new Endpoint(GET, "/api/v1/servicios", Acceso.PUBLICO),
            new Endpoint(GET, "/api/v1/servicios/{id}", Acceso.PUBLICO),
            new Endpoint(GET, "/api/v1/profesionales", Acceso.PUBLICO),
            new Endpoint(GET, "/api/v1/disponibilidad", Acceso.PUBLICO),
            // Cliente (B6a)
            new Endpoint(GET, "/api/v1/me/beneficiarios", Acceso.CLIENTE),
            new Endpoint(GET, "/api/v1/me/beneficiarios/{id}", Acceso.CLIENTE),
            new Endpoint(POST, "/api/v1/me/beneficiarios", Acceso.CLIENTE),
            new Endpoint(PUT, "/api/v1/me/beneficiarios/{id}", Acceso.CLIENTE),
            new Endpoint(DELETE, "/api/v1/me/beneficiarios/{id}", Acceso.CLIENTE),
            new Endpoint(GET, "/api/v1/me/citas", Acceso.CLIENTE),
            new Endpoint(POST, "/api/v1/citas", Acceso.CLIENTE),
            new Endpoint(POST, "/api/v1/citas/{id}/cancelacion", Acceso.CLIENTE),
            // Citas compartidas (B6b): cada uno ve lo suyo; lo ajeno es 404 (probado en cada clase)
            new Endpoint(GET, "/api/v1/citas/{id}", Acceso.DEFINITIVA),
            new Endpoint(PATCH, "/api/v1/citas/{id}/estado", Acceso.EMPLEADO_O_ADMIN),
            // Empleado (B4, B6b)
            new Endpoint(GET, "/api/v1/empleado/servicios", Acceso.EMPLEADO),
            new Endpoint(GET, "/api/v1/empleado/agenda", Acceso.EMPLEADO),
            new Endpoint(GET, "/api/v1/empleado/citas", Acceso.EMPLEADO),
            // Administrador (B4, B5, B6b)
            new Endpoint(GET, "/api/v1/admin/negocio", Acceso.ADMIN),
            new Endpoint(PUT, "/api/v1/admin/negocio", Acceso.ADMIN),
            new Endpoint(GET, "/api/v1/admin/servicios", Acceso.ADMIN),
            new Endpoint(GET, "/api/v1/admin/servicios/{id}", Acceso.ADMIN),
            new Endpoint(POST, "/api/v1/admin/servicios", Acceso.ADMIN),
            new Endpoint(PUT, "/api/v1/admin/servicios/{id}", Acceso.ADMIN),
            new Endpoint(PATCH, "/api/v1/admin/servicios/{id}/activo", Acceso.ADMIN),
            new Endpoint(GET, "/api/v1/admin/empleados", Acceso.ADMIN),
            new Endpoint(GET, "/api/v1/admin/empleados/{id}", Acceso.ADMIN),
            new Endpoint(POST, "/api/v1/admin/empleados", Acceso.ADMIN),
            new Endpoint(PUT, "/api/v1/admin/empleados/{id}", Acceso.ADMIN),
            new Endpoint(PUT, "/api/v1/admin/empleados/{id}/servicios", Acceso.ADMIN),
            new Endpoint(PATCH, "/api/v1/admin/empleados/{id}/activo", Acceso.ADMIN),
            new Endpoint(POST, "/api/v1/admin/empleados/{id}/password-temporal", Acceso.ADMIN),
            new Endpoint(GET, "/api/v1/admin/horario-general", Acceso.ADMIN),
            new Endpoint(PUT, "/api/v1/admin/horario-general", Acceso.ADMIN),
            new Endpoint(GET, "/api/v1/admin/empleados/{id}/horario", Acceso.ADMIN),
            new Endpoint(PUT, "/api/v1/admin/empleados/{id}/horario", Acceso.ADMIN),
            new Endpoint(GET, "/api/v1/admin/dias-cierre", Acceso.ADMIN),
            new Endpoint(GET, "/api/v1/admin/dias-cierre/{id}", Acceso.ADMIN),
            new Endpoint(POST, "/api/v1/admin/dias-cierre", Acceso.ADMIN),
            new Endpoint(DELETE, "/api/v1/admin/dias-cierre/{id}", Acceso.ADMIN),
            new Endpoint(GET, "/api/v1/admin/citas", Acceso.ADMIN),
            new Endpoint(POST, "/api/v1/admin/citas", Acceso.ADMIN),
            new Endpoint(GET, "/api/v1/admin/clientes", Acceso.ADMIN));

    /** Un id que no existe: quien pasa la seguridad recibe un 404, nunca toca datos de otras pruebas. */
    private static final String ID_INEXISTENTE = "999999999";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapeos;

    private final Map<Perfil, String> tokens = new EnumMap<>(Perfil.class);

    @BeforeAll
    void crearPerfiles() throws Exception {
        ApiDePrueba api = new ApiDePrueba(mockMvc, jsonMapper);
        tokens.put(Perfil.CLIENTE, api.clienteConSesion().bearer());
        long empleado = ApiDePrueba.crearEmpleado(jdbc, passwordEncoder, ApiDePrueba.emailNuevo(), ApiDePrueba.PASSWORD);
        tokens.put(Perfil.EMPLEADO_TEMPORAL, "Bearer " + jwtService.emitir(empleado, "EMPLEADO", true));
        tokens.put(Perfil.EMPLEADO, "Bearer " + jwtService.emitir(empleado, "EMPLEADO", false));
        tokens.put(Perfil.ADMIN, ApiDePrueba.bearerAdmin(jwtService, jdbc));
    }

    static Stream<Arguments> casos() {
        return ENDPOINTS.stream().flatMap(e -> Stream.of(Perfil.values()).map(p -> Arguments.of(e, p)));
    }

    @ParameterizedTest(name = "{0} con {1}")
    @MethodSource("casos")
    void cadaPerfilEntraSoloDondeDebe(Endpoint endpoint, Perfil perfil) throws Exception {
        MockHttpServletRequestBuilder peticion = ApiDePrueba.conIpNueva(
                request(endpoint.metodo(), endpoint.ruta().replace("{id}", ID_INEXISTENTE)));
        if (endpoint.metodo() != GET && endpoint.metodo() != DELETE) {
            peticion.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        if (perfil != Perfil.ANONIMO) {
            peticion.header(HttpHeaders.AUTHORIZATION, tokens.get(perfil));
        }

        MvcResult resultado = mockMvc.perform(peticion).andReturn();
        int estado = resultado.getResponse().getStatus();
        String detalle = detalle(resultado);

        if (perfil.puede(endpoint.acceso())) {
            assertThat(esRechazoDeSeguridad(estado, detalle))
                    .as("%s debería dejar pasar a %s, pero respondió %d: %s", endpoint, perfil, estado, detalle)
                    .isFalse();
        } else if (perfil == Perfil.ANONIMO) {
            assertThat(estado).as("%s sin sesión", endpoint).isEqualTo(401);
            assertThat(detalle).isEqualTo("Hay que iniciar sesión.");
        } else {
            assertThat(estado).as("%s con %s", endpoint, perfil).isEqualTo(403);
            assertThat(detalle).isEqualTo(perfil == Perfil.EMPLEADO_TEMPORAL
                    ? "Debes cambiar tu contraseña antes de continuar."
                    : "No tienes permiso para esta operación.");
        }
    }

    @Test
    void laTablaTieneTodosLosEndpointsDeLaApi() {
        Set<String> reales = new TreeSet<>();
        mapeos.getHandlerMethods().keySet().forEach(info -> info.getPatternValues().stream()
                .filter(ruta -> ruta.startsWith("/api/v1/"))
                .forEach(ruta -> info.getMethodsCondition().getMethods()
                        .forEach(metodo -> reales.add(metodo.name() + " " + ruta))));
        Set<String> enLaTabla = new TreeSet<>();
        ENDPOINTS.forEach(e -> enLaTabla.add(e.clave()));

        List<String> sinDecidir = new ArrayList<>(reales);
        sinDecidir.removeAll(enLaTabla);
        List<String> queYaNoExisten = new ArrayList<>(enLaTabla);
        queYaNoExisten.removeAll(reales);

        assertThat(sinDecidir).as("Endpoints que faltan en la matriz de permisos").isEmpty();
        assertThat(queYaNoExisten).as("Endpoints de la matriz que ya no existen").isEmpty();
        assertThat(ENDPOINTS).hasSize(enLaTabla.size());
    }

    /** El 401 y el 403 que escribe {@code ProblemDetailsSecurityHandler} (no los de la lógica, como un refresh caducado). */
    private static boolean esRechazoDeSeguridad(int estado, String detalle) {
        return (estado == 401 && "Hay que iniciar sesión.".equals(detalle))
                || (estado == 403 && ("No tienes permiso para esta operación.".equals(detalle)
                        || "Debes cambiar tu contraseña antes de continuar.".equals(detalle)));
    }

    private String detalle(MvcResult resultado) throws Exception {
        String cuerpo = resultado.getResponse().getContentAsString();
        if (cuerpo.isBlank() || !cuerpo.startsWith("{")) {
            return "";
        }
        JsonNode nodo = jsonMapper.readTree(cuerpo).get("detail");
        return nodo == null ? "" : nodo.asString();
    }
}
