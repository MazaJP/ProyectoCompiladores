import java.io.*;
import java.util.*;

/**
 * Tabla de símbolos con soporte de scopes anidados (global → función → bloque).
 *
 * Internamente usa una pila de mapas: cada nivel de la pila es un scope.
 * Al entrar a una función/bloque se hace push, al salir se hace pop.
 * La búsqueda siempre busca desde el tope hacia el fondo (scope más local primero).
 *
 * Al final del análisis, toda la tabla acumulada se escribe en un archivo .sym.
 */
public class TablaSimbolos {

    // ── Estructura interna ─────────────────────────────────────────────────

    // Cada elemento de la pila es el scope actual con sus símbolos
    private final Deque<Map<String, EntradaTabla>> pilaScopes = new ArrayDeque<>();

    // Historial completo de entradas (para escribir el archivo de salida)
    private final List<EntradaTabla> historial = new ArrayList<>();

    // Nombre del scope activo (se actualiza al entrar/salir de funciones)
    private String scopeActual = "global";

    // Ruta del archivo de salida para la tabla
    private final String rutaSalida;

    // ── Constructor ────────────────────────────────────────────────────────
    public TablaSimbolos(String rutaEntrada) {
        // El archivo de tabla se guarda con extensión .sym junto al .mlng
        this.rutaSalida = rutaEntrada.endsWith(".mlng")
                ? rutaEntrada.substring(0, rutaEntrada.length() - 5) + ".sym"
                : rutaEntrada + ".sym";

        // Abrir el scope global al construir la tabla
        abrirScope("global");
    }

    // ── Gestión de scopes ──────────────────────────────────────────────────

    /**
     * Abre un nuevo scope (al entrar a una función o bloque).
     * Se hace push de un mapa vacío en la pila.
     */
    public void abrirScope(String nombre) {
        scopeActual = nombre;
        pilaScopes.push(new LinkedHashMap<>());
    }

    /**
     * Cierra el scope actual (al salir de una función o bloque).
     * Los símbolos del scope cerrado quedan en el historial pero ya no son accesibles.
     */
    public void cerrarScope() {
        if (pilaScopes.size() > 1) {
            pilaScopes.pop(); // elimina el scope más interno
            // el scope activo regresa al anterior
            scopeActual = (pilaScopes.size() == 1) ? "global" : scopeActual;
        }
    }

    // ── Inserción ──────────────────────────────────────────────────────────

    /**
     * Declara un nuevo símbolo en el scope actual.
     * Devuelve false si ya existe (declaración duplicada en el mismo scope).
     */
    public boolean declarar(EntradaTabla entrada) {
        Map<String, EntradaTabla> scopeTop = pilaScopes.peek();
        if (scopeTop.containsKey(entrada.getNombre())) {
            return false; // ya declarado en este scope → error semántico
        }
        scopeTop.put(entrada.getNombre(), entrada);
        historial.add(entrada);   // se guarda en el historial para el archivo de salida
        return true;
    }

    // ── Búsqueda ───────────────────────────────────────────────────────────

    /**
     * Busca un símbolo recorriendo los scopes de más interno a más externo.
     * Devuelve null si no existe (variable no declarada).
     */
    public EntradaTabla buscar(String nombre) {
        // Recorre la pila desde el tope (scope local) hacia el fondo (global)
        for (Map<String, EntradaTabla> scope : pilaScopes) {
            if (scope.containsKey(nombre)) {
                return scope.get(nombre);
            }
        }
        return null; // no encontrado
    }

    // ── Actualización de valor ─────────────────────────────────────────────

    /**
     * Actualiza el valor almacenado de un símbolo ya declarado.
     * Se usa cuando el semántico evalúa una asignación.
     */
    public boolean asignarValor(String nombre, Object valor) {
        EntradaTabla entrada = buscar(nombre);
        if (entrada == null) return false;
        entrada.setValor(valor);
        return true;
    }

    // ── Getters de utilidad ────────────────────────────────────────────────

    /** Devuelve el nombre del scope activo. */
    public String getScopeActual() { return scopeActual; }

    // ── Escritura del archivo de salida ────────────────────────────────────

    /**
     * Escribe el historial completo de la tabla en el archivo .sym.
     * Se llama al final del análisis semántico.
     * Formato: encabezado + una fila por cada símbolo.
     */
    public void escribirArchivo() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(rutaSalida))) {
            // ── Encabezado ──────────────────────────────────────────────────
            String separador = "=".repeat(90);
            bw.write(separador); bw.newLine();
            bw.write(String.format("  TABLA DE SÍMBOLOS — MiniLang%n"));
            bw.write(separador); bw.newLine();
            bw.write(String.format("%-20s %-8s %-12s %-12s %-20s %s%n",
                    "NOMBRE", "TIPO", "CATEGORÍA", "SCOPE", "VALOR", "LÍNEA"));
            bw.write("-".repeat(90)); bw.newLine();

            // ── Filas ────────────────────────────────────────────────────────
            for (EntradaTabla e : historial) {
                bw.write(e.toString());
                bw.newLine();
            }

            bw.write(separador); bw.newLine();
            bw.write("Total de símbolos: " + historial.size()); bw.newLine();

            System.out.println("Tabla de símbolos guardada en: " + rutaSalida);

        } catch (IOException ex) {
            System.out.println("Error al escribir tabla de símbolos: " + ex.getMessage());
        }
    }
}
