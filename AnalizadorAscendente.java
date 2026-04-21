import java.util.*;

/**
 * Analizador sintáctico ASCENDENTE para MiniLang.
 *
 * Mecanismo:
 *  - Mantiene una pila de SimboloPila.
 *  - Operación DESPLAZAR: mueve el token actual a la pila.
 *  - Operación REDUCIR:   saca N elementos del tope y empuja el no-terminal de cabeza.
 *  - Las expresiones se analizan con el algoritmo de patio de maniobras (shunting-yard),
 *    que es un método ascendente por precedencia de operadores.
 *
 * Gramática (producciones numeradas):
 *  P0 : PROGRAMA      → LISTA_SENT
 *  P1 : LISTA_SENT    → LISTA_SENT SENTENCIA
 *  P2 : LISTA_SENT    → SENTENCIA
 *  P3 : SENTENCIA     → NEWLINE
 *  P4 : SENTENCIA     → DECL NEWLINE
 *  P5 : SENTENCIA     → ASIGN NEWLINE
 *  P6 : SENTENCIA     → LLAMADA NEWLINE
 *  P7 : SENTENCIA     → SENT_IF
 *  P8 : SENTENCIA     → SENT_WHILE
 *  P9 : SENTENCIA     → DEF_FUNC
 *  P10: SENTENCIA     → SENT_RETURN NEWLINE
 *  P11: SENTENCIA     → SENT_READ NEWLINE
 *  P12: SENTENCIA     → SENT_WRITE NEWLINE
 *  P13: TIPO          → INT
 *  P14: TIPO          → FLOAT
 *  P15: TIPO          → STRING
 *  P16: TIPO          → BOOL
 *  P17: DECL          → TIPO ID IGUAL EXPR
 *  P18: DECL          → TIPO ID
 *  P19: ASIGN         → ID IGUAL EXPR
 *  P20: LLAMADA       → ID PARENI LISTA_ARGS PAREND
 *  P21: LLAMADA       → ID PARENI PAREND
 *  P22: SENT_IF       → IF EXPR DOSP NEWLINE BLOQUE ELSE DOSP NEWLINE BLOQUE
 *  P23: SENT_IF       → IF EXPR DOSP NEWLINE BLOQUE
 *  P24: SENT_WHILE    → WHILE EXPR DOSP NEWLINE BLOQUE
 *  P25: DEF_FUNC      → FUNCION ID PARENI LISTA_PARAMS PAREND DOSP NEWLINE BLOQUE
 *  P26: DEF_FUNC      → FUNCION ID PARENI PAREND DOSP NEWLINE BLOQUE
 *  P27: SENT_RETURN   → RETURN EXPR
 *  P28: SENT_RETURN   → RETURN
 *  P29: SENT_READ     → READ PARENI ID PAREND
 *  P30: SENT_WRITE    → WRITE PARENI LISTA_ARGS PAREND
 *  P31: BLOQUE        → INDENT LISTA_SENT DEDENT
 *  P32: LISTA_PARAMS  → LISTA_PARAMS COMA PARAM
 *  P33: LISTA_PARAMS  → PARAM
 *  P34: PARAM         → TIPO ID
 *  P35: LISTA_ARGS    → LISTA_ARGS COMA EXPR
 *  P36: LISTA_ARGS    → EXPR
 *  P37: EXPR          → EXPR OR EXPR
 *  P38: EXPR          → EXPR AND EXPR
 *  P39: EXPR          → NOT EXPR
 *  P40: EXPR          → EXPR MAYORQ   EXPR
 *  P41: EXPR          → EXPR MENORQ   EXPR
 *  P42: EXPR          → EXPR MAYORIGU EXPR
 *  P43: EXPR          → EXPR MENORIGU EXPR
 *  P44: EXPR          → EXPR EQUIVA   EXPR
 *  P45: EXPR          → EXPR NEGA     EXPR
 *  P46: EXPR          → EXPR SUMA     EXPR
 *  P47: EXPR          → EXPR RESTA    EXPR
 *  P48: EXPR          → EXPR MULTI    EXPR
 *  P49: EXPR          → EXPR DIV      EXPR
 *  P50: EXPR          → RESTA EXPR  (menos unario)
 *  P51: EXPR          → PARENI EXPR PAREND
 *  P52: EXPR          → ID PARENI LISTA_ARGS PAREND
 *  P53: EXPR          → ID PARENI PAREND
 *  P54: EXPR          → ID
 *  P55: EXPR          → NUMENTERO
 *  P56: EXPR          → NUMDECIMAL
 *  P57: EXPR          → CADENASTRING
 *  P58: EXPR          → TRUE
 *  P59: EXPR          → FALSE
 */
public class AnalizadorAscendente {

    // GRAMÁTICA


    private static final List<Produccion> GRAMATICA = new ArrayList<>();

    static {
        int n = 0;
        GRAMATICA.add(new Produccion(n++, "PROGRAMA",     "LISTA_SENT"));
        GRAMATICA.add(new Produccion(n++, "LISTA_SENT",   "LISTA_SENT", "SENTENCIA"));
        GRAMATICA.add(new Produccion(n++, "LISTA_SENT",   "SENTENCIA"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "NEWLINE"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "DECL",        "NEWLINE"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "ASIGN",       "NEWLINE"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "LLAMADA",     "NEWLINE"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "SENT_IF"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "SENT_WHILE"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "DEF_FUNC"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "SENT_RETURN", "NEWLINE"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "SENT_READ",   "NEWLINE"));
        GRAMATICA.add(new Produccion(n++, "SENTENCIA",    "SENT_WRITE",  "NEWLINE"));
        GRAMATICA.add(new Produccion(n++, "TIPO",         "INT"));
        GRAMATICA.add(new Produccion(n++, "TIPO",         "FLOAT"));
        GRAMATICA.add(new Produccion(n++, "TIPO",         "STRING"));
        GRAMATICA.add(new Produccion(n++, "TIPO",         "BOOL"));
        GRAMATICA.add(new Produccion(n++, "DECL",         "TIPO", "ID", "IGUAL", "EXPR"));
        GRAMATICA.add(new Produccion(n++, "DECL",         "TIPO", "ID"));
        GRAMATICA.add(new Produccion(n++, "ASIGN",        "ID", "IGUAL", "EXPR"));
        GRAMATICA.add(new Produccion(n++, "LLAMADA",      "ID", "PARENI", "LISTA_ARGS", "PAREND"));
        GRAMATICA.add(new Produccion(n++, "LLAMADA",      "ID", "PARENI", "PAREND"));
        GRAMATICA.add(new Produccion(n++, "SENT_IF",      "IF","EXPR","DOSP","NEWLINE","BLOQUE","ELSE","DOSP","NEWLINE","BLOQUE"));
        GRAMATICA.add(new Produccion(n++, "SENT_IF",      "IF","EXPR","DOSP","NEWLINE","BLOQUE"));
        GRAMATICA.add(new Produccion(n++, "SENT_WHILE",   "WHILE","EXPR","DOSP","NEWLINE","BLOQUE"));
        GRAMATICA.add(new Produccion(n++, "DEF_FUNC",     "FUNCION","ID","PARENI","LISTA_PARAMS","PAREND","DOSP","NEWLINE","BLOQUE"));
        GRAMATICA.add(new Produccion(n++, "DEF_FUNC",     "FUNCION","ID","PARENI","PAREND","DOSP","NEWLINE","BLOQUE"));
        GRAMATICA.add(new Produccion(n++, "SENT_RETURN",  "RETURN", "EXPR"));
        GRAMATICA.add(new Produccion(n++, "SENT_RETURN",  "RETURN"));
        GRAMATICA.add(new Produccion(n++, "SENT_READ",    "READ","PARENI","ID","PAREND"));
        GRAMATICA.add(new Produccion(n++, "SENT_WRITE",   "WRITE","PARENI","LISTA_ARGS","PAREND"));
        GRAMATICA.add(new Produccion(n++, "BLOQUE",       "INDENT","LISTA_SENT","DEDENT"));
        GRAMATICA.add(new Produccion(n++, "LISTA_PARAMS", "LISTA_PARAMS","COMA","PARAM"));
        GRAMATICA.add(new Produccion(n++, "LISTA_PARAMS", "PARAM"));
        GRAMATICA.add(new Produccion(n++, "PARAM",        "TIPO","ID"));
        GRAMATICA.add(new Produccion(n++, "LISTA_ARGS",   "LISTA_ARGS","COMA","EXPR"));
        GRAMATICA.add(new Produccion(n++, "LISTA_ARGS",   "EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","OR","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","AND","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "NOT","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","MAYORQ","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","MENORQ","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","MAYORIGU","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","MENORIGU","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","EQUIVA","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","NEGA","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","SUMA","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","RESTA","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","MULTI","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "EXPR","DIV","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "RESTA","EXPR"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "PARENI","EXPR","PAREND"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "ID","PARENI","LISTA_ARGS","PAREND"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "ID","PARENI","PAREND"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "ID"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "NUMENTERO"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "NUMDECIMAL"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "CADENASTRING"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "TRUE"));
        GRAMATICA.add(new Produccion(n++, "EXPR", "FALSE"));
    }

    // PRECEDENCIA Y ASOCIATIVIDAD (para shunting-yard)
    // De menor a mayor: OR, AND, NOT, comparaciones, +/-, */

    private static final Map<String, Integer> PRECEDENCIA = new HashMap<>();
    private static final Set<String> ASOC_DERECHA = new HashSet<>();

    static {
        PRECEDENCIA.put("OR",       1);
        PRECEDENCIA.put("AND",      2);
        PRECEDENCIA.put("NOT",      3);
        PRECEDENCIA.put("EQUIVA",   4);
        PRECEDENCIA.put("NEGA",     4);
        PRECEDENCIA.put("MAYORQ",   4);
        PRECEDENCIA.put("MENORQ",   4);
        PRECEDENCIA.put("MAYORIGU", 4);
        PRECEDENCIA.put("MENORIGU", 4);
        PRECEDENCIA.put("SUMA",     5);
        PRECEDENCIA.put("RESTA",    5);
        PRECEDENCIA.put("MULTI",    6);
        PRECEDENCIA.put("DIV",      6);
        PRECEDENCIA.put("UNARIO",   7);
        ASOC_DERECHA.add("NOT");
        ASOC_DERECHA.add("UNARIO");
    }


    // ESTADO DEL ANALIZADOR


    private final List<Token> tokens;
    private int pos;
    private final Deque<SimboloPila> pila = new ArrayDeque<>();
    private final List<String> errores    = new ArrayList<>();

    /** Evita reportar múltiples errores para la misma sentencia. */
    private boolean yaRecupero = false;

    public AnalizadorAscendente(List<Token> tokens) {
        this.tokens = tokens;
        this.pos    = 0;
        pila.push(new SimboloPila("$"));
    }

    public boolean analizar() {
        parsearListaSentencias();

        if (!pila.isEmpty() && pila.peek().getNombre().equals("LISTA_SENT")) {
            reducir(GRAMATICA.get(0));
        }

        if (!es(TokenType.EOF)) {
            registrarError("token inesperado al final del archivo", actual());
        }

        return errores.isEmpty();
    }

    public List<String> getErrores() { return errores; }


    // OPERACIONES FUNDAMENTALES DEL ANALIZADOR ASCENDENTE


    /**
     * DESPLAZAR: mueve el token actual al tope de la pila y avanza.
     */
    private Token desplazar() {
        Token t = actual();
        pila.push(new SimboloPila(t));
        if (pos < tokens.size() - 1) pos++;
        return t;
    }

    /**
     * REDUCIR: saca los elementos del cuerpo de la producción
     * y empuja el no-terminal de la cabeza.
     */
    private void reducir(Produccion p) {
        for (int i = 0; i < p.getCuerpo().length; i++) {
            pila.pop();
        }
        pila.push(new SimboloPila(p.getCabeza()));
    }

    // AUXILIARES


    private Token actual() { return tokens.get(pos); }

    private Token siguiente() {
        return pos + 1 < tokens.size() ? tokens.get(pos + 1) : tokens.get(tokens.size() - 1);
    }

    private boolean es(TokenType tipo) { return actual().getTipo() == tipo; }

    private boolean esTipo(TokenType t) {
        return t == TokenType.INT || t == TokenType.FLOAT
            || t == TokenType.STRING || t == TokenType.BOOL;
    }

    private boolean esInicioExpresion() {
        switch (actual().getTipo()) {
            case ID: case NUMENTERO: case NUMDECIMAL: case CADENASTRING:
            case TRUE: case FALSE: case PARENI: case NOT: case RESTA:
                return true;
            default: return false;
        }
    }

    /**
     * Registra un error con línea y columna del token problemático.
     * Si el token es NEWLINE muestra "fin de línea" en lugar del lexema
     * porque el NEWLINE siempre aparece en columna 1 y no es informativo.
     */
    private void registrarError(String mensaje, Token t) {
        String simbolo;
        if (t.getTipo() == TokenType.NEWLINE || t.getTipo() == TokenType.EOF) {
            simbolo = "fin de línea";
        } else {
            simbolo = "'" + t.getLexema() + "'";
        }
        errores.add("Línea " + t.getLinea() + ", col " + t.getColumnaInicio()
                + ": " + simbolo + " Error: " + mensaje);
    }

    /**
     * Recuperación de errores: avanza hasta el próximo NEWLINE o DEDENT
     * para continuar analizando el resto del archivo.
     * Activa la bandera yaRecupero para no generar errores en cascada.
     */
    private void recuperar() {
        yaRecupero = true;
        while (!es(TokenType.NEWLINE) && !es(TokenType.DEDENT) && !es(TokenType.EOF)) {
            if (pos < tokens.size() - 1) pos++;
            else break;
        }
        if (es(TokenType.NEWLINE) && pos < tokens.size() - 1) pos++;
    }

    /**
     * Intenta desplazar el tipo esperado.
     * Si no coincide, reporta el error usando el token ANTERIOR
     * como referencia de posición (más preciso que el token actual).
     */
    private void esperarYDesplazar(TokenType tipo, String mensaje) {
        if (es(tipo)) {
            desplazar();
        } else {
            // El token anterior indica dónde exactamente faltó el símbolo
            Token referencia = pos > 0 ? tokens.get(pos - 1) : actual();
            String simbolo;
            if (actual().getTipo() == TokenType.NEWLINE || actual().getTipo() == TokenType.EOF) {
                simbolo = "fin de línea";
            } else {
                simbolo = "'" + actual().getLexema() + "'";
            }
            errores.add("Línea " + referencia.getLinea() + ", col " + referencia.getColumnaFin()
                    + ": " + simbolo + " Error: " + mensaje);
        }
    }

    /** Consume el NEWLINE al final de una sentencia simple. */
    private void consumirNewline() {
        if (es(TokenType.NEWLINE)) {
            desplazar();
        } else if (!es(TokenType.EOF) && !es(TokenType.DEDENT)) {
            registrarError("se esperaba fin de línea", actual());
        }
    }




    private void parsearListaSentencias() {
        boolean primero = true;

        while (!es(TokenType.EOF) && !es(TokenType.DEDENT)) {

            if (es(TokenType.NEWLINE)) {
                desplazar();                       // SHIFT NEWLINE
                reducir(GRAMATICA.get(3));          // SENTENCIA → NEWLINE
            } else {
                parsearSentencia();
            }

            if (primero) {
                reducir(GRAMATICA.get(2));          // LISTA_SENT → SENTENCIA
                primero = false;
            } else {
                reducir(GRAMATICA.get(1));          // LISTA_SENT → LISTA_SENT SENTENCIA
            }
        }

        if (primero) {
            pila.push(new SimboloPila("LISTA_SENT"));
        }
    }

    // PARSEO DE SENTENCIAS INDIVIDUALES


    private void parsearSentencia() {
        yaRecupero = false; // resetear por cada sentencia nueva
        Token t = actual();
        try {
            switch (t.getTipo()) {
                case INT: case FLOAT: case STRING: case BOOL:
                    parsearDeclaracion(); break;
                case ID:
                    parsearAsignacionOLlamada(); break;
                case IF:
                    parsearIf(); break;
                case WHILE:
                    parsearWhile(); break;
                case FUNCION:
                    parsearDefinicionFuncion(); break;
                case RETURN:
                    parsearReturn(); break;
                case READ:
                    parsearRead(); break;
                case WRITE:
                    parsearWrite(); break;
                default:
                    registrarError("inicio de sentencia inválido", t);
                    recuperar();
                    pila.push(new SimboloPila("SENTENCIA"));
            }
        } catch (Exception e) {
            // Solo reportar si la recuperación no fue activada ya
            if (!yaRecupero) {
                registrarError("error inesperado en la sentencia", t);
                recuperar();
            }
            pila.push(new SimboloPila("SENTENCIA"));
        }
    }

    // ─── Declaración de variable ─────────────────────────────────

    private void parsearDeclaracion() {
        desplazar();     // SHIFT tipo
        reducirTipo();   // REDUCIR: tipo → TIPO

        if (!es(TokenType.ID)) {
            registrarError("se esperaba identificador", actual());
            reducir(GRAMATICA.get(18));
            consumirNewline();
            reducir(GRAMATICA.get(4));
            return;
        }
        desplazar(); // SHIFT ID

        if (es(TokenType.IGUAL)) {
            desplazar();                 // SHIFT IGUAL
            parsearExpresion();          // EXPR en pila
            reducir(GRAMATICA.get(17));  // DECL → TIPO ID IGUAL EXPR
        } else {
            reducir(GRAMATICA.get(18));  // DECL → TIPO ID
        }

        consumirNewline();
        reducir(GRAMATICA.get(4));       // SENTENCIA → DECL NEWLINE
    }

    private void reducirTipo() {
        switch (pila.peek().getToken().getTipo()) {
            case INT:    reducir(GRAMATICA.get(13)); break;
            case FLOAT:  reducir(GRAMATICA.get(14)); break;
            case STRING: reducir(GRAMATICA.get(15)); break;
            default:     reducir(GRAMATICA.get(16)); break; // BOOL
        }
    }

    // ─── Asignación o llamada ─────────────────────────────────────

    private void parsearAsignacionOLlamada() {
        desplazar(); // SHIFT ID

        if (es(TokenType.IGUAL)) {
            desplazar();                 // SHIFT IGUAL
            parsearExpresion();          // EXPR en pila
            reducir(GRAMATICA.get(19));  // ASIGN → ID IGUAL EXPR
            consumirNewline();
            reducir(GRAMATICA.get(5));   // SENTENCIA → ASIGN NEWLINE

        } else if (es(TokenType.PARENI)) {
            desplazar();                 // SHIFT PARENI
            if (es(TokenType.PAREND)) {
                desplazar();
                reducir(GRAMATICA.get(21)); // LLAMADA → ID PARENI PAREND
            } else {
                parsearListaArgumentos();
                esperarYDesplazar(TokenType.PAREND, "se esperaba ')'");
                reducir(GRAMATICA.get(20)); // LLAMADA → ID PARENI LISTA_ARGS PAREND
            }
            consumirNewline();
            reducir(GRAMATICA.get(6));   // SENTENCIA → LLAMADA NEWLINE

        } else {
            registrarError("se esperaba '=' o '(' después del identificador", actual());
            recuperar();
            pila.push(new SimboloPila("SENTENCIA"));
        }
    }

    // ─── If / Else ───────────────────────────────────────────────

    private void parsearIf() {
        desplazar();                       // SHIFT IF
        parsearExpresion();                // EXPR
        esperarYDesplazar(TokenType.DOSP,    "se esperaba ':'");
        esperarYDesplazar(TokenType.NEWLINE, "se esperaba nueva línea tras ':'");
        parsearBloque();                   // BLOQUE

        if (es(TokenType.ELSE)) {
            desplazar();                   // SHIFT ELSE
            esperarYDesplazar(TokenType.DOSP,    "se esperaba ':' después de else");
            esperarYDesplazar(TokenType.NEWLINE, "se esperaba nueva línea tras else:");
            parsearBloque();               // BLOQUE
            reducir(GRAMATICA.get(22));    // SENT_IF → IF EXPR DOSP NEWLINE BLOQUE ELSE DOSP NEWLINE BLOQUE
        } else {
            reducir(GRAMATICA.get(23));    // SENT_IF → IF EXPR DOSP NEWLINE BLOQUE
        }
        reducir(GRAMATICA.get(7));         // SENTENCIA → SENT_IF
    }

    // ─── While ───────────────────────────────────────────────────

    private void parsearWhile() {
        desplazar();                       // SHIFT WHILE
        parsearExpresion();                // EXPR
        esperarYDesplazar(TokenType.DOSP,    "se esperaba ':'");
        esperarYDesplazar(TokenType.NEWLINE, "se esperaba nueva línea tras ':'");
        parsearBloque();                   // BLOQUE
        reducir(GRAMATICA.get(24));        // SENT_WHILE → WHILE EXPR DOSP NEWLINE BLOQUE
        reducir(GRAMATICA.get(8));         // SENTENCIA → SENT_WHILE
    }

    // ─── Definición de función ────────────────────────────────────

    private void parsearDefinicionFuncion() {
        desplazar();                       // SHIFT FUNCION
        esperarYDesplazar(TokenType.ID,     "se esperaba nombre de función");
        esperarYDesplazar(TokenType.PARENI, "se esperaba '('");

        if (es(TokenType.PAREND)) {
            desplazar();
            esperarYDesplazar(TokenType.DOSP,    "se esperaba ':'");
            esperarYDesplazar(TokenType.NEWLINE, "se esperaba nueva línea");
            parsearBloque();
            reducir(GRAMATICA.get(26));    // DEF_FUNC → FUNCION ID PARENI PAREND DOSP NEWLINE BLOQUE
        } else {
            parsearListaParametros();
            esperarYDesplazar(TokenType.PAREND, "se esperaba ')'");
            esperarYDesplazar(TokenType.DOSP,   "se esperaba ':'");
            esperarYDesplazar(TokenType.NEWLINE, "se esperaba nueva línea");
            parsearBloque();
            reducir(GRAMATICA.get(25));    // DEF_FUNC → FUNCION ID PARENI LISTA_PARAMS PAREND DOSP NEWLINE BLOQUE
        }
        reducir(GRAMATICA.get(9));         // SENTENCIA → DEF_FUNC
    }

    // ─── Return ──────────────────────────────────────────────────

    private void parsearReturn() {
        desplazar();                       // SHIFT RETURN
        if (esInicioExpresion()) {
            parsearExpresion();
            reducir(GRAMATICA.get(27));    // SENT_RETURN → RETURN EXPR
        } else {
            reducir(GRAMATICA.get(28));    // SENT_RETURN → RETURN
        }
        consumirNewline();
        reducir(GRAMATICA.get(10));        // SENTENCIA → SENT_RETURN NEWLINE
    }

    // ─── Read ────────────────────────────────────────────────────

    private void parsearRead() {
        desplazar();                       // SHIFT READ
        esperarYDesplazar(TokenType.PARENI, "se esperaba '('");
        esperarYDesplazar(TokenType.ID,     "se esperaba identificador");
        esperarYDesplazar(TokenType.PAREND, "se esperaba ')'");
        reducir(GRAMATICA.get(29));        // SENT_READ → READ PARENI ID PAREND
        consumirNewline();
        reducir(GRAMATICA.get(11));        // SENTENCIA → SENT_READ NEWLINE
    }

    // ─── Write ───────────────────────────────────────────────────

    private void parsearWrite() {
        desplazar();                       // SHIFT WRITE
        esperarYDesplazar(TokenType.PARENI, "se esperaba '('");
        parsearListaArgumentos();
        esperarYDesplazar(TokenType.PAREND, "se esperaba ')'");
        reducir(GRAMATICA.get(30));        // SENT_WRITE → WRITE PARENI LISTA_ARGS PAREND
        consumirNewline();
        reducir(GRAMATICA.get(12));        // SENTENCIA → SENT_WRITE NEWLINE
    }

    // ─── Bloque ──────────────────────────────────────────────────

    private void parsearBloque() {
        if (!es(TokenType.INDENT)) {
            registrarError("se esperaba bloque indentado", actual());
            pila.push(new SimboloPila("BLOQUE"));
            return;
        }
        desplazar();                       // SHIFT INDENT
        parsearListaSentencias();          // LISTA_SENT
        if (!es(TokenType.DEDENT)) {
            registrarError("se esperaba fin del bloque (DEDENT)", actual());
        } else {
            desplazar();                   // SHIFT DEDENT
        }
        reducir(GRAMATICA.get(31));        // BLOQUE → INDENT LISTA_SENT DEDENT
    }

    // ─── Lista de parámetros ─────────────────────────────────────

    private void parsearListaParametros() {
        parsearParametro();
        reducir(GRAMATICA.get(33));        // LISTA_PARAMS → PARAM

        while (es(TokenType.COMA)) {
            desplazar();                   // SHIFT COMA
            parsearParametro();
            reducir(GRAMATICA.get(32));    // LISTA_PARAMS → LISTA_PARAMS COMA PARAM
        }
    }

    private void parsearParametro() {
        if (!esTipo(actual().getTipo())) {
            registrarError("se esperaba tipo en el parámetro", actual());
            pila.push(new SimboloPila("PARAM"));
            return;
        }
        desplazar();                       // SHIFT tipo
        reducirTipo();                     // TIPO → INT/FLOAT/STRING/BOOL
        esperarYDesplazar(TokenType.ID, "se esperaba nombre de parámetro");
        reducir(GRAMATICA.get(34));        // PARAM → TIPO ID
    }

    // ─── Lista de argumentos ─────────────────────────────────────

    private void parsearListaArgumentos() {
        if (!esInicioExpresion()) {
            pila.push(new SimboloPila("LISTA_ARGS"));
            return;
        }
        parsearExpresion();                // primer EXPR
        reducir(GRAMATICA.get(36));        // LISTA_ARGS → EXPR

        while (es(TokenType.COMA)) {
            desplazar();                   // SHIFT COMA
            parsearExpresion();            // siguiente EXPR
            reducir(GRAMATICA.get(35));    // LISTA_ARGS → LISTA_ARGS COMA EXPR
        }
    }


    // ANÁLISIS DE EXPRESIONES — ALGORITMO DE PATIO DE MANIOBRAS
 //método ascendente
    //
    // pilaOps : pila auxiliar de operadores pendientes de reducir
    // DESPLAZA operandos/operadores; REDUCE cuando la precedencia
    // del tope es mayor o igual que el operador entrante.


    private void parsearExpresion() {
        Deque<String> pilaOps  = new ArrayDeque<>();
        int numOperandos       = 0;
        boolean esperaOperando = true;

        bucle:
        while (true) {
            Token t    = actual();
            String tipo = t.getTipo().name();

            // Lado izquierdo: operando o prefijo
            if (esperaOperando) {

                if (t.getTipo() == TokenType.NOT) {
                    desplazar();
                    pilaOps.push("NOT");

                } else if (t.getTipo() == TokenType.RESTA) {
                    // Menos unario
                    desplazar();
                    pilaOps.push("UNARIO");

                } else if (t.getTipo() == TokenType.PARENI) {
                    desplazar();
                    pilaOps.push("PARENI");

                } else if (esOperandoSimple(t.getTipo())) {
                    if (t.getTipo() == TokenType.ID
                            && siguiente().getTipo() == TokenType.PARENI) {
                        parsearLlamadaEnExpresion();
                    } else {
                        desplazar();
                        aplicarReduccionOperando(t.getTipo()); // REDUCIR → EXPR
                    }
                    numOperandos++;
                    esperaOperando = false;

                    // Aplicar operadores unarios pendientes
                    while (!pilaOps.isEmpty()
                            && (pilaOps.peek().equals("NOT")
                                || pilaOps.peek().equals("UNARIO"))) {
                        aplicarOperadorUnario(pilaOps.pop());
                    }

                } else {
                    if (!yaRecupero) {
                        registrarError("expresión inválida", t);
                        yaRecupero = true;
                    }
                    break;
                }

            // Lado derecho: operador binario o fin
            } else {
                if (t.getTipo() == TokenType.PAREND
                        && pilaContienePareni(pilaOps)) {
                    // Cierre de paréntesis: reducir hasta PARENI
                    while (!pilaOps.isEmpty() && !pilaOps.peek().equals("PARENI")) {
                        reducirOperadorBinario(pilaOps.pop());
                        numOperandos--;
                    }
                    if (!pilaOps.isEmpty()) pilaOps.pop(); // sacar PARENI
                    desplazar();                           // SHIFT PAREND
                    reducirProduccion(51);                 // EXPR → PARENI EXPR PAREND

                } else if (PRECEDENCIA.containsKey(tipo)) {
                    // Operador binario: REDUCIR tope si tiene mayor/igual precedencia
                    while (!pilaOps.isEmpty()
                            && !pilaOps.peek().equals("PARENI")
                            && !pilaOps.peek().equals("NOT")
                            && !pilaOps.peek().equals("UNARIO")) {
                        String tope      = pilaOps.peek();
                        int precTope     = PRECEDENCIA.getOrDefault(tope, 0);
                        int precActual   = PRECEDENCIA.get(tipo);
                        boolean reducir  = (precTope > precActual)
                                || (precTope == precActual && !ASOC_DERECHA.contains(tipo));
                        if (reducir) {
                            reducirOperadorBinario(pilaOps.pop()); // REDUCIR
                            numOperandos--;
                        } else {
                            break;
                        }
                    }
                    pilaOps.push(tipo);
                    desplazar(); // SHIFT operador
                    esperaOperando = true;

                } else {
                    break bucle; // token no pertenece a la expresión
                }
            }
        }

        // Vaciar operadores restantes de la pila auxiliar
        while (!pilaOps.isEmpty()) {
            String op = pilaOps.pop();
            if (op.equals("PARENI")) {
                registrarError("paréntesis sin cerrar", actual());
            } else if (op.equals("NOT") || op.equals("UNARIO")) {
                aplicarOperadorUnario(op);
            } else {
                reducirOperadorBinario(op);
                numOperandos--;
            }
        }

        // Colocar resultado EXPR en la pila principal
        pila.push(new SimboloPila("EXPR"));
    }

    // ─── Llamada a función dentro de expresión ────────────────────

    private void parsearLlamadaEnExpresion() {
        desplazar(); // SHIFT ID
        desplazar(); // SHIFT PARENI

        if (es(TokenType.PAREND)) {
            desplazar();
            reducirProduccion(53); // EXPR → ID PARENI PAREND
        } else {
            parsearListaArgumentos();
            esperarYDesplazar(TokenType.PAREND, "se esperaba ')'");
            reducirProduccion(52); // EXPR → ID PARENI LISTA_ARGS PAREND
        }
    }

    // ─── Reducciones en expresiones ──────────────────────────────

    private void aplicarReduccionOperando(TokenType tipo) {
        switch (tipo) {
            case ID:           reducirProduccion(54); break;
            case NUMENTERO:    reducirProduccion(55); break;
            case NUMDECIMAL:   reducirProduccion(56); break;
            case CADENASTRING: reducirProduccion(57); break;
            case TRUE:         reducirProduccion(58); break;
            default:           reducirProduccion(59); break; // FALSE
        }
    }

    private void aplicarOperadorUnario(String op) {
        if (op.equals("NOT")) {
            reducirProduccion(39); // EXPR → NOT EXPR
        } else {
            reducirProduccion(50); // EXPR → RESTA EXPR (menos unario)
        }
    }

    private void reducirOperadorBinario(String op) {
        switch (op) {
            case "OR":       reducirProduccion(37); break;
            case "AND":      reducirProduccion(38); break;
            case "MAYORQ":   reducirProduccion(40); break;
            case "MENORQ":   reducirProduccion(41); break;
            case "MAYORIGU": reducirProduccion(42); break;
            case "MENORIGU": reducirProduccion(43); break;
            case "EQUIVA":   reducirProduccion(44); break;
            case "NEGA":     reducirProduccion(45); break;
            case "SUMA":     reducirProduccion(46); break;
            case "RESTA":    reducirProduccion(47); break;
            case "MULTI":    reducirProduccion(48); break;
            case "DIV":      reducirProduccion(49); break;
        }
    }

    private void reducirProduccion(int numero) {
        reducir(GRAMATICA.get(numero));
    }

    private boolean esOperandoSimple(TokenType t) {
        return t == TokenType.ID       || t == TokenType.NUMENTERO
            || t == TokenType.NUMDECIMAL || t == TokenType.CADENASTRING
            || t == TokenType.TRUE     || t == TokenType.FALSE;
    }

    private boolean pilaContienePareni(Deque<String> pilaOps) {
        for (String s : pilaOps) if (s.equals("PARENI")) return true;
        return false;
    }
}