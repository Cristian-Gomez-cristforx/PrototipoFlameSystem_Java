package Modulos_gestiones;

import java.util.*;
import java.time.LocalDateTime;

/**
 * GestionPedidos.java
 *
 * Lógica completa de pedidos traducida de Python.
 * Conecta con Inventario.java para validar stock y descontar al finalizar.
 * Incluye lógica de "Omisiones por unidad".
 */
public class GestionPedidos {

    // -------------------------------------------------------------------------
    // MODELOS INTERNOS (Lineas y Pedido)
    // -------------------------------------------------------------------------

    public static class LineaProducto {
        public Inventario.Producto producto;
        public int cantidad;
        // Mapa: IndiceDeUnidad (0..n) -> Set de IDs de insumos a omitir
        public Map<Integer, Set<Integer>> omitidosPorUnidad;

        public LineaProducto(Inventario.Producto producto, int cantidad, Map<Integer, Set<Integer>> omitidosPorUnidad) {
            this.producto = producto;
            this.cantidad = cantidad;
            this.omitidosPorUnidad = omitidosPorUnidad != null ? omitidosPorUnidad : new HashMap<>();
        }

        // Calcula cuánto se debe gastar realmente de cada insumo teniendo en cuenta las omisiones
        public Map<Integer, Integer> consumoPorInsumoTrasOmisiones() {
            Map<Integer, Integer> resultado = new HashMap<>();
            Map<Integer, Integer> ingredientesBase = producto.ingredientes; // id -> cantidad_unitaria

            // Contar cuántas veces se omite cada insumo
            Map<Integer, Integer> conteoOmisiones = new HashMap<>();
            for (Set<Integer> setIds : omitidosPorUnidad.values()) {
                for (Integer id : setIds) {
                    conteoOmisiones.put(id, conteoOmisiones.getOrDefault(id, 0) + 1);
                }
            }

            for (Map.Entry<Integer, Integer> entry : ingredientesBase.entrySet()) {
                int insId = entry.getKey();
                int cantBase = entry.getValue();

                int unidadesOmitidas = conteoOmisiones.getOrDefault(insId, 0);
                int unidadesReales = this.cantidad - unidadesOmitidas;
                if (unidadesReales < 0) unidadesReales = 0;

                resultado.put(insId, cantBase * unidadesReales);
            }
            return resultado;
        }

        public Map<Integer, Integer> consumoTotalIgnorandoOmisiones() {
            Map<Integer, Integer> res = new HashMap<>();
            for (var entry : producto.ingredientes.entrySet()) {
                res.put(entry.getKey(), entry.getValue() * this.cantidad);
            }
            return res;
        }
    }

    public static class LineaBebida {
        public Inventario.Bebida bebida;
        public int cantidad;

        public LineaBebida(Inventario.Bebida bebida, int cantidad) {
            this.bebida = bebida;
            this.cantidad = cantidad;
        }
    }

    public static class Pedido {
        public int idPedido;
        public String mesero;
        public String cliente;
        public String tipoPedido; // "Mesa", "Domicilio", "Recoger"
        public String estado;     // EN_CONSTRUCCION, EN_COLA, COCINADO, FINALIZADO, CANCELADO

        public List<LineaProducto> lineasProductos = new ArrayList<>();
        public List<LineaBebida> lineasBebidas = new ArrayList<>();
        public String comentarios = "";

        public boolean pagado = false;
        public boolean cocinado = false;
        public boolean consumoAplicado = false; // true cuando ya se descontó del inventario

        public LocalDateTime fechaCreacion;
        public LocalDateTime fechaFinalizacion;
        public double totalVenta = 0.0;

        // Almacena el detalle de lo que se consumió realmente (para reportes)
        public Map<String, Map<Integer, Integer>> consumoDetallado = null; 

        public Pedido(int id, String mesero, String cliente, String tipo) {
            this.idPedido = id;
            this.mesero = mesero;
            this.cliente = cliente;
            this.tipoPedido = tipo;
            this.estado = "EN_CONSTRUCCION";
            this.fechaCreacion = LocalDateTime.now();
        }

        public boolean esModificable() {
            return !cocinado && !estado.equals("FINALIZADO") && !estado.equals("CANCELADO");
        }

        public String resumen() {
            String est = estado;
            if (cocinado && pagado && consumoAplicado) est = "FINALIZADO";
            return String.format("[%d] Mesero:%s Cliente:%s Tipo:%s Estado:%s", 
                    idPedido, mesero, cliente, tipoPedido, est);
        }

        public void detalles() {
            System.out.println("\n--- DETALLES PEDIDO ---");
            System.out.println(resumen());
            if (!lineasProductos.isEmpty()) {
                System.out.println("Productos:");
                for (int i = 0; i < lineasProductos.size(); i++) {
                    LineaProducto lp = lineasProductos.get(i);
                    System.out.printf("  %d) %s x%d%n", i, lp.producto.nombre, lp.cantidad);
                    if (!lp.omitidosPorUnidad.isEmpty()) {
                        System.out.println("    Omisiones:");
                        lp.omitidosPorUnidad.forEach((u, set) -> 
                            System.out.println("      Unidad " + u + ": Omitir IDs " + set));
                    }
                }
            } else {
                System.out.println("Productos: (ninguno)");
            }

            if (!lineasBebidas.isEmpty()) {
                System.out.println("Bebidas:");
                for (LineaBebida lb : lineasBebidas) {
                    System.out.printf("  - %s x%d%n", lb.bebida.nombre, lb.cantidad);
                }
            }
            if (!comentarios.isEmpty()) {
                System.out.println("Comentarios: " + comentarios);
            }
            System.out.println("-----------------------\n");
        }
    }

    // -------------------------------------------------------------------------
    // GESTOR DE PEDIDOS (Lógica principal)
    // -------------------------------------------------------------------------
    public static class GestorPedidos {
        private Inventario inventario;
        private Map<Integer, Pedido> pedidos = new LinkedHashMap<>();
        private List<Pedido> historial = new ArrayList<>();
        private int nextId = 1;

        public GestorPedidos(Inventario inventario) {
            this.inventario = inventario;
        }

        public Pedido obtenerPedido(int id) {
            return pedidos.get(id);
        }

        // --- LÓGICA DE NEGOCIO ---

        public Pedido crearPedido(String mesero, String cliente, String tipo) {
            Pedido p = new Pedido(nextId++, mesero, cliente, tipo);
            pedidos.put(p.idPedido, p);
            return p;
        }

        /**
         * Agrega producto validando stock con lógica de omisiones.
         */
        public boolean agregarProducto(int pedidoId, int productoId, int cantidad, Map<Integer, Set<Integer>> omitidos) {
            Pedido p = pedidos.get(pedidoId);
            if (p == null || !p.esModificable()) return false;

            Inventario.Producto prod = inventario.getProductos().get(productoId);
            if (prod == null) return false;

            // Simular consumo para validar stock
            LineaProducto simulacion = new LineaProducto(prod, cantidad, omitidos);
            Map<Integer, Integer> necesario = simulacion.consumoPorInsumoTrasOmisiones();

            for (var entry : necesario.entrySet()) {
                Inventario.Insumo ins = inventario.getInsumos().get(entry.getKey());
                if (ins == null || ins.cantidad < entry.getValue()) {
                    System.out.println("Stock insuficiente para insumo ID: " + entry.getKey());
                    return false;
                }
            }

            // Si pasa, agregar
            p.lineasProductos.add(new LineaProducto(prod, cantidad, omitidos));
            return true;
        }

        public boolean agregarBebida(int pedidoId, int bebidaId, int cantidad) {
            Pedido p = pedidos.get(pedidoId);
            if (p == null || !p.esModificable()) return false;
            
            Inventario.Bebida bev = inventario.getBebidas().get(bebidaId);
            if (bev == null || bev.cantidad < cantidad) return false;

            p.lineasBebidas.add(new LineaBebida(bev, cantidad));
            return true;
        }

        public void confirmarEnvioCocina(int pid) {
            Pedido p = pedidos.get(pid);
            if (p != null) p.estado = "EN_COLA";
        }

        public boolean cancelarPedido(int pid) {
            Pedido p = pedidos.get(pid);
            if (p == null || p.cocinado) return false; // No cancelar si ya se cocinó
            p.estado = "CANCELADO";
            return true;
        }

        public boolean marcarPagado(int pid) {
            Pedido p = pedidos.get(pid);
            if (p == null || p.estado.equals("CANCELADO")) return false;
            p.pagado = true;
            intentarFinalizar(p);
            return true;
        }

        /**
         * Lógica crítica: Marca cocinado, calcula consumo real final y trata de finalizar.
         */
        public boolean marcarCocinadoLogico(int pid, Map<Integer, Map<Integer, Set<Integer>>> omisionesPorLinea) {
            Pedido p = pedidos.get(pid);
            if (p == null || p.cocinado || p.estado.equals("CANCELADO")) return false;

            Map<Integer, Integer> totalInsumos = new HashMap<>();
            
            // Procesar líneas y actualizar omisiones finales
            for (int i = 0; i < p.lineasProductos.size(); i++) {
                LineaProducto lp = p.lineasProductos.get(i);
                
                // Si el cocinero indicó nuevas omisiones, actualizarlas
                if (omisionesPorLinea != null && omisionesPorLinea.containsKey(i)) {
                    lp.omitidosPorUnidad = omisionesPorLinea.get(i);
                }

                Map<Integer, Integer> consumoLinea = lp.consumoPorInsumoTrasOmisiones();
                for (var entry : consumoLinea.entrySet()) {
                    totalInsumos.put(entry.getKey(), totalInsumos.getOrDefault(entry.getKey(), 0) + entry.getValue());
                }
            }
            
            // Guardar "foto" del consumo
            p.consumoDetallado = new HashMap<>();
            p.consumoDetallado.put("insumos", totalInsumos);
            
            // Calcular bebidas
            Map<Integer, Integer> totalBebidas = new HashMap<>();
            for (LineaBebida lb : p.lineasBebidas) {
                totalBebidas.put(lb.bebida.idInsumo, totalBebidas.getOrDefault(lb.bebida.idInsumo, 0) + lb.cantidad);
            }
            p.consumoDetallado.put("bebidas", totalBebidas);

            p.cocinado = true;
            p.estado = "COCINADO";
            intentarFinalizar(p);
            return true;
        }

        /**
         * Descuenta stock REALMENTE solo si está Pagado y Cocinado.
         */
        private void intentarFinalizar(Pedido p) {
            if (!p.cocinado || !p.pagado || p.consumoAplicado) return;

            // Descontar Insumos
            Map<Integer, Integer> insumosAUsar = p.consumoDetallado.get("insumos");
            for (var entry : insumosAUsar.entrySet()) {
                Inventario.Insumo ins = inventario.getInsumos().get(entry.getKey());
                if (ins != null) {
                    ins.cantidad = Math.max(0, ins.cantidad - entry.getValue());
                }
            }

            // Descontar Bebidas
            Map<Integer, Integer> bebidasAUsar = p.consumoDetallado.get("bebidas");
            for (var entry : bebidasAUsar.entrySet()) {
                Inventario.Bebida beb = inventario.getBebidas().get(entry.getKey());
                if (beb != null) {
                    beb.cantidad = Math.max(0, beb.cantidad - entry.getValue());
                }
            }

            // Calcular Total Venta final
            double total = 0;
            for(LineaProducto lp : p.lineasProductos) total += lp.producto.precio * lp.cantidad;
            for(LineaBebida lb : p.lineasBebidas) total += lb.bebida.precio * lb.cantidad;
            
            p.totalVenta = total;
            p.consumoAplicado = true;
            p.estado = "FINALIZADO";
            p.fechaFinalizacion = LocalDateTime.now();
            
            historial.add(p);
            // Opcional: limpiar de activos para no llenar memoria, o mantenerlos
        }

        public List<Pedido> listarActivos() {
            List<Pedido> lista = new ArrayList<>();
            for(Pedido p : pedidos.values()) {
                if(!p.estado.equals("FINALIZADO") && !p.estado.equals("CANCELADO")) lista.add(p);
            }
            return lista;
        }
        
        public List<Pedido> listarParaCocina() {
            List<Pedido> lista = new ArrayList<>();
            for(Pedido p : pedidos.values()) {
                if(p.estado.equals("EN_COLA") && !p.cocinado) lista.add(p);
            }
            return lista;
        }

        public List<Pedido> listarHistorial() {
            return historial;
        }

        // ---------------------------------------------------------------------
        // MÉTODOS INTERACTIVOS (MENÚS E INPUTS)
        // ---------------------------------------------------------------------

        public Integer crearPedidoInteractivo(Scanner sc, String mesero) {
            System.out.print("Nombre del cliente (opcional): ");
            String cliente = sc.nextLine();
            System.out.println("Tipos: 1) Mesa  2) Domicilio  3) Recoger");
            String t = sc.nextLine();
            String tipo = t.equals("2") ? "domicilio" : (t.equals("3") ? "recoger" : "mesa");

            Pedido p = crearPedido(mesero, cliente, tipo);
            System.out.println("Pedido creado ID " + p.idPedido + ". Borrador.");

            while(true) {
                System.out.println("\n--- Construyendo Pedido ---");
                System.out.println("A) Agregar producto");
                System.out.println("B) Agregar bebida");
                System.out.println("V) Ver pedido");
                System.out.println("F) Finalizar envío a cocina");
                System.out.println("X) Cancelar creación");
                String op = sc.nextLine().toUpperCase();

                if(op.equals("A")) agregarProductoInteractivo(sc, p.idPedido);
                else if(op.equals("B")) agregarBebidaInteractivo(sc, p.idPedido);
                else if(op.equals("V")) p.detalles();
                else if(op.equals("F")) {
                    confirmarEnvioCocina(p.idPedido);
                    System.out.println("Pedido enviado a cocina.");
                    return p.idPedido;
                }
                else if(op.equals("X")) {
                    cancelarPedido(p.idPedido);
                    System.out.println("Borrador descartado.");
                    return null;
                }
            }
        }

        private void agregarProductoInteractivo(Scanner sc, int pid) {
            System.out.print("ID del producto: ");
            try {
                int prodId = Integer.parseInt(sc.nextLine());
                System.out.print("Cantidad: ");
                int cant = Integer.parseInt(sc.nextLine());

                // Preguntar omisiones
                Map<Integer, Set<Integer>> omisiones = new HashMap<>();
                System.out.print("¿Indicar omisiones? (s/n): ");
                if(sc.nextLine().equalsIgnoreCase("s")) {
                    for(int i=0; i<cant; i++) {
                        System.out.printf("Unidad %d - IDs insumos a omitir (coma separados) o Enter: ", i);
                        String line = sc.nextLine();
                        if(!line.isEmpty()) {
                            Set<Integer> set = new HashSet<>();
                            for(String s : line.split(",")) {
                                try { set.add(Integer.parseInt(s.trim())); } catch(Exception e){}
                            }
                            omisiones.put(i, set);
                        }
                    }
                }

                if(agregarProducto(pid, prodId, cant, omisiones)) {
                    System.out.println("Producto agregado.");
                } else {
                    System.out.println("No se pudo agregar (Stock insuficiente o ID inválido).");
                }
            } catch(Exception e) {
                System.out.println("Entrada inválida.");
            }
        }

        private void agregarBebidaInteractivo(Scanner sc, int pid) {
            try {
                System.out.print("ID bebida: ");
                int bid = Integer.parseInt(sc.nextLine());
                System.out.print("Cantidad: ");
                int cant = Integer.parseInt(sc.nextLine());
                if(agregarBebida(pid, bid, cant)) System.out.println("Bebida agregada.");
                else System.out.println("Error al agregar bebida (Stock o ID).");
            } catch(Exception e) { System.out.println("Error entrada."); }
        }

        public void buscarYEditarPorTipoInteractivo(Scanner sc, String mesero) {
            System.out.println("Filtrar por: 1) Mesa 2) Domicilio 3) Recoger");
            String t = sc.nextLine();
            String tipo = t.equals("2") ? "domicilio" : (t.equals("3") ? "recoger" : "mesa");
            
            List<Pedido> filtrados = new ArrayList<>();
            for(Pedido p : pedidos.values()) {
                if(p.tipoPedido.equalsIgnoreCase(tipo) && p.esModificable()) filtrados.add(p);
            }

            if(filtrados.isEmpty()) { System.out.println("No hay pedidos modificables de ese tipo."); return; }

            for(Pedido p : filtrados) System.out.println(p.resumen());
            
            System.out.print("ID pedido a editar (0 salir): ");
            try {
                int pid = Integer.parseInt(sc.nextLine());
                if(pid == 0) return;
                Pedido p = pedidos.get(pid);
                if(p != null && p.esModificable()) {
                    System.out.println("1) Agregar Producto 2) Agregar Bebida 3) Pagar 4) Cancelar 5) Comentario");
                    String op = sc.nextLine();
                    if(op.equals("1")) agregarProductoInteractivo(sc, pid);
                    else if(op.equals("2")) agregarBebidaInteractivo(sc, pid);
                    else if(op.equals("3")) { if(marcarPagado(pid)) System.out.println("Pagado."); }
                    else if(op.equals("4")) { if(cancelarPedido(pid)) System.out.println("Cancelado."); }
                    else if(op.equals("5")) { 
                        System.out.print("Comentario: "); 
                        p.comentarios += " " + sc.nextLine(); 
                    }
                }
            } catch(Exception e) { System.out.println("Error."); }
        }

        public void menuCocineroInteractivo(Scanner sc, Main.Usuario usuario) {
            System.out.println("-- Menú Cocina: " + usuario.nombre + " --");
            while(true) {
                System.out.println("\n1. Ver pedidos en cola\n2. Marcar pedido como COCINADO\n0. Salir");
                String op = sc.nextLine();
                if(op.equals("1")) {
                    List<Pedido> cola = listarParaCocina();
                    if(cola.isEmpty()) System.out.println("No hay pedidos pendientes.");
                    else cola.forEach(p -> { System.out.println(p.resumen()); p.detalles(); });
                } else if(op.equals("2")) {
                    System.out.print("ID pedido a marcar cocinado: ");
                    try {
                        int pid = Integer.parseInt(sc.nextLine());
                        Pedido p = obtenerPedido(pid);
                        if(p != null && !p.cocinado) {
                            // Opción de agregar omisiones de última hora
                            marcarCocinadoLogico(pid, null); // Simplificado para Java, o se puede agregar lógica de omisión aquí
                            System.out.println("Pedido marcado como COCINADO.");
                        } else {
                            System.out.println("Pedido no válido.");
                        }
                    } catch(Exception e) { System.out.println("ID inválido."); }
                } else if(op.equals("0")) {
                    return;
                }
            }
        }
    }
}

