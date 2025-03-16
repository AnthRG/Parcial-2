package app;

import app.controllers.*;
import app.entidades.Estudiante;
import app.entidades.Foto;
import app.entidades.Usuario;
import app.servicios.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import io.javalin.rendering.template.JavalinThymeleaf;
import io.javalin.security.RouteRole;
import jakarta.servlet.http.Cookie;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import static io.javalin.apibuilder.ApiBuilder.*;
import io.javalin.websocket.WsContext;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;


public class Main {
    private static String modoConexion = "";

    enum Rules implements RouteRole {
        ANONYMOUS,
        USER,
    }
    private static Set<WsContext> wsClients = ConcurrentHashMap.newKeySet();


    public static void main(String[] args) {
        java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.OFF);

        if (args.length >= 1) {
            modoConexion = args[0];
            System.out.println("Modo de Operacion: " + modoConexion);
        }


        //Iniciando la base de datos.
        if (modoConexion.isEmpty()) {
            BootStrapServices.getInstancia().init();
        }


        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");  // SIN la barra inicial "/templates/"
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);
        templateEngine.setTemplateResolver(resolver);

        // Crear la aplicación Javalin con el motor de plantillas
        var app = Javalin.create(config -> {
            config.staticFiles.add("/publico"); // Archivos estáticos
            config.fileRenderer(new JavalinThymeleaf(templateEngine)); // Configurar Thymeleaf


            config.router.apiBuilder(() -> {
                ApiBuilder.path("/crud-simple", () -> {
                    ApiBuilder.get("/", CrudUsuarioController::listar);
                    ApiBuilder.get("/crear", CrudUsuarioController::crearUsuarioForm);
                    post("/crear", CrudUsuarioController::procesarCreacionUsuario);
                    ApiBuilder.get("/visualizar/{username}", CrudUsuarioController::visualizarUsuario);
                    ApiBuilder.get("/editar/{username}", CrudUsuarioController::editarUsuarioForm);
                    post("/editar", CrudUsuarioController::procesarEditarUsuario);
                    ApiBuilder.get("/eliminar/{username}", CrudUsuarioController::eliminarUsuario);
                });

                ApiBuilder.path("/crud-estudiante", () -> {
                    ApiBuilder.get("/", EstudianteCrudController::listar);
                    ApiBuilder.get("/crear", EstudianteCrudController::crearEstudianteForm);
                    post("/crear", EstudianteCrudController::procesarCreacionEstudiante);
                    ApiBuilder.get("/pendientes", ctx -> {
                        try {
                            ctx.regnder("pendientes/EstudiantePendiente.html");
                        } catch (Exception e) {
                            e.printStackTrace();
                            ctx.status(500).result("Error interno en la vista de pendientes.");
                        }
                    });


                });

                ApiBuilder.path("/api", () -> {
                    ApiBuilder.get("/estudiantes", EstudianteCrudController::listarUbicaciones);
                    ApiBuilder.get("/estudiantes/{id}", EstudianteCrudController::obtenerEstudiante);
                });



                path("/fotos", () -> {
                    get(ctx -> {
                        ctx.redirect("/fotos/listar");
                    });
                    get("/listar", FotoController::listarFotos);
                    post("/procesarFoto", FotoController::procesarFotos);
                    get("/visualizar/{id}", FotoController::visualizarFotos);
                    get("/eliminar/{id}", FotoController::eliminarFotos);
                });

            });
        }).start(7070);

        app.ws("/sync", ws -> {
            ws.onMessage(ctx -> {
                try {
                    Gson gson = new Gson();
                    // Se parsea el mensaje como JsonObject para extraer la información necesaria
                    JsonObject jsonObject = gson.fromJson(ctx.message(), JsonObject.class);
                    String type = jsonObject.get("type").getAsString();

                    if ("sync".equalsIgnoreCase(type)) {
                        JsonArray dataArray = jsonObject.getAsJsonArray("data");
                        for (JsonElement element : dataArray) {
                            JsonObject obj = element.getAsJsonObject();

                            String nombre = obj.get("nombre").getAsString();
                            String sector = obj.get("sector").getAsString();
                            String nivelEscolarStr = obj.get("nivelEscolar").getAsString();

                            String usuarioRegistro = (obj.has("usuarioRegistro") && !obj.get("usuarioRegistro").getAsString().isEmpty())
                                    ? obj.get("usuarioRegistro").getAsString() : "offline";
                            Usuario user = UsuarioServices.getInstance().find(usuarioRegistro);
                            double latitud = obj.get("latitud").getAsDouble();
                            double longitud = obj.get("longitud").getAsDouble();

                            // Crear Estudiante (sin latitud y longitud)
                            Estudiante estudiante = new Estudiante(nombre,
                                    sector,
                                    nivelEscolarStr,
                                    user,
                                    latitud,
                                    longitud
                            );
                            EstudianteServices.getInstance().crear(estudiante);
                        }
                    }
                    ctx.send("{\"status\": \"success\"}");
                } catch (Exception e) {
                    e.printStackTrace();
                    ctx.send("{\"status\": \"error\"}");
                }
            });

        });


        app.before(ctx -> {
            if (ctx.sessionAttribute("USUARIO") == null) { // Si no hay sesión activa
                Cookie[] cookies = ctx.req().getCookies();

                if (cookies != null) {
                    Optional<Cookie> cookieOpt = Arrays.stream(cookies)
                            .filter(cookie -> "usuarioRecordado".equals(cookie.getName()))
                            .findFirst();

                    if (cookieOpt.isPresent()) {
                        String username = EncriptarUser.desencriptar(cookieOpt.get().getValue());
                        Usuario usuario = UsuarioServices.getInstance().find(username);

                        if (usuario != null) {
                            ctx.sessionAttribute("USUARIO", usuario); // Restaura la sesión
                        }
                    }
                }
            }
        });

        app.before("/", ctx -> {
            if (ctx.sessionAttribute("USUARIO") == null) {
                ctx.redirect("/formulario");
            }
        });

        app.before("/crud-simple*", ctx -> {
            if (!SessionCheckAdmin(ctx)) {
                ctx.redirect("/");
            }
        });

        app.before("/crud-estudiante*", ctx -> {
            if (!SessionCheckEncuestador(ctx) && !SessionCheckAdmin(ctx)) {
                ctx.redirect("/");
            }
        });

        app.get("/login", ctx -> ctx.redirect("/formulario"));
        app.get("/formulario", ctx -> ctx.render("/formulario.html"));
        app.get("/registro", ctx -> ctx.render("registro.html"));
        app.get("/", ctx -> ctx.render("index.html"));



        app.get("/logout", ctx -> {
            ctx.req().getSession().invalidate();
            ctx.removeCookie("usuarioRecordado");
            ctx.redirect("/formulario");
        });

        //filtros are down for trials
        /*app.before("/crud-*", ctx -> {
            if (!SessionCheckAutor(ctx)) {
                ctx.redirect("/");
            }
        });

        app.before("/crud-simple*", ctx -> {
            if (!SessionCheckAdmin(ctx)) {
                ctx.redirect("/");
            }
        });

        app.before("/fotos*", ctx -> {
            if (!SessionCheckAdmin(ctx)) {
                ctx.redirect("/");
            }
        });*/

        if (UsuarioServices.getInstance().findAll().isEmpty()) {
            startDB();
            System.out.println("Usuarios no encontrados");
        }


        app.post("/autenticar", ctx -> {
            String usuario = ctx.formParam("usuario");
            String contrasena = ctx.formParam("password");
            boolean recordar = "on".equals(ctx.formParam("recordar"));
            Usuario user = new Usuario(usuario, null, contrasena, false, false);

            if (validarUsuario(user)) {
                user = UsuarioServices.getInstance().find(user.getUsername());
                ctx.sessionAttribute("USUARIO", user);

                if (recordar) {
                    String datosEncriptados = EncriptarUser.encriptar(user.getUsername());
                    Cookie cookie = new Cookie("usuarioRecordado", datosEncriptados);
                    cookie.setMaxAge(7 * 24 * 60 * 60); // Expira en 1 semana
                    cookie.setPath("/");
                    ctx.res().addCookie(cookie);
                }

                ctx.redirect("/");
            } else {
                ctx.redirect("/formulario?error=credenciales"); // Redirigir con parámetro de error
            }
        });

        app.post("/registro", ctx -> {
            String nombre = ctx.formParam("nombre");
            String username = ctx.formParam("username");
            String contrasena = ctx.formParam("password");
            Foto foto = null;
            try {
                UploadedFile uploadedFile = ctx.uploadedFile("foto");
                if (uploadedFile != null) {
                    byte[] bytes = uploadedFile.content().readAllBytes();
                    String encodedString = Base64.getEncoder().encodeToString(bytes);
                    foto = new Foto(uploadedFile.filename(), uploadedFile.contentType(), encodedString);
                }

            } catch (Exception e) {
                System.out.println("foto aint working");
            }


            if (nombre == null || username == null || contrasena == null) {
                ctx.status(400);
                ctx.result("Llena todos los campos");
                return;
            }
            try {
                if (foto != null) {
                    FotoServices.getInstancia().crear(foto);
                }
            } catch (Exception e) {

            }
            Usuario user = new Usuario(nombre, username, contrasena, false, false, foto);
            UsuarioServices.getInstance().crear(user);

            ctx.redirect("/");
        });


        app.post("/logout", ctx -> {
            ctx.sessionAttribute("USUARIO", null); // Eliminar sesión

            Cookie cookie = new Cookie("usuarioRecordado", "");
            cookie.setMaxAge(0); // Expirar inmediatamente
            cookie.setPath("/");
            ctx.res().addCookie(cookie);

            ctx.redirect("/login");
        });

    }


    private static boolean SessionCheckEncuestador(Context ctx) {
        Usuario user = ctx.sessionAttribute("USUARIO");
        return user != null && user.isEncuestador();
    }

    private static boolean SessionCheck(Context ctx) {
        Usuario user = ctx.sessionAttribute("USUARIO");

        if (user == null) {
            Cookie[] cookies = ctx.req().getCookies();

            if (cookies != null) {
                Optional<Cookie> cookieOpt = Arrays.stream(cookies)
                        .filter(cookie -> "usuarioRecordado".equals(cookie.getName()))
                        .findFirst();

                if (cookieOpt.isPresent()) {
                    String username = EncriptarUser.desencriptar(cookieOpt.get().getValue());
                    Usuario usuario = UsuarioServices.getInstance().find(username);

                    if (usuario != null) {
                        ctx.sessionAttribute("USUARIO", usuario);
                        return true;
                    }
                }
            }
            return false; // No hay sesión ni cookie válida
        }

        return true; // Usuario ya tenía sesión activa
    }




    private static boolean SessionCheckAdmin(Context ctx) {
        Usuario user = ctx.sessionAttribute("USUARIO");
        if (user != null) {
            ctx.sessionAttribute("USUARIO", user);
            if (user.isAdministrador()) {
                return true;
            }
        }
        return false; // No hay sesión ni cookie válida
    }


    public static boolean validarUsuario(Usuario user) {
        Usuario usuario = UsuarioServices.getInstance().find(user.getUsername());
        try {
            if (usuario != null) {
                if (usuario.getUsername().equals(user.getUsername())) {
                    return user.getPassword().equals(usuario.getPassword());
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }

    }

    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 7000; //Retorna el puerto por defecto en caso de no estar en Heroku.
    }

    public static String getModoConexion() {
        return modoConexion;
    }

    private static void startDB() {
        Usuario Admin = new Usuario("admin", "Gustavo", "admin", true, false);
        UsuarioServices.getInstance().crear(Admin);
        UsuarioServices.getInstance().crear(new Usuario("Admin", "Anthony", "Admin", false, false));
        UsuarioServices.getInstance().crear(new Usuario("user1", "Usuario Normal", "user123", false, false));
        UsuarioServices.getInstance().crear(new Usuario("autor1", "Autor", "autor123", false, true));

    }


}