package com.alura.Literalura.principal;

import com.alura.Literalura.model.Autor;
import com.alura.Literalura.model.DatosAutor;
import com.alura.Literalura.model.DatosLibro;
import com.alura.Literalura.model.Libro;
import com.alura.Literalura.repository.AutorRepository;
import com.alura.Literalura.repository.LibroRepository;
import com.alura.Literalura.service.ConsumoAPI;
import com.alura.Literalura.service.ConvierteDatos;
import com.alura.Literalura.service.ConvierteDatosAutor;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private static final ConsumoAPI CONSUMO_API = new ConsumoAPI();
    private static final ConvierteDatos CONVERSOR = new ConvierteDatos();
    private static final ConvierteDatosAutor CONVERSOR_AUTOR = new ConvierteDatosAutor();
    private static final String URL_BASE = "https://gutendex.com/books/";
    private final Scanner teclado = new Scanner(System.in);
    private final LibroRepository repositorio;
    private final AutorRepository repositorio2;
    private List<Libro> libros;
    private List<Autor> autores;

    public Principal(LibroRepository repository, AutorRepository repository2) {
        this.repositorio = repository;
        this.repositorio2 = repository2;
    }

    public void muestraElMenu() {
        int opcion;
        do {
            System.out.println(getMenu());
            opcion = teclado.nextInt();
            teclado.nextLine();
            processOption(opcion);
        } while (opcion != 0);
        System.out.println("Cerrando la aplicacion");
    }

    private String getMenu() {
        return """
                Elija la tarea a traves de su número:
                
                1- buscar libro por titulo
                2- listar libros registrados
                3- listar autores registrados
                4- listar autores vivos en un determinado año
                5- listar libros por idioma
                6- buscar autores por nombre
                7- top 10 libros en la API
                8- top 5 libros en la DB
                9- autores en derecho público                                  
                0 - Salir
                """;
    }

    private void processOption(int opcion) {
        switch (opcion) {
            case 1 -> buscarLibroPorTitulo();
            case 2 -> mostrarLibrosBuscados();
            case 3 -> mostrarAutoresRegistrados();
            case 4 -> mostrarAutoresVivosEnUnDeterminadoAno();
            case 5 -> listarLibrosPorIdioma();
            case 6 -> buscarAutorPorNombre();
            case 7 -> top10LibrosEnLaAPI();
            case 8 -> top5LibrosEnLaDB();
            case 9 -> autoresEnDerechoPublico();
            case 0 -> System.out.println("Cerrando la aplicacion");
            default -> System.out.println("Opcion no encontrada");
        }
    }

    private DatosLibro getDatosLibro(String nombreLibro) throws Exception {
        var json = CONSUMO_API.obtenerDatos(URL_BASE + "?search=" + nombreLibro.replace(" ", "+"));
        return CONVERSOR.obtenerDatos(json, DatosLibro.class);
    }

    private DatosAutor getDatosAutor(String nombreLibro) throws Exception {
        var json = CONSUMO_API.obtenerDatos(URL_BASE + "?search=" + nombreLibro.replace(" ", "+"));
        return CONVERSOR_AUTOR.obtenerDatos(json, DatosAutor.class);
    }

    private String pregunta() {
        System.out.println("Escribe el nombre del libro que deseas buscar");
        return teclado.nextLine();
    }

    private void mostrarLibrosBuscados() {
        libros = Optional.ofNullable(repositorio.findAll()).orElseGet(ArrayList::new);
        libros.stream()
                .sorted(Comparator.comparing(Libro::getDescargas))
                .forEach(System.out::println);
    }

    public void buscarLibroPorTitulo() {
        mostrarLibrosBuscados();
        String libroBuscado = pregunta();

        libros = Optional.ofNullable(libros).orElseGet(ArrayList::new);

        Optional<Libro> broli = libros.stream()
                .filter(l -> l.getTitulo().toLowerCase().contains(libroBuscado.toLowerCase()))
                .findFirst();

        if (broli.isPresent()) {
            System.out.println(broli.get());
            System.out.println("El libro ya fue cargado, pruebe con otro");
        } else {
            try {
                DatosLibro datosLibro = getDatosLibro(libroBuscado);
                if (datosLibro != null) {
                    DatosAutor datosAutor = getDatosAutor(libroBuscado);
                    if (datosAutor != null) {
                        Autor autor = obtenerAutor(datosAutor);
                        Libro libro = crearYGuardarLibro(datosLibro, autor);
                        System.out.println(libro);
                    } else {
                        System.out.println("No se encontró el autor para el libro");
                    }
                } else {
                    System.out.println("No se encontró el libro");
                }
            } catch (Exception e) {
                System.out.println("Excepción: " + e.getMessage());
            }
        }
    }

    private Autor obtenerAutor(DatosAutor datosAutor) {
        autores = Optional.ofNullable(repositorio2.findAll()).orElseGet(ArrayList::new);

        return autores.stream()
                .filter(a -> Optional.ofNullable(datosAutor.nombre()).map(n -> a.getNombre().toLowerCase().contains(n.toLowerCase())).orElse(false))
                .findFirst()
                .orElseGet(() -> {
                    Autor autor = new Autor(datosAutor.nombre(), datosAutor.nacimiento(), datosAutor.fallecimiento());
                    repositorio2.save(autor);
                    return autor;
                });
    }

    private Libro crearYGuardarLibro(DatosLibro datosLibro, Autor autor) {
        Libro libro = new Libro(
                datosLibro.titulo(),
                autor,
                Optional.ofNullable(datosLibro.idioma()).orElse(Collections.emptyList()),
                datosLibro.descargas()
        );

        libros.add(libro);
        autor.setLibros(libros);

        repositorio.save(libro);
        System.out.println("Libro guardado exitosamente");
        return libro;
    }

    public void mostrarAutoresRegistrados() {
        autores = repositorio2.findAll();
        autores.forEach(System.out::println);
    }

    public void mostrarAutoresVivosEnUnDeterminadoAno() {
        System.out.println("Ingrese un año");
        int anio = teclado.nextInt();
        autores = repositorio2.findAll();
        List<String> autoresNombre = autores.stream()
                .filter(a -> a.getFallecimiento() >= anio && a.getNacimiento() <= anio)
                .map(Autor::getNombre)
                .collect(Collectors.toList());
        autoresNombre.forEach(System.out::println);
    }

    public void listarLibrosPorIdioma() {
        libros = repositorio.findAll();
        List<String> idiomasUnicos = libros.stream()
                .map(Libro::getIdioma)
                .distinct()
                .collect(Collectors
                        .toList());

        idiomasUnicos.forEach(idioma -> {
            switch (idioma) {
                case "en" -> System.out.println("en - english");
                case "es" -> System.out.println("es - español");
            }
        });

        System.out.println("");
        System.out.println("Ingrese el idioma del que desea buscar los libros");
        String idiomaBuscado = teclado.nextLine();
        List<Libro> librosBuscados = libros.stream()
                .filter(l -> l.getIdioma().contains(idiomaBuscado))
                .collect(Collectors
                        .toList());
        librosBuscados.forEach(System.out::println);
    }

    public void buscarAutorPorNombre() {
        System.out.println("Ingrese el nombre del autor que desea buscar");
        var nombreAutor = teclado.nextLine();
        Optional<Autor> autorBuscado = repositorio2.findByNombreContainingIgnoreCase(nombreAutor);
        autorBuscado.ifPresentOrElse(
                System.out::println,
                () -> System.out.println("Autor no encontrado")
        );
    }

    public void top10LibrosEnLaAPI() {
        try {
            String json = CONSUMO_API.obtenerDatos(URL_BASE + "?sort");
            List<DatosLibro> datosLibros = CONVERSOR.obtenerDatosArray(json, DatosLibro.class);
            List<DatosAutor> datosAutor = CONVERSOR_AUTOR.obtenerDatosArray(json, DatosAutor.class);

            List<Libro> libros = new ArrayList<>();
            for (int i = 0; i < datosLibros.size(); i++) {
                Autor autor = new Autor(datosAutor.get(i).nombre(), datosAutor.get(i).nacimiento(), datosAutor.get(i).fallecimiento());
                Libro libro = new Libro(datosLibros.get(i).titulo(), autor, datosLibros.get(i).idioma(), datosLibros.get(i).descargas());
                libros.add(libro);
            }

            libros.sort(Comparator.comparingDouble(Libro::getDescargas).reversed());
            libros.stream().limit(10).forEach(System.out::println);

        } catch (Exception e) {
            System.out.println("Ha ocurrido un error " + e.getMessage());
        }
    }

    public void top5LibrosEnLaDB() {
        try {
            List<Libro> libros = repositorio.findAll();
            libros.stream()
                    .sorted(Comparator.comparingDouble(Libro::getDescargas).reversed())
                    .limit(5)
                    .forEach(System.out::println);
        } catch (Exception e) {
            System.out.println();
        }
    }

    public void autoresEnDerechoPublico() {
        try {
            String json = CONSUMO_API.obtenerDatos(URL_BASE + "?sort");
            List<DatosAutor> datosAutor = CONVERSOR_AUTOR.obtenerDatosArray(json, DatosAutor.class);

            Map<String, Autor> autoresMap = new HashMap<>();

            for (DatosAutor datoAutor : datosAutor) {
                String nombre = datoAutor.nombre();
                Autor autor = autoresMap.get(nombre);

                if (autor == null) {
                    autor = new Autor(nombre, datoAutor.nacimiento(), datoAutor.fallecimiento());
                    autoresMap.put(nombre, autor);
                }

                List<Libro> librosArray = new ArrayList<>();
                autor.setLibros(librosArray);
            }

            List<Autor> autoresOrdenados = autoresMap.values().stream()
                    .filter(a -> a.getFallecimiento() < 1954)
                    .collect(Collectors.toList());

            List<Autor> diezAutores = autoresOrdenados.subList(0, Math.min(10, autoresOrdenados.size()));

            for (int i = 0; i < diezAutores.size(); i++) {
                System.out.println((i + 1) + ". " + diezAutores.get(i).getNombre() +
                        ", año de fallecimiento: " + diezAutores.get(i).getFallecimiento());
            }

        } catch (Exception e) {
            System.out.println("ha ocurrido un errror: " + e.getMessage());
        }
    }
}