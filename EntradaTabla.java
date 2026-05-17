import java.util.List;

/**
 * Representa una entrada en la tabla de símbolos.
 * Guarda todo lo relevante de un símbolo: nombre, tipo, categoría,
 * valor actual, scope al que pertenece y línea donde fue declarado.
 */
public class EntradaTabla {

    // ── Categorías de símbolo ──────────────────────────────────────────────
    public enum Categoria {
        VARIABLE,   // declarada con tipo sin const
        FUNCION,    // declarada con la palabra reservada "funcion"
        PARAMETRO   // parámetro formal de una función
    }

    private final String       nombre;      // lexema del identificador
    private final TipoSemantico tipo;        // tipo semántico (INT, FLOAT, STRING, BOOL)
    private final Categoria    categoria;   // si es variable, función o parámetro
    private final String       scope;       // nombre del scope donde vive (ej. "global", "main")
    private final int          linea;       // línea de declaración

    private Object             valor;       // valor almacenado (puede ser null si no se asignó)
    private List<TipoSemantico> tiposParams; // solo para funciones: tipos de sus parámetros

    // Constructor para variables y parámetros
    public EntradaTabla(String nombre, TipoSemantico tipo, Categoria categoria,
                        String scope, int linea) {
        this.nombre    = nombre;
        this.tipo      = tipo;
        this.categoria = categoria;
        this.scope     = scope;
        this.linea     = linea;
        this.valor     = null;
    }

    // Constructor para funciones (también guarda los tipos de sus parámetros)
    public EntradaTabla(String nombre, TipoSemantico tipo, Categoria categoria,
                        String scope, int linea, List<TipoSemantico> tiposParams) {
        this(nombre, tipo, categoria, scope, linea);
        this.tiposParams = tiposParams;
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public String        getNombre()      { return nombre; }
    public TipoSemantico getTipo()        { return tipo; }
    public Categoria     getCategoria()   { return categoria; }
    public String        getScope()       { return scope; }
    public int           getLinea()       { return linea; }
    public Object        getValor()       { return valor; }
    public List<TipoSemantico> getTiposParams() { return tiposParams; }

    // ── Setter de valor (se actualiza en asignaciones) ─────────────────────
    public void setValor(Object v) { this.valor = v; }

    /**
     * Representación de la fila para el archivo de salida de la tabla.
     * Columnas: Nombre | Tipo | Categoría | Scope | Valor | Línea
     */
    @Override
    public String toString() {
        String valorStr = (valor != null) ? valor.toString() : "-";
        // Para funciones, mostrar también la firma de parámetros
        if (categoria == Categoria.FUNCION && tiposParams != null) {
            valorStr = "params=" + tiposParams;
        }
        return String.format("%-20s %-8s %-12s %-12s %-20s línea %d",
                nombre, tipo, categoria, scope, valorStr, linea);
    }
}