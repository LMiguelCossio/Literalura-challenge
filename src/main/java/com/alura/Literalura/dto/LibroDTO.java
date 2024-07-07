package com.alura.Literalura.dto;

import com.alura.Literalura.model.Autor;

public record LibroDTO(
        Long Id,
        String titulo,
        Autor autor,
        String idioma,
        Double descargas

) {
}
