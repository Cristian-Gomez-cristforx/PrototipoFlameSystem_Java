package Modulos_gestiones;

import java.util.*;

/**
 * Inventario.java
 * Conversión fiel de Inventario.py a Java.
 * - Mantiene la misma lógica y menú interactivo.
 * - IDs: id_insumo_actual, id_bebida_actual, id_producto_actual (separados).
 * - Las bebidas se guardan en 'bebidas' y los insumos en 'insumos'.
 *
 * Usa desde Main así (si Main está fuera del paquete):
 *   import Modulos_gestiones.Inventario;
 *   Scanner sc = new Scanner(System.in);
 *   Inventario inv = new Inventario();  inv.menuInventario(sc);
 *
 * Fuente original: Inventario.py. :contentReference[oaicite:1]{index=1}
 */
public class Inventario {

    // -----------------------
    // Modelos
    // -----------------------
    public static class Insumo {
        public final int idInsumo;
        public String nombre;
        public int cantidad;
        public double precio;

        public Insumo(int idInsumo, String nombre, int cantidad, double precio) {
            this.idInsumo = idInsumo;
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.precio = precio;
        }

        @Override
        public String toString() {
            return String.format("%d | %s | Cantidad: %d | Precio: %.2f", idInsumo, nombre, cantidad, precio);
        }
    }

    public static class Bebida extends Insumo {
        public String tipoBebida;
        public String tamanio;

        public Bebida(int idBebida, String nombre, int cantidad, double precio, String tipoBebida, String tamanio) {
            super(idBebida, nombre, cantidad, precio);
            this.tipoBebida = tipoBebida;
            this.tamanio = tamanio;
        }

        @Override
        public String toString() {
            return String.format("%d | %s | Cantidad: %d | Precio: %.2f | Tipo: %s | Tamaño: %s",
                    idInsumo, nombre, cantidad, precio, tipoBebida, tamanio);
        }
    }

    public static class Producto {
        public final int idProducto;
        public String nombre;
        public double precio;
        // mapa ingredienteId -> cantidad usada
        public final Map<Integer, Integer> ingredientes;

        public Producto(int idProducto, String nombre, double precio, Map<Integer, Integer> ingredientes) {
            this.idProducto = idProducto;
            this.nombre = nombre;
            this.precio = precio;
            this.ingredientes = ingredientes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(ingredientes);
        }

        @Override
        public String toString() {
            return String.format("%d | %s  | Precio: %.2f", idProducto, nombre, precio);
        }
    }

    // -----------------------
    // Estado
    // -----------------------
    private final Map<Integer, Producto> productos = new LinkedHashMap<>();
    private int idProductoActual = 1;

    private final Map<Integer, Insumo> insumos = new LinkedHashMap<>();
    private int idInsumoActual = 1;

    private final Map<Integer, Bebida> bebidas = new LinkedHashMap<>();
    private int idBebidaActual = 1;

    // -----------------------
    // MENÚ (interactivo). Llamar desde Main con un Scanner compartido.
    // -----------------------
    public void menuInventario(Scanner sc) {
        while (true) {
            System.out.println();
            System.out.println("========== MENÚ INVENTARIO ==========");
            System.out.println("1. Registrar producto");
            System.out.println("2. Actualizar precio de un producto");
            System.out.println("3. Mostrar productos y ver detalles");
            System.out.println("4. Actualizar cantidad insumo");
            System.out.println("5. Actualizar cantidad bebida");
            System.out.println("6. Registrar Insumo");
            System.out.println("7. Registrar Bebida");
            System.out.println("8. Ver Insumos");
            System.out.println("9. Ver bebidas");
            System.out.println("10. Salir");
            System.out.print("Seleccione una opción: ");

            String opcion = sc.nextLine().trim();
            switch (opcion) {
                case "1":
                    registrarProductoInteractivo(sc);
                    break;
                case "2":
                    actualizarPrecioProductoInteractivo(sc);
                    break;
                case "3":
                    mostrarProductosYDetallesInteractivo(sc);
                    break;
                case "4":
                    actualizarCantidadInsumoInteractivo(sc);
                    break;
                case "5":
                    actualizarCantidadBebidaInteractivo(sc);
                    break;
                case "6":
                    registrarInsumoInteractivo(sc);
                    break;
                case "7":
                    registrarBebidaInteractivo(sc);
                    break;
                case "8":
                    mostrarInsumos();
                    break;
                case "9":
                    mostrarBebidas();
                    break;
                case "10":
                    System.out.println("Saliendo del sistema...");
                    return;
                default:
                    System.out.println("Opción no válida. Intente otra vez.");
            }
        }
    }

    // -----------------------
    // Métodos públicos (API) - equivalentes a las funciones Python que devuelven objetos
    // -----------------------

    /**
     * Registrar insumo (no interactivo).
     * Retorna el Insumo creado o null si ya existe por nombre.
     */
    public Insumo registrarInsumo(String nombre, int cantidad, double precio) {
        if (nombre == null || nombre.trim().isEmpty()) return null;
        String norm = nombre.trim().toLowerCase();
        for (Insumo ins : insumos.values()) {
            if (ins.nombre.trim().toLowerCase().equals(norm)) {
                // ya existe
                return null;
            }
        }
        int nuevoId = idInsumoActual++;
        Insumo nuevo = new Insumo(nuevoId, nombre, cantidad, precio);
        insumos.put(nuevoId, nuevo);
        System.out.println("Insumo '" + nombre + "' registrado con ID: " + nuevoId + ".");
        return nuevo;
    }

    /**
     * Registrar bebida (no interactivo).
     * Retorna la Bebida creada o null si ya existe por nombre.
     */
    public Bebida registrarBebida(String nombre, int cantidad, double precio, String tipo, String tamanio) {
        if (nombre == null || nombre.trim().isEmpty()) return null;
        String norm = nombre.trim().toLowerCase();
        for (Bebida b : bebidas.values()) {
            if (b.nombre.trim().toLowerCase().equals(norm)) {
                return null;
            }
        }
        int nuevoId = idBebidaActual++;
        Bebida nueva = new Bebida(nuevoId, nombre, cantidad, precio, tipo, tamanio);
        bebidas.put(nuevoId, nueva);
        System.out.println("Bebida '" + nombre + "' registrada con ID: " + nuevoId + ".");
        return nueva;
    }

    /**
     * Registrar producto. Ingredientes es mapa id_insumo -> cantidad usada.
     * Retorna Producto o null si nombre duplicado.
     */
    public Producto registrarProducto(String nombre, double precio, Map<Integer, Integer> ingredientes) {
        if (nombre == null || nombre.trim().isEmpty()) return null;
        String norm = nombre.trim().toLowerCase();
        for (Producto p : productos.values()) {
            if (p.nombre.trim().toLowerCase().equals(norm)) return null;
        }
        if (insumos.isEmpty()) {
            System.out.println("¡Alerta, NO hay insumos para agregar!. Primero registre insumos");
            return null;
        }

        // validar ingredientes: si se provee, chequear ids existentes
        if (ingredientes != null) {
            for (Integer iid : ingredientes.keySet()) {
                if (!insumos.containsKey(iid)) {
                    System.out.println("Ingrediente inválido: ID " + iid);
                    return null;
                }
            }
        }

        Producto nuevo = new Producto(idProductoActual, nombre, precio, ingredientes);
        productos.put(idProductoActual, nuevo);
        System.out.println("Producto '" + nombre + "' registrado con ID: " + idProductoActual);
        idProductoActual += 1;
        return nuevo;
    }

    // -----------------------
    // Métodos interactivos (menú) - leen por Scanner y delegan al API
    // -----------------------

    private void registrarInsumoInteractivo(Scanner sc) {
        System.out.print("Nombre del insumo: ");
        String nombre = sc.nextLine().trim();
        if (nombre.isEmpty()) {
            System.out.println("Nombre inválido.");
            return;
        }
        Integer cantidad = pedirEntero(sc, "Cantidad disponible: ");
        if (cantidad == null) return;
        Double precio = pedirDouble(sc, "Precio del insumo: ");
        if (precio == null) return;

        Insumo res = registrarInsumo(nombre, cantidad, precio);
        if (res == null) System.out.println("¡ERROR!: Ya existe un insumo registrado con el nombre: " + nombre + ".");
    }

    private void registrarBebidaInteractivo(Scanner sc) {
        System.out.print("Nombre de la bebida: ");
        String nombre = sc.nextLine().trim();
        if (nombre.isEmpty()) {
            System.out.println("Nombre inválido.");
            return;
        }
        Integer cantidad = pedirEntero(sc, "Cantidad disponible: ");
        if (cantidad == null) return;
        Double precio = pedirDouble(sc, "Precio de la bebida: ");
        if (precio == null) return;

        System.out.print("Tipo de bebida: ");
        String tipo = sc.nextLine().trim();
        System.out.print("Tamaño (350ml/600ml/etc): ");
        String tamanio = sc.nextLine().trim();

        Bebida b = registrarBebida(nombre, cantidad, precio, tipo, tamanio);
        if (b == null) System.out.println("¡ERROR!: Ya existe una bebida registrada con el nombre: " + nombre + ".");
        else {
            // además, guardar en 'insumos' si tu flujo Python lo ponía ahí (en tu archivo Python bebidas y insumos eran separados,
            // pero en el flujo de uso las bebidas se mostraban aparte; mantengo separación tal como el Python que me pasaste).
            // Si quieres que también se agreguen a 'insumos', descomenta la línea siguiente:
            // insumos.put(b.idInsumo, b);
        }
    }

    private void registrarProductoInteractivo(Scanner sc) {
        System.out.print("Nombre del producto: ");
        String nombre = sc.nextLine().trim();
        if (nombre.isEmpty()) {
            System.out.println("Nombre inválido.");
            return;
        }
        if (insumos.isEmpty()) {
            System.out.println("\n¡Alerta, NO hay insumos para agregar!. Primero registre insumos");
            return;
        }

        System.out.println("\n--- Lista de Insumos Disponibles ---");
        mostrarInsumos();

        System.out.println("\nAgrega ingredientes al producto (por ID).");
        System.out.println("Si no quieres agregar más, escribe 0.\n");

        Map<Integer, Integer> ingredientes = new LinkedHashMap<>();
        while (true) {

            System.out.print("ID insumo: ");
            String entrada = sc.nextLine().trim();

            int ingId;
            try {
                ingId = Integer.parseInt(entrada);  // int(input())
    }       catch (Exception e) {
                  System.out.println("Ingrese un número válido.");
                  continue;  
    }

            if (ingId == 0)
            break;

            if (!insumos.containsKey(ingId)) {
                System.out.println(" Ese insumo no existe.");
                continue;
    }

            System.out.print("Cantidad del insumo que usa este producto: ");
            String cantStr = sc.nextLine().trim();

            int cantidadUsada;
            try {
                cantidadUsada = Integer.parseInt(cantStr);  // int(input())
    }       catch (Exception e) {
                   System.out.println("Ingrese un número válido.");
                   continue;  // igual que Python
    }

            ingredientes.put(ingId, cantidadUsada);
}


        Double precio = pedirDouble(sc, "Precio del producto: ");
        if (precio == null) return;

        Producto pro=registrarProducto(nombre, precio, ingredientes);
         if (pro== null) System.out.println("¡ERROR!: Ya existe un producto registrado con el nombre: " + nombre + ".");
    }

    private void actualizarPrecioProductoInteractivo(Scanner sc) {
        System.out.print("Nombre o ID del producto a actualizar: ");
        String identificador = sc.nextLine().trim();
        Producto prod = buscarProductoPorNombreOId(identificador);
        if (prod == null) {
            System.out.println("El producto no existe.");
            return;
        }
        Double nuevoPrecio = pedirDouble(sc, "Nuevo precio: ");
        if (nuevoPrecio == null) return;
        prod.precio = nuevoPrecio;
        System.out.printf("Precio actualizado para '%s' (ID %d) → Nuevo precio: %.2f%n",
                prod.nombre, prod.idProducto, nuevoPrecio);
    }

    private void mostrarProductosYDetallesInteractivo(Scanner sc) {
        if (productos.isEmpty()) {
            System.out.println("No hay productos registrados.");
            return;
        }

        System.out.println("\n--- Productos Registrados ---");
        for (Producto p : productos.values()) {
            System.out.println(p);
        }

        System.out.print("\nIngrese NOMBRE o ID del producto para ver detalles (0 para cancelar): ");
        String entrada = sc.nextLine().trim();
        if (entrada.isEmpty() || entrada.equals("0")) {
            System.out.println("Operación cancelada.");
            return;
        }
        Producto prod = buscarProductoPorNombreOId(entrada);
        if (prod == null) {
            System.out.println("Producto no encontrado.");
            return;
        }
        detallesProducto(prod.idProducto);
    }

    private void actualizarCantidadBebidaInteractivo(Scanner sc) {
        if (bebidas.isEmpty()) {
            System.out.println("No hay bebidas registradas.");
            return;
        }
        System.out.print("ID de la bebida: ");
        Integer bebId = pedirEntero(sc, "");
        if (bebId == null) return;
        if (!bebidas.containsKey(bebId)) {
            System.out.println("Esa bebida no existe.");
            return;
        }
        Bebida beb = bebidas.get(bebId);
        System.out.println("Bebida: " + beb.nombre + " | Cantidad actual: " + beb.cantidad);
        Integer nuevaCantidad = pedirEntero(sc, "Nueva cantidad: ");
        if (nuevaCantidad == null) return;
        beb.cantidad = nuevaCantidad;
        System.out.println("✔ Cantidad actualizada correctamente.");
    }

    private void actualizarCantidadInsumoInteractivo(Scanner sc) {
        if (insumos.isEmpty()) {
            System.out.println("No hay insumos registrados.");
            return;
        }
        Integer insId = pedirEntero(sc, "ID del insumo: ");
        if (insId == null) return;
        if (!insumos.containsKey(insId)) {
            System.out.println("Ese insumo no existe.");
            return;
        }
        Insumo ins = insumos.get(insId);
        System.out.println("Insumo: " + ins.nombre + " | Cantidad actual: " + ins.cantidad);
        Integer nueva = pedirEntero(sc, "Nueva cantidad: ");
        if (nueva == null) return;
        ins.cantidad = nueva;
        System.out.println("✔ Cantidad actualizada correctamente.");
    }

    // -----------------------
    // Mostrar / helpers
    // -----------------------

    public void mostrarInsumos() {
        if (insumos.isEmpty()) {
            System.out.println("No hay insumos registrados.");
            return;
        }
        System.out.println("\n--- Insumos Disponibles ---");
        for (Insumo ins : insumos.values()) {
            System.out.println(ins);
        }
    }

    public void mostrarBebidas() {
        if (bebidas.isEmpty()) {
            System.out.println("No hay bebidas registradas.");
            return;
        }
        System.out.println("\n--- Bebidas Disponibles ---");
        for (Bebida b : bebidas.values()) {
            System.out.println(b);
        }
    }

    public void detallesProducto(int idProducto) {
        if (!productos.containsKey(idProducto)) {
            System.out.println("Ese producto no existe.");
            return;
        }
        Producto p = productos.get(idProducto);
        System.out.println("\n--- Detalles de " + p.nombre + " ---");
        System.out.println("ID: " + p.idProducto);
        System.out.printf("Precio: %.2f%n", p.precio);

        System.out.println("\nIngredientes:");
        if (p.ingredientes == null || p.ingredientes.isEmpty()) {
            System.out.println(" - Este producto no tiene ingredientes.");
            return;
        }
        for (Map.Entry<Integer, Integer> e : p.ingredientes.entrySet()) {
            Integer ingId = e.getKey();
            Integer cant = e.getValue();
            Insumo ins = insumos.get(ingId);
            if (ins != null) {
                System.out.printf(" - %s (ID %d) -> Usa %d | Stock: %d%n", ins.nombre, ingId, cant, ins.cantidad);
            } else {
                System.out.printf(" - (INSUMO NO ENCONTRADO) (ID %d) -> Usa %d%n", ingId, cant);
            }
        }
    }

    private Producto buscarProductoPorNombreOId(String nombreOId) {
        if (nombreOId == null || nombreOId.isEmpty()) return null;
        try {
            int pid = Integer.parseInt(nombreOId);
            return productos.get(pid);
        } catch (NumberFormatException ex) {
            String target = nombreOId.trim().toLowerCase();
            for (Producto p : productos.values()) {
                if (p.nombre.trim().toLowerCase().equals(target)) return p;
            }
        }
        return null;
    }

    // -----------------------
    // Input helpers (replican pedir_entero / pedir_float)
    // -----------------------
    private Integer pedirEntero(Scanner sc, String prompt) {
        while (true) {
            if (prompt != null && !prompt.isEmpty()) System.out.print(prompt);
            String line = sc.nextLine().trim();
            if (line.isEmpty()) {
                System.out.println("Entrada vacía. Intente de nuevo.");
                return null;
            }
            try {
                int v = Integer.parseInt(line);
                if (v <= 0) {
                    System.out.println("Error: Ingrese un número entero positivo.");
                    continue;
                }
                return v;
            } catch (NumberFormatException ex) {
                System.out.println("Error: Ingrese un número entero válido.");
            }
        }
    }

    private Double pedirDouble(Scanner sc, String prompt) {
        while (true) {
            if (prompt != null && !prompt.isEmpty()) System.out.print(prompt);
            String line = sc.nextLine().trim();
            if (line.isEmpty()) {
                System.out.println("Entrada vacía. Intente de nuevo.");
                return null;
            }
            try {
                double v = Double.parseDouble(line);
                if (v <= 0) {
                    System.out.println("Error: Ingrese un número positivo.");
                    continue;
                }
                return v;
            } catch (NumberFormatException ex) {
                System.out.println("Error: Ingrese un número decimal válido.");
            }
        }
    }

    // -----------------------
    // Getters para usar desde Main / Reportes si hace falta
    // -----------------------
    public Map<Integer, Insumo> getInsumos() {
        return Collections.unmodifiableMap(insumos);
    }
    public Map<Integer, Bebida> getBebidas() {
        return Collections.unmodifiableMap(bebidas);
    }
    public Map<Integer, Producto> getProductos() {
        return Collections.unmodifiableMap(productos);
    }

    // -----------------------
    // MAIN para pruebas (opcional). Si vas a usar Main.java fuera del paquete no hace falta.
    // -----------------------
    public static void main(String[] args) {
        Inventario inv = new Inventario();
    
        Scanner sc = new Scanner(System.in);
        inv.menuInventario(sc);
        sc.close();
    }
}



