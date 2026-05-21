    import java.util.List;

    /**
     * Representa una entrada en la tabla de símbolos.
     * Guarda todo lo relevante de un símbolo: nombre, tipo, categoría,
     * valor actual, ambito, línea de declaración y si es constante.
     */
    public class EntradaTabla {

        public enum Categoria {
            VARIABLE,   // declarada con solo el tipo int, float, double, string, bool
            CONSTANTE,  // declarada con "const tipo"
            FUNCION,    // declarada con la palabra reservada "funcion"
            PARAMETRO   // parámetro formal de una función
        }

        private final String            nombre;      // nomrbe del identificador
        private final TipoSemantico     tipo;        // tipo semántico
        private final Categoria         categoria;   // VARIABLE, CONSTANTE, FUNCION o PARAMETRO
        private final String            scope;       // ambito donde vive ("global", nombre de función, etc.)
        private final int               linea;       // línea de declaración
        private final boolean           esConstante; // true si fue declarado con "const"

        private Object                  valor;       // valor almacenado (null si no fue asignado aún)
        private boolean                 yaAsignada;  // true si ya recibió un valor (para validar const)
        private List<TipoSemantico>     tiposParams; // solo para funciones: tipos de sus parámetros

        //asigna cada valro recibido 
        public EntradaTabla(String nombre, TipoSemantico tipo, Categoria categoria, String scope, int linea) {
            this.nombre      = nombre;
            this.tipo        = tipo;
            this.categoria   = categoria;
            this.scope       = scope;
            this.linea       = linea;
            this.esConstante = (categoria == Categoria.CONSTANTE);
            this.valor       = null;
            this.yaAsignada  = false;
        }
//Guardamos lo stipos de parametros que usamos asignamos los parametrso
        public EntradaTabla(String nombre, TipoSemantico tipo, Categoria categoria,String scope, int linea, List<TipoSemantico> tiposParams) {
            this(nombre, tipo, categoria, scope, linea);
            this.tiposParams = tiposParams;
        }

        public String           getNombre()      { return nombre; }
        public TipoSemantico    getTipo()        { return tipo; }
        public Categoria        getCategoria()   { return categoria; }
        public String           getScope()       { return scope; }
        public int              getLinea()       { return linea; }
        public Object           getValor()       { return valor; }
        public boolean          isEsConstante()  { return esConstante; }
        public boolean          isYaAsignada()   { return yaAsignada; }
        public List<TipoSemantico> getTiposParams() { return tiposParams; }

        
         //Asigna un valor a la entrada.
         //Si es constante y ya tiene un valor previo, devuelve false
         //para que el semántico reporte el error de reasignación.
         
        public boolean setValor(Object v) {
            if (esConstante && yaAsignada) {
                return false; // intento de reasignar una constante da error
            }
            this.valor      = v;
            this.yaAsignada = true;
            return true;
        }

        
          //Formato de salida de la tabla
          //Columnas: Nombre | Tipo | Categoría | Ambito | Valor | Línea
         
        @Override
        public String toString() {
            String valorStr;
            if (categoria == Categoria.FUNCION && tiposParams != null) {
                valorStr = "params=" + tiposParams;
            } else {
                valorStr = (valor != null) ? valor.toString() : "-";
            }
            return String.format("%-20s %-8s %-12s %-12s %-20s línea %d",
                    nombre, tipo, categoria, scope, valorStr, linea);
        }
    }