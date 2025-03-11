package app.controllers;

import app.entidades.Estudiante;
import app.entidades.Usuario;
import app.servicios.EstudianteServices;
import app.servicios.UsuarioServices;
import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EstudianteCrudController {
    public static void listar(@NotNull Context ctx) throws Exception {
        List<Estudiante> lista = EstudianteServices.getInstance().findAll();
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("titulo", "Listado de Estudiantes");
        modelo.put("estudiantes", lista);
        ctx.render("/crud-tradicional/listarEstudiante.html", modelo);
    }

    public static void crearEstudianteForm(@NotNull Context ctx) throws Exception {
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("titulo", "Formulario Creación Estudiante");
        modelo.put("accion", "/crud-estudiante/crear");
        ctx.render("/crud-tradicional/CrudEstudiante.html", modelo);
    }

    public static void procesarCreacionEstudiante(@NotNull Context ctx) throws Exception {
        String nombre = ctx.formParam("nombre");
        String sector = ctx.formParam("sector");
        String nivelEscolar = ctx.formParam("nivelEscolar");
        Usuario user = ctx.sessionAttribute("USUARIO");

        Estudiante estudiante = new Estudiante(nombre, sector, nivelEscolar, user);
        EstudianteServices.getInstance().crear(estudiante);
        ctx.redirect("/crud-estudiante/");
    }
}
