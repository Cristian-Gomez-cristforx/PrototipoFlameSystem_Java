
package Modulos_gestiones;
import java.util.*;

public class Gestion_Usuarios {

    // Scanner global (equivale a input())
    private static final Scanner SC = new Scanner(System.in);

    public static class Usuario {

        // ----- Variable de clase (equivalente a usuarios_registrados = []) -----
        public static List<Usuario> usuariosRegistrados = new ArrayList<>();

        // ----- Atributos -----
        private String nombre;
        private String correo;
        private String contrasena;
        private String rol;
        private long documento;
        private long telefono;

        public Usuario(String nombre, String correo, String contrasena, String rol,
                       long documento, long telefono) {

            this.nombre = nombre;
            this.correo = correo;
            this.contrasena = contrasena;
            this.rol = rol;
            this.documento = documento;
            this.telefono = telefono;
        }

        // ----- registrar_usuario -----
        public void registrarUsuario(String name, String email, String password,
                                     String rol, long docu, long phone) {

            Usuario nuevo = new Usuario(name, email, password, rol, docu, phone);
            Usuario.usuariosRegistrados.add(nuevo);

            System.out.println("Usuario de tipo " + nuevo.rol + " registrado con éxito.");
        }

        // ----- iniciar_sesion -----
        public Usuario iniciarSesion(String correoActual, String contraActual) {
            for (Usuario u : Usuario.usuariosRegistrados) {
                if (u.correo.equals(correoActual) && u.contrasena.equals(contraActual)) {
                    return u;
                }
            }
            return null;
        }

        // ----- restablecer_contraseña -----
        public boolean restablecerContrasena() {

            while (true) {

                System.out.print("\nIngrese la dirección de correo asociada a su cuenta: ");
                String verificarCorreo = SC.nextLine();

                for (Usuario cuenta : Usuario.usuariosRegistrados) {

                    if (cuenta.correo.equals(verificarCorreo)) {

                        int codigoAleatorio = new Random().nextInt(900000) + 100000;

                        int arrobaIndex = cuenta.correo.indexOf("@");
                        String nombreUsuario = cuenta.correo.substring(0, arrobaIndex);

                        int caracteresVisibles = 3;
                        int largoOculto = nombreUsuario.length() - caracteresVisibles;

                        String parteOculta =
                                "*".repeat(Math.max(0, largoOculto));

                        String parteVisible = nombreUsuario.substring(0,
                                Math.min(3, nombreUsuario.length()));

                        String correoEnmascarado =
                                parteVisible + parteOculta + cuenta.correo.substring(arrobaIndex);

                        System.out.println("\nHemos enviado un código de 6 dígitos a: " + correoEnmascarado);
                        System.out.println("--FlameSystem--\nCódigo: " + codigoAleatorio);

                        // Validación del código
                        while (true) {
                            int ingresoCodigo = pedirEntero("Ingrese el código: ");

                            if (ingresoCodigo == codigoAleatorio) {

                                System.out.print("Ingrese su nueva contraseña: ");
                                String nueva = SC.nextLine();

                                System.out.print("Confirme su contraseña: ");
                                String confirmar = SC.nextLine();

                                if (nueva.equals(confirmar)) {
                                    cuenta.contrasena = nueva;
                                    System.out.println("Contraseña cambiada con éxito.");
                                    return true;
                                } else {
                                    System.out.println("Las contraseñas no coinciden.");
                                }

                            } else {
                                System.out.println("¡Error! El código no coincide.");
                            }
                        }
                    }
                }

                System.out.println("La dirección de correo no está asociada a ninguna cuenta.");
            }
        }

        @Override
        public String toString() {
            return "Usuario: " + nombre + " | correo: " + correo +
                   " | contraseña: " + contrasena + " | rol: " + rol +
                   " | documento: " + documento + " | teléfono: " + telefono;
        }
    }

    // ======================================================
    //        FUNCIONES SUELTAS DEL MÓDULO (STATIC)
    // ======================================================

    public static int pedirEntero(String texto) {
        while (true) {
            System.out.print(texto);
            try {
                return Integer.parseInt(SC.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Error: ingrese un número entero válido.");
            }
        }
    }

    public static long pedirLong(String texto) {
    while (true) {
        System.out.print(texto);
        try {
            long numero = Long.parseLong(SC.nextLine().trim());

            if (numero < 0) {
                System.out.println("Error: Ingrese un número positivo.");
                continue;
            }

            return numero;

        } catch (NumberFormatException e) {
            System.out.println("Error: Ingrese un número válido.");
        }
    }
}

    public static String verificarCorreo(String texto) {
        while (true) {
            System.out.print(texto);
            String corre = SC.nextLine().trim();

            if (corre.contains("@") && corre.contains("com")) {

                boolean repetido = false;
                for (Usuario u : Usuario.usuariosRegistrados) {
                    if (u.correo.equalsIgnoreCase(corre)) {
                        System.out.println("Este correo ya está asociado a una cuenta con rol "
                                + u.rol + ". Ingrese otro.");
                        repetido = true;
                        break;
                    }
                }
                if (!repetido) return corre;

            } else {
                System.out.println("Formato de correo inválido.");
            }
        }
    }

    
    public static void main(String[] args) {

        // Crear tu admin inicial
        Usuario admin = new Usuario("Felipe Dominguez",
                "felipe@gmail.com",
                "flamepipe7040",
                "admin",
                1090350760L,
                3134206754L);

        Usuario.usuariosRegistrados.add(admin);

        // ------------------ MENÚ PRINCIPAL ------------------
        while (true) {

            System.out.print("Presione ENTER para iniciar sesión ");
            String option = SC.nextLine();

            boolean usuarioDecidioSalir = false;

            // --------- AQUÍ respeta EXACTO tu Python ---------
            if (option.equals("")) {

                int intentos = 0;

                while (true) {

                    System.out.print("Ingrese su correo electrónico: ");
                    String actualCorreo = SC.nextLine();

                    System.out.print("Ingrese su contraseña: ");
                    String actualContrasena = SC.nextLine();

                    Usuario usuarioLogin =
                            admin.iniciarSesion(actualCorreo, actualContrasena);

                    if (usuarioLogin == null) {

                        System.out.println("\nInicio de sesión fallido.");
                        intentos++;

                        if (intentos >= 3) {
                            System.out.println("Has fallado 3 veces.");
                            System.out.print("¿Deseas restablecer la contraseña? si/no: ");
                            String elegir = SC.nextLine();

                            if (elegir.equalsIgnoreCase("si")) {
                                if (admin.restablecerContrasena()) break;

                            } else if (elegir.equalsIgnoreCase("no")) {
                                System.out.println("Volviendo al menú inicial...");
                                break;

                            } else {
                                System.out.println("Ingrese solo si o no.");
                            }
                        }
                        continue;
                    }

                    

                    // Menú admin
                    if (usuarioLogin.rol.equalsIgnoreCase("admin")) {

                        System.out.println("\n--Bienvenido ADMIN, " +
                                usuarioLogin.nombre + "--");

                        while (true) {
                            System.out.println("\n1. Registrar usuario");
                            System.out.println("2. Registrar producto");
                            System.out.println("3. Mostrar clientes");
                            System.out.println("4. Cerrar sesión");
                            System.out.print("Opción: ");

                            String op = SC.nextLine();

                            if (op.equals("1")) {

                                System.out.println("Ingrese datos del nuevo usuario:");

                                System.out.print("Nombre completo: ");
                                String nombre = SC.nextLine();

                                String correo = verificarCorreo("Correo: ");

                                System.out.print("Contraseña: ");
                                String contra = SC.nextLine();

                                System.out.print("Rol: ");
                                String rol = SC.nextLine();

                                // DOCUMENTO ÚNICO
                                long docu;
                                while (true) {
                                    docu = pedirEntero("Número de documento: ");
                                    boolean existe = false;
                                    for (Usuario u : Usuario.usuariosRegistrados) {
                                        if (u.documento == docu) {
                                            System.out.println("Este documento ya está usado por rol: "
                                                    + u.rol);
                                            existe = true;
                                            break;
                                        }
                                    }
                                    if (!existe) break;
                                }

                                // TELÉFONO 10 dígitos
                                long phone;
                                while (true) {
                                    phone = pedirLong("Teléfono (10 dígitos): ");
                                    if (String.valueOf(phone).length() == 10) break;
                                    System.out.println("Debe tener 10 dígitos.");
                                }

                                admin.registrarUsuario(nombre, correo, contra, rol, docu, phone);

                            } else if (op.equals("2")) {
                                // igual que tu Python: no implementado
                                System.out.println("Función no implementada.");

                            } else if (op.equals("3")) {

                                int contador = 0;
                                for (Usuario u : Usuario.usuariosRegistrados) {
                                    contador++;
                                    System.out.println("\nUsuario " + contador + ": " + u);
                                }

                            } else if (op.equals("4")) {
                                System.out.println("Cerrando sesión...");
                                usuarioDecidioSalir = true;
                                break;
                            }
                        }

                    }

                    // ------------------- MENU MESERO -------------------
                    else if (usuarioLogin.rol.equalsIgnoreCase("mesero")) {

                        System.out.println("\n--Bienvenido MESERO, " +
                                usuarioLogin.nombre + "--");

                        while (true) {

                            System.out.println("\n1. Registrar pedido");
                            System.out.println("2. Cancelar pedido");
                            System.out.println("3. Confirmar pago");
                            System.out.println("4. Cerrar sesión");

                            String op = SC.nextLine();

                            if (op.equals("4")) {
                                System.out.println("Cerrando sesión...");
                                usuarioDecidioSalir = true;
                                break;
                            }
                        }
                    }

                    if (usuarioDecidioSalir) break;
                }
            }
        }
    }
}
