package com.gestiondecitas;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Perfil {@code prod} (paso 7a). Arranca el backend en un puerto de verdad, porque
 * la lectura de {@code X-Forwarded-For} la hace Tomcat y no actúa con MockMvc.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "prod"})
@Import(TestcontainersConfiguration.class)
class PerfilProduccionTest {

    private final HttpClient http = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();

    @LocalServerPort
    private int puerto;

    /**
     * D-9: detrás de Caddy todas las peticiones llegan desde la IP del proxy. Sin
     * leer {@code X-Forwarded-For}, todos los usuarios compartirían los 5 intentos
     * de login por minuto.
     */
    @Test
    void cadaIpRealTieneSuPropioLimiteDeIntentosDeLogin() throws Exception {
        String ipA = ipDePrueba("203.0.113.");
        String ipB = ipDePrueba("198.51.100.");

        for (int i = 0; i < 5; i++) {
            assertThat(login(ipA).statusCode()).isEqualTo(401);
        }
        assertThat(login(ipA).statusCode()).isEqualTo(429);

        // Otra IP real, aunque llegue por el mismo proxy, empieza de cero.
        assertThat(login(ipB).statusCode()).isEqualTo(401);
    }

    @Test
    void swaggerEstaApagado() throws Exception {
        assertThat(get("/v3/api-docs").statusCode()).isEqualTo(404);
        assertThat(get("/swagger-ui/index.html").statusCode()).isEqualTo(404);
    }

    @Test
    void laApiSigueFuncionando() throws Exception {
        assertThat(get("/api/v1/negocio").statusCode()).isEqualTo(200);
        assertThat(get("/actuator/health").statusCode()).isEqualTo(200);
    }

    /** Login con un correo que no existe (siempre 401) desde la IP real {@code ip}. */
    private HttpResponse<String> login(String ip) throws IOException, InterruptedException {
        String cuerpo = "{\"email\":\"" + ApiDePrueba.emailNuevo() + "\",\"password\":\"No-es-la-clave-123\"}";
        HttpRequest peticion = HttpRequest.newBuilder(url("/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", ip)
                .header("X-Forwarded-Proto", "https")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();
        return http.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String ruta) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(url(ruta)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI url(String ruta) {
        return URI.create("http://127.0.0.1:" + puerto + ruta);
    }

    /** IP de los rangos reservados para documentación, distinta en cada ejecución. */
    private static String ipDePrueba(String prefijo) {
        return prefijo + ThreadLocalRandom.current().nextInt(1, 255);
    }
}
