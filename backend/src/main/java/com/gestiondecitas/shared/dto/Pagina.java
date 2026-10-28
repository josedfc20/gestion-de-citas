package com.gestiondecitas.shared.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Una página de resultados (paso 3.3: {@code ?page=0&size=20}). */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <E, T> Pagina<T> de(Page<E> pagina, Function<E, T> convertir) {
        return new Pagina<>(pagina.getContent().stream().map(convertir).toList(), pagina.getNumber(),
                pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }
}
