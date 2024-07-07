package com.alura.Literalura.repository;

import com.alura.Literalura.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LibroRepository extends JpaRepository<Libro,Long> {
    Optional<Libro> findByTituloContainingIgnoreCase(String nombreLibro);
}
