package com.gestiondecitas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * La especificación OpenAPI del repositorio ({@code docs/api/openapi.json}) es
 * la de la API real (paso 6b, ADR-017). Si alguien cambia un endpoint o un DTO
 * sin regenerarla, esta prueba falla y dice cómo hacerlo, igual que
 * {@link PermisosTest} con un endpoint que falta en la tabla.
 *
 * <p>Para regenerarla: {@code ./mvnw test -Dtest=OpenApiActualizadoTest -Dopenapi.actualizar=true}.
 */
@IntegracionTest
class OpenApiActualizadoTest {

    private static final Path ARCHIVO = Path.of("..", "docs", "api", "openapi.json");
    private static final String REGENERAR =
            "./mvnw test -Dtest=OpenApiActualizadoTest -Dopenapi.actualizar=true";

    private final JsonMapper json = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    @Autowired
    private MockMvc mvc;

    @Test
    void elArchivoDelRepositorioCoincideConLaApi() throws Exception {
        String respuesta = mvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode actual = ordenar(json.readTree(respuesta));
        // La dirección del servidor depende de dónde se ejecute; no es parte de la API.
        ((ObjectNode) actual).remove("servers");
        String texto = json.writeValueAsString(actual) + "\n";

        if (Boolean.getBoolean("openapi.actualizar")) {
            Files.createDirectories(ARCHIVO.getParent());
            Files.writeString(ARCHIVO, texto, StandardCharsets.UTF_8);
        }

        assertThat(ARCHIVO)
                .as("Falta docs/api/openapi.json. Genéralo con: " + REGENERAR)
                .exists();
        assertThat(leer())
                .as("docs/api/openapi.json no coincide con la API. Si el cambio es"
                        + " intencionado, regenéralo con: " + REGENERAR)
                .isEqualTo(texto);
    }

    private String leer() throws IOException {
        return Files.readString(ARCHIVO, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    /** Ordena las claves de los objetos para que el archivo no cambie de un arranque a otro. */
    private JsonNode ordenar(JsonNode nodo) {
        if (nodo instanceof ObjectNode objeto) {
            ObjectNode ordenado = json.createObjectNode();
            List<String> claves = new ArrayList<>(objeto.propertyNames());
            claves.sort(null);
            for (String clave : claves) {
                ordenado.set(clave, ordenar(objeto.get(clave)));
            }
            return ordenado;
        }
        if (nodo instanceof ArrayNode lista) {
            ArrayNode copia = json.createArrayNode();
            lista.forEach(elemento -> copia.add(ordenar(elemento)));
            return copia;
        }
        return nodo;
    }
}
