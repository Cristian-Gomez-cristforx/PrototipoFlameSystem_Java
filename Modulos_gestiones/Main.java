package Modulos_gestiones;

import java.util.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.LocalDate; // <-- Asegúrate de tener esta
import java.time.format.DateTimeParseException;
/**
 * Main.java
 * Integración final idéntica a Menu_final.py
 * Conecta Usuarios, Inventario, GestorPedidos y Reportes.
 */
public class Main {

    // ----------------------------
    // Clase Usuario (Igual que antes)
    // ----------------------------
    public static class Usuario {
        public static List<Usuario> usuariosRegistrados = new ArrayList<>();
        String nombre, correo, contrasena, rol;
        long documento, telefono;

        public Usuario(String nombre, String correo, String contrasena, String rol, long documento, long telefono) {
            this.nombre = nombre;
            this.correo = correo;
            this.contrasena = contrasena;
            this.rol = rol;
            this.documento = documento;
            this.telefono = telefono;
        }

        public static Usuario iniciarSesionStatic(String correo, String contrasena) {
            for (Usuario u : usuariosRegistrados) {
                if (u.correo.equalsIgnoreCase(correo) && u.contrasena.equals(contrasena)) return u;
            }
            return null;
        }

        public void registrarUsuario(String nombre, String correo, String contrasena, String rol, long documento, long telefono) {
            Usuario u = new Usuario(nombre, correo, contrasena, rol, documento, telefono);
            usuariosRegistrados.add(u);
            System.out.println("Usuario registrado: " + nombre);
        }
        
        public boolean restablecerContrasena(Scanner sc) {
             System.out.print("Nueva contraseña: ");
             this.contrasena = sc.nextLine();
             return true;
        }

        @Override
        public String toString() {
            return String.format("%s (%s)", nombre, rol);
        }
    }

    // ----------------------------
    // Helpers Main (Asumiendo que pedirLong es igual)
    // ----------------------------
    private static long pedirLong(Scanner sc, String t) {
        while(true) {
            System.out.print(t);
            try { return Long.parseLong(sc.nextLine()); } catch(Exception e) {}
        }
    }

    // ----------------------------
    // MENÚ MESERO (Llama a GestorPedidos - Sin cambios)
    // ----------------------------
    private static boolean menuMesero(GestionPedidos.GestorPedidos gestor, Scanner sc, boolean permitirCerrar, String usuarioNombre) {
        System.out.println("-- Bienvenido al Menú de Mesero, " + usuarioNombre + " --");
        while(true) {
            String opSalida = permitirCerrar ? "0. Cerrar Sesión" : "0. Salir menú";
            System.out.println("\n--- Opciones ---");
            System.out.println("1. Crear pedido (flujo completo)");
            System.out.println("2. Buscar y editar pedido por TIPO");
            System.out.println("3. Ver mis pedidos (activos)");
            System.out.println("4. Ver todos activos");
            System.out.println("5. Ver historial (finalizados)");
            System.out.println(opSalida);
            System.out.print("Opción: ");
            String opt = sc.nextLine();

            if (opt.equals("1")) {
                gestor.crearPedidoInteractivo(sc, usuarioNombre);
            } else if (opt.equals("2")) {
                gestor.buscarYEditarPorTipoInteractivo(sc, usuarioNombre);
            } else if (opt.equals("3")) {
                System.out.println("Tus pedidos activos:");
                gestor.listarActivos().stream()
                    .filter(p -> p.mesero.equalsIgnoreCase(usuarioNombre))
                    .forEach(p -> System.out.println(p.resumen()));
            } else if (opt.equals("4")) {
                gestor.listarActivos().forEach(p -> System.out.println(p.resumen()));
            } else if (opt.equals("5")) {
                gestor.listarHistorial().forEach(p -> System.out.println(p.resumen() + " | Fin: " + p.fechaFinalizacion.toLocalDate()));
            } else if (opt.equals("0")) {
                System.out.println(permitirCerrar ? "Cerrando sesión..." : "Volviendo...");
                return permitirCerrar;
            } else {
                System.out.println("Opción inválida.");
            }
        }
    }

    // ----------------------------
    // MAIN PRINCIPAL
    // ----------------------------
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // 1. Instanciar Inventario (Base de datos en memoria)
        Inventario inventario = new Inventario();
        // 2. Instanciar GestorPedidos pasándole el Inventario (Dependencia)
        GestionPedidos.GestorPedidos gestor = new GestionPedidos.GestorPedidos(inventario);
        // 3. Instanciar Reportes pasándole el GestorPedidos
        Reportes reportes = new Reportes(gestor, sc);


        // Admin por defecto
        Usuario admin = new Usuario("Felipe Dominguez", "felipe@gmail.com", "flamepipe7040", "admin", 1090350760L, 3134206754L);
        Usuario.usuariosRegistrados.add(admin);

        while (true) {
            System.out.print("\nPresione Enter para Iniciar sesión (o escriba 'salir'): ");
            String ini = sc.nextLine();
            if(ini.equalsIgnoreCase("salir")) break;

            int intentos = 0;
            while (true) {
                System.out.print("Correo: "); String correo = sc.nextLine();
                System.out.print("Contraseña: "); String pass = sc.nextLine();
                Usuario usuarioLogin = Usuario.iniciarSesionStatic(correo, pass);

                if (usuarioLogin == null) {
                    System.out.println("Datos incorrectos.");
                    intentos++;
                    if(intentos >= 3) {
                        System.out.println("Restablecer? (si/no)");
                        if(sc.nextLine().equalsIgnoreCase("si")) {
                            // Lógica simple restablecer buscando usuario...
                            break; 
                        }
                    }
                    continue;
                }

                // LOGIN EXITOSO
                if (usuarioLogin.rol.equalsIgnoreCase("admin")) {
                    System.out.println("-- Menú ADMIN: " + usuarioLogin.nombre + " --");
                    boolean salirSesion = false;
                    while(!salirSesion) {
                        // --- MENÚ ADMIN ACTUALIZADO ---
                        System.out.println("\n1. Registrar Usuario\n2. Inventario\n3. Ver usuarios\n4. Menú Reportes\n5. Menú Mesero\n6. Cerrar sesión");
                        String op = sc.nextLine();
                        switch(op) {
                            case "1":
                                System.out.print("Nombre: "); String n = sc.nextLine();
                                System.out.print("Correo: "); String c = sc.nextLine();
                                System.out.print("Clave: "); String cl = sc.nextLine();
                                System.out.print("Rol: "); String r = sc.nextLine();
                                long doc = pedirLong(sc, "Doc: ");
                                long tel = pedirLong(sc, "Tel: ");
                                admin.registrarUsuario(n,c,cl,r,doc,tel);
                                break;
                            case "2":
                                inventario.menuInventario(sc);
                                break;
                            case "3":
                                int contador = 0;
                                for(Usuario usu : Usuario.usuariosRegistrados) {
                                    contador++;
                                    System.out.println(String.format("\n--Usuario número: %d - %s--", contador, usu));
                                }
                                break;
                            case "4": // NUEVO MENÚ REPORTES
                                reportes.menuReportes();
                                break;
                            case "5": // MENÚ MESERO
                                menuMesero(gestor, sc, false, usuarioLogin.nombre);
                                break;
                            case "6":
                                System.out.println("Cerrando sesión...");
                                salirSesion = true;
                                break;
                            default:
                                System.out.println("Opción inválida.");
                        }
                    }
                } 
                else if (usuarioLogin.rol.equalsIgnoreCase("mesero")) {
                    boolean cerrar = menuMesero(gestor, sc, true, usuarioLogin.nombre);
                    if (cerrar) {
                        System.out.println("Sesión mesero cerrada.");
                        break; 
                    }
                } 
                else if (usuarioLogin.rol.equalsIgnoreCase("cocinero")) {
                    gestor.menuCocineroInteractivo(sc, usuarioLogin);
                    break;
                }
                break; // Romper loop intentos si salió de sesión
            }
        }
        sc.close();
        System.out.println("Aplicación terminada.");
    }
}

