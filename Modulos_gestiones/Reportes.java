package Modulos_gestiones;

import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import static Modulos_gestiones.GestionPedidos.Pedido;


public class Reportes {

    private GestionPedidos.GestorPedidos gestorPedidos;
    private final Scanner sc;
    private final DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");


    public Reportes(GestionPedidos.GestorPedidos gestorPedidos, Scanner sc) {
        this.gestorPedidos = gestorPedidos;
        this.sc = sc;
    }

    // --- Helper para pedir fechas ---
    private LocalDate pedirFecha(String mensaje) {
        LocalDate fecha = null;
        while (fecha == null) {
            System.out.print(mensaje + " (formato DD/MM/AAAA): ");
            String fechaStr = sc.nextLine().trim();
            if (fechaStr.isEmpty()) {
                System.out.println("No se puede dejar vacío.");
                continue;
            }
            try {
                fecha = LocalDate.parse(fechaStr, inputFormatter);
            } catch (DateTimeParseException e) {
                System.out.println("Formato de fecha inválido. Use DD/MM/AAAA.");
            }
        }
        return fecha;
    }

    // --- Menú principal de Reportes ---
    public void menuReportes() {
        System.out.println("\n--- Menú de Reportes ---");
        while (true) {
            System.out.println("1. Reporte de Pedidos Vendidos (Rango de Fechas)");
            System.out.println("2. Ver Historial Completo");
            System.out.println("0. Volver al Menú Admin");
            System.out.print("Opción: ");
            String op = sc.nextLine();

            if (op.equals("1")) {
                generarReporteVentasPorFecha();
            } else if (op.equals("2")) {
                mostrarHistorialCompleto();
            } else if (op.equals("0")) {
                return;
            } else {
                System.out.println("Opción inválida.");
            }
        }
    }

    private void mostrarHistorialCompleto() {
        System.out.println("\n--- Historial Completo de Pedidos Finalizados ---");
        List<Pedido> historial = gestorPedidos.listarHistorial();
        if (historial.isEmpty()) {
            System.out.println("El historial de pedidos está vacío.");
            return;
        }

        double totalGlobal = 0.0;
        for (Pedido p : historial) {
            System.out.printf("ID:%d | Mesero:%s | Fecha:%s | Total:$%.2f %n", 
                p.idPedido, p.mesero, p.fechaFinalizacion.format(inputFormatter), p.totalVenta);
            totalGlobal += p.totalVenta;
        }
        System.out.printf("\nTOTAL VENDIDO EN EL HISTORIAL: $%.2f%n", totalGlobal);
    }

    private void generarReporteVentasPorFecha() {
        System.out.println("\n--- Reporte por Rango de Fechas ---");
        
        // 1. Pedir rango de fechas
        LocalDate fechaInicio = pedirFecha("Ingrese fecha de inicio del rango");
        LocalDate fechaFin = pedirFecha("Ingrese fecha de fin del rango");

        if (fechaInicio.isAfter(fechaFin)) {
            System.out.println("Error: La fecha de inicio no puede ser posterior a la fecha de fin.");
            return;
        }

        // 2. Filtrar el historial
        List<Pedido> historial = gestorPedidos.listarHistorial();
        List<Pedido> pedidosFiltrados = new ArrayList<>();
        double totalVendido = 0.0;
        int pedidosContados = 0;

        for (Pedido p : historial) {
            // Solo considerar pedidos FINALIZADOS y con fecha
            if (p.estado.equals("FINALIZADO") && p.fechaFinalizacion != null) {
                LocalDate fechaPedido = p.fechaFinalizacion.toLocalDate();
                
                // Comparación inclusiva: [fechaInicio, fechaFin]
                // !fechaPedido.isBefore(fechaInicio) <==> es igual o después de la fecha de inicio
                // !fechaPedido.isAfter(fechaFin)    <==> es igual o antes de la fecha de fin
                boolean enRango = !fechaPedido.isBefore(fechaInicio) && !fechaPedido.isAfter(fechaFin);
                
                if (enRango) {
                    pedidosFiltrados.add(p);
                    totalVendido += p.totalVenta;
                    pedidosContados++;
                }
            }
        }

        // 3. Mostrar resultados
        System.out.printf("\n--- RESULTADOS DEL REPORTE (%s al %s) ---%n", 
            fechaInicio.format(inputFormatter), fechaFin.format(inputFormatter));
        
        if (pedidosFiltrados.isEmpty()) {
            System.out.println("No se encontraron pedidos finalizados en este rango de fechas.");
            return;
        }

        for (Pedido p : pedidosFiltrados) {
            System.out.printf("ID:%d | Mesero:%s | Cliente:%s | Total:$%.2f | Hora:%s %n", 
                p.idPedido, p.mesero, p.cliente, p.totalVenta, p.fechaFinalizacion.format(outputFormatter));
        }

        System.out.printf("\nPedidos encontrados: %d%n", pedidosContados);
        System.out.printf("**TOTAL VENDIDO EN EL RANGO: $%.2f**%n", totalVendido);
        System.out.println("-----------------------------------------------\n");
    }
}