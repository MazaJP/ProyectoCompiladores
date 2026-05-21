import java.io.*;
import java.util.*;

/**
 * Internamente usa una pila de mapas: cada nivel de la pila es un ambito.
 * Al entrar a una función/bloque se hace push, al salir se hace pop.
 * La búsqueda siempre busca desde el tope hacia el fondo
 *
 * Al final del análisis, toda la tabla acumulada se escribe en un archivo .sym.
 */
public class TablaSimbolos {

    //pila para para los ambitos
    private final Deque<Map<String, EntradaTabla>> pilaScopes = new ArrayDeque<>();

    // Guardamos todos los simbolos que son declarados 
    private final List<EntradaTabla> historial = new ArrayList<>();

    // Nombre de ambito por defautl global
    private String scopeActual = "global";

    // Ruta del archivo de salida para la tabla
    private final String rutaSalida;

    // creacion de la tabla .sym
    public TablaSimbolos(String rutaEntrada) {
        // El archivo de tabla se guarda con extensión .sym junto al .mlng
        this.rutaSalida = rutaEntrada.endsWith(".mlng")
                ? rutaEntrada.substring(0, rutaEntrada.length() - 5) + ".sym"
                : rutaEntrada + ".sym";

        // Abrir el ambito global donde se guardan las variables globales
        abrirScope("global");
    }

  
      //Abre un nuevo ambito al entrar a una función o bloque
     //Se hace push de un mapa vacío en la pila.
     //mantenideo el orden en el que se fueron metiendo
    public void abrirScope(String nombre) {
        scopeActual = nombre;
        pilaScopes.push(new LinkedHashMap<>());
    }

     //Cierra el scope actual al salir de una función o bloque
     //Los símbolos del ambito cerrado quedan en el historial pero ya no son accesibles.

    public void cerrarScope() {
        //no sacamos el ambito global
        if (pilaScopes.size() > 1) {
            //sacamos cada ""cajon"guardando tadas las variable en el historial 
            pilaScopes.pop();
            scopeActual = (pilaScopes.size() == 1) ? "global" : scopeActual;
        }
    }


    //Revisamos si el nuevo ambito no tiene el mismo nombre que el anterior
    public boolean declarar(EntradaTabla entrada) {
        Map<String, EntradaTabla> scopeTop = pilaScopes.peek();
        if (scopeTop.containsKey(entrada.getNombre())) {
            return false; // ya declarado en este ambito da falso
        }
        //agrega el nuevo ambito lo agrega y lo que este tiene
        scopeTop.put(entrada.getNombre(), entrada);
        historial.add(entrada); // se guarda en el historial para el archivo de salida
        return true;
    }
    //Busca el ambito dentro de la pila hasta encontrarlo 
    public EntradaTabla buscar(String nombre) {
        for (Map<String, EntradaTabla> scope : pilaScopes) {
            if (scope.containsKey(nombre)) return scope.get(nombre);
        }
        return null;
    }
    //calcula el valor de una expresion para guardarlo en la tabla 
    public boolean asignarValor(String nombre, Object valor) {
        EntradaTabla entrada = buscar(nombre);
        if (entrada == null) return false;
        entrada.setValor(valor);
        return true;
    }
    // saber en que ambito estamos
    public String getScopeActual() { return scopeActual; }
 
     //Escribe el historial completo de la tabla en el archivo .sym.
     //agarra lo que esta en la tabla
    public void escribirArchivo() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(rutaSalida))) {

            bw.write(String.format("%-20s %-8s %-12s %-12s %-20s %s",
                    "NOMBRE", "TIPO", "CATEGORIA", "AMBITO", "VALOR", "LINEA"));
            bw.newLine();
            bw.write("-".repeat(80));
            bw.newLine();

            for (EntradaTabla e : historial) {
                bw.write(e.toString());
                bw.newLine();
            }

            bw.write("-".repeat(80));
            bw.newLine();
            bw.write("Total: " + historial.size() + " simbolos");
            bw.newLine();

            System.out.println("Tabla de simbolos guardada en: " + rutaSalida);

        } catch (IOException ex) {
            System.out.println("Error al escribir tabla de simbolos: " + ex.getMessage());
        }
    }
}